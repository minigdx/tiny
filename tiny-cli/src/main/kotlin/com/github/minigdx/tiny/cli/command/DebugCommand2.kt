package com.github.minigdx.tiny.cli.command

import com.github.ajalt.clikt.core.Abort
import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.Context
import com.github.ajalt.clikt.parameters.options.default
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.types.file
import com.github.ajalt.clikt.parameters.types.int
import com.github.minigdx.tiny.cli.config.GameParameters
import com.github.minigdx.tiny.cli.debug.FileChangedMessage
import com.github.minigdx.tiny.cli.debug.FileInfo
import com.github.minigdx.tiny.cli.debug.FilesMessage
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
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File
import java.net.URI
import java.util.jar.JarInputStream

class DebugCommand2 : CliktCommand(name = "debug2") {
    val gameDirectory by option("-d", "--directory", help = "The directory containing all game information.")
        .file(mustExist = true, canBeDir = true, canBeFile = false)
        .default(File("."))

    val port by option("-p", "--port", help = "Port for the web debugger.")
        .int()
        .default(8082)

    val debug by option("--debug", help = "Debug port used by the game engine.")
        .int()
        .default(8081)

    override fun help(context: Context) = "Debug the current game using a web-based debugger."

    override fun run() {
        val configFile = gameDirectory.resolve("_tiny.json")
        if (!configFile.exists()) {
            echo("\uD83D\uDE2D No _tiny.json found! Can't debug the game without.")
            throw Abort()
        }
        val gameParameters = GameParameters.read(configFile)

        val staticResources = loadDebuggerResources()

        val scriptFiles = gameParameters.getAllScripts()

        val lastModified = mutableMapOf<String, Long>()
        lastModified["_tiny.json"] = configFile.lastModified()
        scriptFiles.forEach { script ->
            val file = gameDirectory.resolve(script)
            if (file.exists()) {
                lastModified[script] = file.lastModified()
            }
        }

        val server = embeddedServer(
            factory = Netty,
            port = port,
        ) {
            install(WebSockets)

            routing {
                webSocket("/ws") {
                    val allFiles = buildFilesList(gameDirectory, gameParameters)
                    val filesMsg = FilesMessage(type = "files", files = allFiles)
                    outgoing.send(Frame.Text(Json.encodeToString(filesMsg)))

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
                            val msg = FileChangedMessage(
                                type = "fileChanged",
                                file = FileInfo("_tiny.json", content),
                            )
                            outgoing.send(Frame.Text(Json.encodeToString(msg)))
                        }

                        currentScripts.forEach { script ->
                            val file = gameDirectory.resolve(script)
                            if (file.exists()) {
                                val modified = file.lastModified()
                                if (modified != lastModified[script]) {
                                    lastModified[script] = modified
                                    val content = file.readText()
                                    val msg = FileChangedMessage(
                                        type = "fileChanged",
                                        file = FileInfo(script, content),
                                    )
                                    outgoing.send(Frame.Text(Json.encodeToString(msg)))
                                }
                            }
                        }
                    }
                }

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
        }

        val address = "http://localhost:$port?debugPort=$debug"
        echo("\uD83D\uDC1B Web debugger starting on $address")

        Runtime.getRuntime().addShutdownHook(
            Thread {
                server.stop()
                echo("\uD83D\uDC4B See you soon!")
            },
        )

        try {
            java.awt.Desktop.getDesktop().browse(URI(address))
        } catch (_: Exception) {
            echo("\uD83D\uDCA1 Open $address in your browser.")
        }

        server.start(wait = true)
    }

    private fun loadDebuggerResources(): Map<String, ByteArray> {
        val resources = mutableMapOf<String, ByteArray>()

        val debuggerZip = DebugCommand2::class.java
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

    private fun buildFilesList(
        gameDir: File,
        gameParameters: GameParameters,
    ): List<FileInfo> {
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
