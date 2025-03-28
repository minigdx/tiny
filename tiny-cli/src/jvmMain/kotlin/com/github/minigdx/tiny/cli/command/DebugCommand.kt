package com.github.minigdx.tiny.cli.command

import com.github.ajalt.clikt.core.Abort
import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.parameters.arguments.argument
import com.github.ajalt.clikt.parameters.arguments.default
import com.github.ajalt.clikt.parameters.options.default
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.types.file
import com.github.ajalt.clikt.parameters.types.int
import com.github.minigdx.tiny.cli.config.GameParameters
import com.github.minigdx.tiny.cli.ui.TinyDebuggerUI
import io.ktor.client.HttpClient
import io.ktor.client.plugins.websocket.WebSockets
import io.ktor.client.plugins.websocket.webSocketSession
import io.ktor.websocket.Frame
import io.ktor.websocket.close
import io.ktor.websocket.readText
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.ReceiveChannel
import kotlinx.coroutines.channels.SendChannel
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File
import javax.swing.SwingUtilities

class DebugCommand : CliktCommand(name = "debug", help = "Debug the current game") {

    val gameDirectory by argument(help = "The directory containing all game information.")
        .file(mustExist = true, canBeDir = true, canBeFile = false)
        .default(File("."))

    val debug by option(help = "Debug port used by the game.")
        .int()
        .default(8080)

    override fun run() {
        val configFile = gameDirectory.resolve("_tiny.json")
        if (!configFile.exists()) {
            echo("\uD83D\uDE2D No _tiny.json found! Can't run the game without.")
            throw Abort()
        }
        val gameParameters = GameParameters.read(configFile)

        val debugCommandSender = Channel<DebugRemoteCommand>()
        val engineCommandReceiver = Channel<EngineRemoteCommand>()

        SwingUtilities.invokeLater {
            TinyDebuggerUI(debugCommandSender, engineCommandReceiver, gameParameters).apply { isVisible = true }
        }

        runBlocking {
            val client = HttpClient {
                install(WebSockets)
            }

            var connected = false
            while (!connected) {
                try {
                    connectToGame(client, debugCommandSender, engineCommandReceiver)
                    connected = true
                } catch (ex: Exception) {
                    delay(500)
                    connected = false
                }
            }
        }
    }

    private suspend fun connectToGame(
        client: HttpClient,
        channel: ReceiveChannel<DebugRemoteCommand>,
        received: SendChannel<EngineRemoteCommand>,
    ) {
        val session = client.webSocketSession("ws://localhost:$debug/debug")

        coroutineScope {
            launch {
                for (message in channel) {
                    session.outgoing.send(Frame.Text(Json.encodeToString(message)))
                    if(message is Disconnect) {
                        session.close()
                    }
                }
            }

            launch {
                for (message in session.incoming) {
                    if (message is Frame.Text) {
                        received.send(Json.decodeFromString(message.readText()))
                    }
                }
            }
        }
    }
}
