package com.github.minigdx.tiny.cli.command

import com.github.ajalt.clikt.core.Abort
import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.parameters.arguments.argument
import com.github.ajalt.clikt.parameters.arguments.default
import com.github.ajalt.clikt.parameters.options.default
import com.github.ajalt.clikt.parameters.options.flag
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.types.file
import com.github.ajalt.clikt.parameters.types.int
import com.github.minigdx.tiny.cli.config.GameParameters
import com.github.minigdx.tiny.cli.debug.DebuggerExecutionListener
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
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.luaj.vm2.LuaError
import java.io.File
import kotlin.time.ExperimentalTime

class RunCommand : CliktCommand(name = "run", help = "Run your game.") {

    val gameDirectory by argument(help = "The directory containing all game information.")
        .file(mustExist = true, canBeDir = true, canBeFile = false)
        .default(File("."))

    val test by option(help = "Run tests before running the game.")
        .flag()

    val debug by option(help = "Port used for debugging")
        .int()
        .default(8080)

    private fun isOracleOrOpenJDK(): Boolean {
        val vendor = System.getProperty("java.vendor")?.lowercase()
        return vendor?.contains("oracle") == true || vendor?.contains("eclipse") == true || vendor?.contains("openjdk") == true
    }

    private fun isMacOS(): Boolean {
        val os = System.getProperty("os.name").lowercase()
        return os.contains("mac") || os.contains("darwin")
    }

    @OptIn(ExperimentalTime::class)
    override fun run() {
        if (isMacOS() && isOracleOrOpenJDK()) {
            echo("\uD83D\uDEA7 === The Tiny CLI on Mac with require a special option.")
            echo("\uD83D\uDEA7 === If the application crash ➡ use the command 'tiny-cli-mac' instead.")
        }

        echo("\uD83D\uDC1B === Running the game using debugger on the port '$debug' ===")
        echo("\uD83D\uDC1B === Use the command 'tiny-cli debug' to connect the debugger to your game  ===")

        val debugCommandReceiver = Channel<DebugRemoteCommand>()
        val engineCommandSender = Channel<EngineRemoteCommand>()

        embeddedServer(
            Netty,
            port = debug,
            configure = {
                shutdownTimeout = 0
                shutdownGracePeriod = 0
            },
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
            val configFile = gameDirectory.resolve("_tiny.json")
            if (!configFile.exists()) {
                echo("\uD83D\uDE2D No _tiny.json found! Can't run the game without.")
                throw Abort()
            }
            val gameParameters = GameParameters.read(configFile)

            val logger = StdOutLogger("tiny-cli", level = LogLevel.INFO)

            val vfs = CommonVirtualFileSystem()
            val gameOption = gameParameters.toGameOptions()
                .copy(runTests = test)

            val debugListener = DebuggerExecutionListener(debugCommandReceiver, engineCommandSender)

            val gameEngine = GameEngine(
                gameOptions = gameOption,
                platform = GlfwPlatform(gameOption, logger, vfs, gameDirectory, LwjglGLRender(logger, gameOption)),
                vfs = vfs,
                logger = logger,
                listener = object : GameEngineListener {

                    override fun switchScript(before: GameScript?, after: GameScript?) {
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

            val data = File("data")
            if (data.exists() && data.isDirectory) {
                WorkspaceLib.DEFAULT = data.listFiles()?.map { JvmLocalFile(it.name, data) } ?: emptyList()
            }
            gameEngine.main()
        } catch (ex: Exception) {
            echo("\uD83E\uDDE8 An unexpected exception occurred. The application will stop. It might be a bug in Tiny. If so, please report it.")
            when (ex) {
                // FIXME: catch TinyException?
                is LuaError -> {
                    val (nb, line) = ex.errorLine() ?: (null to null)
                    echo("Error found line $nb:$line")
                }

                is TinyException -> {
                    echo("Error found line ${ex.lineNumber}:${ex.line}")
                }
            }
            echo()
            ex.printStackTrace()
        }
    }
}
