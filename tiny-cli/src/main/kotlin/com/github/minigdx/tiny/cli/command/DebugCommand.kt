package com.github.minigdx.tiny.cli.command

import com.github.ajalt.clikt.core.Abort
import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.Context
import com.github.ajalt.clikt.parameters.options.default
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.types.file
import com.github.ajalt.clikt.parameters.types.int
import com.github.minigdx.tiny.cli.config.GameParameters
import io.ktor.http.ContentType
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import io.ktor.server.response.respondText
import io.ktor.server.routing.get
import io.ktor.server.routing.routing
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import java.io.File

class DebugCommand : CliktCommand(name = "debug") {
    val gameDirectory by option("-d", "--directory", help = "The directory containing all game information.")
        .file(mustExist = true, canBeDir = true, canBeFile = false)
        .default(File("."))

    val debug by option(help = "Debug port used by the game.")
        .int()
        .default(8081)

    val webPort by option("--web-port", help = "Port for the web debugger UI.")
        .int()
        .default(8082)

    override fun help(context: Context) = "Debug the current game"

    override fun run() {
        val configFile = gameDirectory.resolve("_tiny.json")
        if (!configFile.exists()) {
            echo("\uD83D\uDE2D No _tiny.json found! Can't run the game without.")
            throw Abort()
        }
        val gameParameters = GameParameters.read(configFile)

        val debuggerHtml = DebugCommand::class.java.getResource("/debugger/index.html")?.readText()
            ?: throw IllegalStateException("Debugger HTML resource not found")

        val configJson = buildJsonObject {
            put("debugPort", debug.toString())
            put("gameName", gameParameters.name)
        }.toString()

        echo("\uD83D\uDD0D Starting web debugger at http://localhost:$webPort")
        echo("\uD83D\uDD0D Make sure the game is running with 'tiny-cli run' on port $debug")

        val server = embeddedServer(
            factory = Netty,
            port = webPort,
        ) {
            routing {
                get("/") {
                    call.respondText(debuggerHtml, ContentType.Text.Html)
                }
                get("/api/config") {
                    call.respondText(configJson, ContentType.Application.Json)
                }
                get("/api/scripts") {
                    val scripts = gameParameters.getAllScripts()
                        .associateWith { scriptPath ->
                            gameDirectory.resolve(scriptPath).readText()
                        }
                    call.respondText(
                        Json.encodeToString<Map<String, String>>(scripts),
                        ContentType.Application.Json,
                    )
                }
            }
        }

        Runtime.getRuntime().addShutdownHook(
            Thread {
                server.stop()
                echo("\uD83D\uDC4B See you soon!")
            },
        )

        server.start(wait = true)
    }
}
