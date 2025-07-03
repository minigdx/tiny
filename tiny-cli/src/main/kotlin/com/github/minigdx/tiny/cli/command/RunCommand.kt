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
import com.github.minigdx.tiny.cli.debug.DebugRemoteCommand
import com.github.minigdx.tiny.cli.debug.DebuggerExecutionListener
import com.github.minigdx.tiny.cli.debug.EngineRemoteCommand
import com.github.minigdx.tiny.cli.debug.Reload
import com.github.minigdx.tiny.engine.GameEngine
import com.github.minigdx.tiny.engine.GameEngineListener
import com.github.minigdx.tiny.engine.TinyException
import com.github.minigdx.tiny.file.CommonVirtualFileSystem
import com.github.minigdx.tiny.file.JvmLocalFile
import com.github.minigdx.tiny.log.LogLevel
import com.github.minigdx.tiny.log.StdOutLogger
import com.github.minigdx.tiny.lua.WorkspaceLib
import com.github.minigdx.tiny.lua.errorLine
import com.github.minigdx.tiny.platform.glfw.GlfwPlatform
import com.github.minigdx.tiny.render.LwjglGLRender
import com.github.minigdx.tiny.resources.GameScript
import io.ktor.server.application.install
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import io.ktor.server.routing.routing
import io.ktor.server.websocket.WebSockets
import io.ktor.server.websocket.webSocket
import io.ktor.websocket.Frame
import io.ktor.websocket.readText
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import org.luaj.vm2.LuaError
import java.io.File
import kotlin.time.ExperimentalTime

class RunCommand : CliktCommand(name = "run") {
    val gameDirectory by argument(help = "The directory containing your game to be run.")
        .file(mustExist = true, canBeDir = true, canBeFile = false)
        .default(File("."))

    val test by option(help = "Run tests before running the game.")
        .flag()

    val debug by option(help = "Port used for debugging")
        .int()
        .default(8081)

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

        echo("\uD83D\uDC1B === Running the game using debugger on the port '$debug' ===")
        echo("\uD83D\uDC1B === Use the command 'tiny-cli debug' to connect the debugger to your game  ===")

        val debugCommandReceiver = Channel<DebugRemoteCommand>()
        val engineCommandSender = Channel<EngineRemoteCommand>()

        embeddedServer(
            factory = Netty,
            port = debug,
        ) {
            install(WebSockets)

            routing {
                webSocket("/debug") {
                    launch {
                        for (command in engineCommandSender) {
                            outgoing.send(Frame.Text(Json.encodeToString(command)))
                        }
                    }

                    for (frame in incoming) {
                        if (frame is Frame.Text) {
                            val command = Json.decodeFromString<DebugRemoteCommand>(frame.readText())
                            debugCommandReceiver.send(command)
                        } else {
                            TODO("$frame content not expected")
                        }
                    }
                }
            }
        }.start()

        try {
            val configFile = effectiveGameDirectory.resolve("_tiny.json")
            if (!configFile.exists()) {
                echo("\uD83D\uDE2D No _tiny.json found in ${effectiveGameDirectory.absolutePath}! Can't run the game without.")
                throw Abort()
            }
            val gameParameters = GameParameters.read(configFile)

            val logger = StdOutLogger("tiny-cli", level = LogLevel.DEBUG)

            val vfs = CommonVirtualFileSystem()
            val gameOption =
                gameParameters.toGameOptions()
                    .copy(runTests = test)

            val debugListener = DebuggerExecutionListener(debugCommandReceiver, engineCommandSender)

            val gameEngine =
                GameEngine(
                    gameOptions = gameOption,
                    platform = GlfwPlatform(gameOption, logger, vfs, effectiveGameDirectory, LwjglGLRender(logger, gameOption)),
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

            val data = effectiveGameDirectory.resolve("data")
            echo("\uD83D\uDC1F === Looking for data directory at: ${data.absolutePath} ===")
            if (data.exists() && data.isDirectory) {
                echo("\uD83D\uDC1F === Data directory found with ${data.listFiles()?.size ?: 0} files ===")
                WorkspaceLib.DEFAULT = data.listFiles()?.map { JvmLocalFile(it.name, data) } ?: emptyList()
            } else {
                echo("\uD83D\uDC1F === Data directory not found at: ${data.absolutePath} ===")
            }
            gameEngine.main()
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
        }
    }
}
