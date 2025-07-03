package com.github.minigdx.tiny.cli.command

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.Context
import com.github.ajalt.clikt.parameters.arguments.argument
import com.github.ajalt.clikt.parameters.arguments.default
import com.github.ajalt.clikt.parameters.options.default
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.types.file
import com.github.ajalt.clikt.parameters.types.int
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.Application
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import io.ktor.server.response.respond
import io.ktor.server.response.respondBytes
import io.ktor.server.routing.RoutingContext
import io.ktor.server.routing.get
import io.ktor.server.routing.head
import io.ktor.server.routing.routing
import java.io.File
import java.io.FileInputStream
import java.util.zip.ZipInputStream

class ServeCommand : CliktCommand(name = "serve") {
    private val port by option(help = "Port of the local webserver.")
        .int()
        .default(8080)

    private val gameDirectory by argument(help = "The game to serve by an embedded web server.")
        .file(mustExist = true, canBeDir = true, canBeFile = true)
        .default(File("."))

    private val resources = mutableMapOf<String, ByteArray>()

    override fun help(context: Context) = "Run your game as a web game."

    override fun run() {
        // Get the zip
        val zipFile = if (gameDirectory.isDirectory) {
            GameExporter(withSourceMap = true).export(gameDirectory, "tiny-export.zip")
            gameDirectory.resolve("tiny-export.zip")
        } else {
            gameDirectory
        }

        // Uncompressed in memory
        val zip = ZipInputStream(FileInputStream(zipFile))

        var entry = zip.nextEntry
        while (entry != null) {
            resources[entry.name] = zip.readAllBytes()
            zip.closeEntry()
            entry = zip.nextEntry
        }

        val method = fun Application.() {
            routing {
                head("/{...}") {
                    if (resources.containsKey(resourceKey())) {
                        call.respond(HttpStatusCode.OK)
                    } else {
                        call.respond(HttpStatusCode.NotFound)
                    }
                }
                get("/{...}") {
                    val key = resourceKey()
                    if (resources.containsKey(key)) {
                        val value = resources[key]
                        if (value != null) {
                            val contentType =
                                if (key.endsWith(".js")) {
                                    ContentType.Application.JavaScript
                                } else if (key.endsWith(".png")) {
                                    ContentType.Image.PNG
                                } else if (key.endsWith(".json")) {
                                    ContentType.Application.Json
                                } else if (key.endsWith(".ldtk")) {
                                    ContentType.Application.Json
                                } else {
                                    ContentType.Application.OctetStream
                                }
                            call.respondBytes(value, contentType)
                        } else {
                            call.respond(HttpStatusCode.NotFound)
                        }
                    } else {
                        call.respond(HttpStatusCode.NotFound)
                    }
                }
                get("/") {
                    val key = "index.html"
                    val value = resources[key]
                    if (value != null) {
                        call.respondBytes(value, ContentType.Text.Html)
                    } else {
                        call.respond(HttpStatusCode.NotFound)
                    }
                }
            }
        }

        // Start a webserver using ktor
        // Creates a Netty server
        val server = embeddedServer(Netty, port = port, module = method)

        echo("\uD83D\uDE80 Try your game on http://localhost:$port with your browser.")

        Runtime.getRuntime().addShutdownHook(
            Thread {
                server.stop()
                echo("\uD83D\uDC4B See you soon!")
            },
        )
        // Starts the server and waits for the engine to stop and exits.
        server.start(wait = true)
        // start a browser to the address
        // route to files from the zip.
    }

    private fun RoutingContext.resourceKey(): String {
        val key =
            call.request.local.uri.let { k ->
                // Small hack as the engine add a /.
                // Need to fix it...
                if (k.startsWith("/")) {
                    k.drop(1)
                } else {
                    k
                }
            }
        return key
    }
}
