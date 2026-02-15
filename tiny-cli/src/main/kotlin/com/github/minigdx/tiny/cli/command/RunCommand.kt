package com.github.minigdx.tiny.cli.command

import com.github.ajalt.clikt.core.Abort
import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.Context
import com.github.ajalt.clikt.parameters.arguments.argument
import com.github.ajalt.clikt.parameters.arguments.default
import com.github.ajalt.clikt.parameters.options.default
import com.github.ajalt.clikt.parameters.options.flag
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.types.file
import com.github.ajalt.clikt.parameters.types.int
import com.github.minigdx.tiny.cli.config.GameParameters
import com.github.minigdx.tiny.cli.debug.AllFiles
import com.github.minigdx.tiny.cli.debug.DebugRemoteCommand
import com.github.minigdx.tiny.cli.debug.DebuggerExecutionListener
import com.github.minigdx.tiny.cli.debug.EngineRemoteCommand
import com.github.minigdx.tiny.cli.debug.FileChanged
import com.github.minigdx.tiny.cli.debug.FileInfo
import com.github.minigdx.tiny.cli.debug.GameMetadata
import com.github.minigdx.tiny.cli.debug.Reload
import com.github.minigdx.tiny.engine.GameEngine
import com.github.minigdx.tiny.engine.GameEngineListener
import com.github.minigdx.tiny.engine.TinyException
import com.github.minigdx.tiny.file.CommonVirtualFileSystem
import com.github.minigdx.tiny.log.LogLevel
import com.github.minigdx.tiny.log.StdOutLogger
import com.github.minigdx.tiny.lua.errorLine
import com.github.minigdx.tiny.platform.glfw.GlfwPlatform
import com.github.minigdx.tiny.resources.GameScript
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.install
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import io.ktor.server.response.respond
import io.ktor.server.response.respondBytes
import io.ktor.server.routing.get
import io.ktor.server.routing.routing
import io.ktor.server.websocket.WebSockets
import io.ktor.server.websocket.webSocket
import io.ktor.websocket.Frame
import io.ktor.websocket.readText
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.luaj.vm2.LuaError
import java.io.File
import java.util.jar.JarInputStream
import kotlin.system.exitProcess
import kotlin.time.ExperimentalTime

class RunCommand : CliktCommand(name = "run") {
    val gameDirectory by argument(help = "The directory containing your game to be run.")
        .file(mustExist = true, canBeDir = true, canBeFile = false)
        .default(File("."))

    val debug by option(help = "Port used for debugging")
        .int()
        .default(8081)

    val noDebug by option("--no-debug", help = "Disable the debug server")
        .flag()

    val macFromJpackage by option(
        help = "Find the game directory inside the Game MacOS App Bundle instead",
        hidden = true,
    )
        .flag()

    override fun help(context: Context) = "Run your game."

    private fun isOracleOrOpenJDK(): Boolean {
        val vendor = System.getProperty("java.vendor")?.lowercase()
        return vendor?.contains("oracle") == true || vendor?.contains("eclipse") == true || vendor?.contains("openjdk") == true
    }

    private fun isMacOS(): Boolean {
        val os = System.getProperty("os.name").lowercase()
        return os.contains("mac") || os.contains("darwin")
    }

    private fun getClassLocationDirectory(): File {
        val classLocation = RunCommand::class.java.protectionDomain.codeSource.location.toURI().path
        val classLocationFile = File(classLocation)
        val classLocationDir = if (classLocationFile.isFile) classLocationFile.parentFile else classLocationFile

        echo("\uD83D\uDC1F === Class location: $classLocation ===")
        echo("\uD83D\uDC1F === Class location directory: ${classLocationDir.absolutePath} ===")

        return classLocationDir
    }

    @OptIn(ExperimentalTime::class)
    override fun run() {
        if (isMacOS() && isOracleOrOpenJDK()) {
            echo("\uD83D\uDEA7 === The Tiny CLI on Mac with require a special option.")
            echo("\uD83D\uDEA7 === If the application crash ➡ use the command 'tiny-cli-mac' instead.")
        }

        val effectiveGameDirectory = if (macFromJpackage) {
            val classLocationDir = getClassLocationDirectory()
            echo("\uD83D\uDC1F === Using class location directory instead of game directory due to mac-from-jpackage flag ===")
            classLocationDir.resolve("game")
        } else {
            echo("\uD83D\uDC1F === Using provided game directory: ${gameDirectory.absolutePath} ===")
            gameDirectory
        }

        val debugCommandReceiver = Channel<DebugRemoteCommand>()
        val engineCommandSender = Channel<EngineRemoteCommand>()

        val server = if (!noDebug) {
            startDebugServer(effectiveGameDirectory, debugCommandReceiver, engineCommandSender)
        } else {
            null
        }

        try {
            val configFile = effectiveGameDirectory.resolve("_tiny.json")
            if (!configFile.exists()) {
                echo("\uD83D\uDE2D No _tiny.json found in ${effectiveGameDirectory.absolutePath}! Can't run the game without.")
                throw Abort()
            }
            val gameParameters = GameParameters.read(configFile)

            val logger = StdOutLogger("tiny-cli", level = LogLevel.DEBUG)

            val homeDirectory = findHomeDirectory(gameParameters)

            val vfs = CommonVirtualFileSystem()
            val gameOption = gameParameters.toGameOptions()

            val debugListener = DebuggerExecutionListener(debugCommandReceiver, engineCommandSender)

            val gameEngine = GameEngine(
                gameOptions = gameOption,
                platform = GlfwPlatform(
                    gameOption,
                    logger,
                    vfs,
                    gameDirectory,
                    homeDirectory,
                ),
                vfs = vfs,
                logger = logger,
                listener =
                    object : GameEngineListener {
                        override fun switchScript(
                            before: GameScript?,
                            after: GameScript?,
                        ) {
                            if (after != null) {
                                debugListener.globals = after.globals!!
                                debugListener.globals.debuglib = debugListener
                            }
                        }

                        override fun reload(gameScript: GameScript?) {
                            gameScript?.run {
                                debugListener.globals = gameScript.globals!!
                                debugListener.globals.debuglib = debugListener

                                CoroutineScope(Dispatchers.IO).launch {
                                    engineCommandSender.send(Reload(gameScript.name))
                                }
                            }
                        }
                    },
            )
            Runtime.getRuntime().addShutdownHook(
                Thread {
                    gameEngine.end()
                    echo("\uD83D\uDC4B See you soon!")
                },
            )

            gameEngine.main()

            // Clean shutdown: stop the debug server and exit
            if (server != null) {
                echo("\uD83D\uDEE1\uFE0F Shutting down debug server...")
                server.stop(1000, 2000)
                echo("\u2705 Debug server stopped. Exiting...")
            }
            exitProcess(0)
        } catch (ex: Exception) {
            echo(
                "\uD83E\uDDE8 An unexpected exception occurred. " +
                    "The application will stop. It might be a bug in Tiny. " +
                    "If so, please report it.",
            )
            when (ex) {
                is LuaError -> {
                    val (nb, line) = ex.errorLine() ?: (null to null)
                    echo("Error found line $nb:$line")
                }

                is TinyException -> {
                    echo("Error found in the script '${ex.name}' line ${ex.lineNumber}:${ex.line}.")
                }
            }
            echo()
            ex.printStackTrace()

            // Clean shutdown even on exception
            if (server != null) {
                echo("\uD83D\uDEE1\uFE0F Shutting down debug server...")
                server.stop(1000, 2000)
            }
            exitProcess(1)
        }
    }

    private fun startDebugServer(
        effectiveGameDirectory: File,
        debugCommandReceiver: Channel<DebugRemoteCommand>,
        engineCommandSender: Channel<EngineRemoteCommand>,
    ): io.ktor.server.engine.EmbeddedServer<*, *> {
        val configFile = effectiveGameDirectory.resolve("_tiny.json")
        val staticResources = loadDebuggerResources()

        val scriptFiles = if (configFile.exists()) {
            val params = GameParameters.read(configFile)
            params.getAllScripts()
        } else {
            emptyList()
        }

        val lastModified = mutableMapOf<String, Long>()
        if (configFile.exists()) {
            lastModified["_tiny.json"] = configFile.lastModified()
        }
        scriptFiles.forEach { script ->
            val file = effectiveGameDirectory.resolve(script)
            if (file.exists()) {
                lastModified[script] = file.lastModified()
            }
        }

        val server = embeddedServer(
            factory = Netty,
            port = debug,
        ) {
            install(WebSockets)

            routing {
                // Unified WebSocket: engine debug commands + file watching + metadata
                webSocket("/debug") {
                    // Send game metadata on connect
                    if (configFile.exists()) {
                        val params = GameParameters.read(configFile)
                        val metadata = GameMetadata(gameId = params.id, gameName = params.name)
                        outgoing.send(Frame.Text(Json.encodeToString<EngineRemoteCommand>(metadata)))
                    }

                    // Send initial file list
                    val allFiles = buildFilesList(effectiveGameDirectory)
                    outgoing.send(Frame.Text(Json.encodeToString<EngineRemoteCommand>(AllFiles(allFiles))))

                    // Forward engine commands to the client
                    launch {
                        for (command in engineCommandSender) {
                            outgoing.send(Frame.Text(Json.encodeToString(command)))
                        }
                    }

                    // File-watching coroutine
                    launch {
                        while (isActive) {
                            delay(500)

                            val currentScripts = try {
                                val params = GameParameters.read(configFile)
                                params.getAllScripts()
                            } catch (_: Exception) {
                                scriptFiles
                            }

                            val configModified = configFile.lastModified()
                            if (configModified != lastModified["_tiny.json"]) {
                                lastModified["_tiny.json"] = configModified
                                val content = configFile.readText()
                                val msg = FileChanged(file = FileInfo("_tiny.json", content))
                                outgoing.send(Frame.Text(Json.encodeToString<EngineRemoteCommand>(msg)))
                            }

                            currentScripts.forEach { script ->
                                val file = effectiveGameDirectory.resolve(script)
                                if (file.exists()) {
                                    val modified = file.lastModified()
                                    if (modified != lastModified[script]) {
                                        lastModified[script] = modified
                                        val content = file.readText()
                                        val msg = FileChanged(file = FileInfo(script, content))
                                        outgoing.send(Frame.Text(Json.encodeToString<EngineRemoteCommand>(msg)))
                                    }
                                }
                            }
                        }
                    }

                    // Process incoming debug commands from the client
                    for (frame in incoming) {
                        if (frame is Frame.Text) {
                            val command = Json.decodeFromString<DebugRemoteCommand>(frame.readText())
                            debugCommandReceiver.send(command)
                        } else {
                            TODO("$frame content not expected")
                        }
                    }
                }

                // Serve debugger webapp static files
                get("/") {
                    val value = staticResources["index.html"]
                    if (value != null) {
                        call.respondBytes(value, ContentType.Text.Html)
                    } else {
                        call.respond(HttpStatusCode.NotFound)
                    }
                }

                get("/{...}") {
                    val key = call.request.local.uri.let { k ->
                        if (k.startsWith("/")) k.drop(1) else k
                    }
                    val value = staticResources[key]
                    if (value != null) {
                        val contentType = when {
                            key.endsWith(".js") -> ContentType.Application.JavaScript
                            key.endsWith(".css") -> ContentType.Text.CSS
                            key.endsWith(".html") -> ContentType.Text.Html
                            key.endsWith(".png") -> ContentType.Image.PNG
                            key.endsWith(".svg") -> ContentType.Image.SVG
                            key.endsWith(".json") -> ContentType.Application.Json
                            key.endsWith(".mjs") -> ContentType.Application.JavaScript
                            else -> ContentType.Application.OctetStream
                        }
                        call.respondBytes(value, contentType)
                    } else {
                        call.respond(HttpStatusCode.NotFound)
                    }
                }
            }
        }.start()

        val debuggerAddress = "http://localhost:$debug"
        echo("\uD83D\uDC1B === Debug server started on port '$debug' ===")
        echo("\uD83D\uDC1B === Debugger webapp: $debuggerAddress ===")

        return server
    }

    private fun loadDebuggerResources(): Map<String, ByteArray> {
        val resources = mutableMapOf<String, ByteArray>()

        val debuggerZip = RunCommand::class.java
            .getResourceAsStream("/tiny-debugger.zip")

        if (debuggerZip != null) {
            JarInputStream(debuggerZip).use { jarInput ->
                var entry = jarInput.nextJarEntry
                while (entry != null) {
                    if (!entry.isDirectory && !entry.name.startsWith("META-INF/")) {
                        resources[entry.name] = jarInput.readAllBytes()
                    }
                    jarInput.closeEntry()
                    entry = jarInput.nextJarEntry
                }
            }
        }

        return resources
    }

    private fun buildFilesList(gameDir: File): List<FileInfo> {
        val configFile = gameDir.resolve("_tiny.json")
        if (!configFile.exists()) return emptyList()
        val gameParameters = GameParameters.read(configFile)
        val files = mutableListOf<FileInfo>()
        gameParameters.getAllScripts().forEach { script ->
            val file = gameDir.resolve(script)
            if (file.exists()) {
                files.add(FileInfo(script, file.readText()))
            }
        }
        return files
    }
}

internal fun findHomeDirectory(gameParameters: GameParameters): File {
    val appDataDir = when {
        System.getProperty("os.name").lowercase().contains("mac") ||
            System.getProperty("os.name").lowercase().contains("darwin") ->
            File(System.getProperty("user.home")).resolve("Library/Application Support")

        System.getProperty("os.name").lowercase().contains("win") ->
            File(System.getenv("APPDATA") ?: System.getProperty("user.home"))

        else -> // Linux and other Unix-like systems
            File(System.getProperty("user.home")).resolve(".local/share")
    }
    val homeDirectory = appDataDir.resolve("tiny/${gameParameters.id}")
    return homeDirectory
}
