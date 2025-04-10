package com.github.minigdx.tiny.cli.command

import com.github.ajalt.clikt.core.Abort
import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.Context
import com.github.ajalt.clikt.parameters.arguments.argument
import com.github.ajalt.clikt.parameters.arguments.default
import com.github.ajalt.clikt.parameters.types.file
import com.github.minigdx.tiny.cli.config.GameParameters
import com.github.minigdx.tiny.engine.GameEngine
import com.github.minigdx.tiny.engine.TinyException
import com.github.minigdx.tiny.file.CommonVirtualFileSystem
import com.github.minigdx.tiny.file.JvmLocalFile
import com.github.minigdx.tiny.log.StdOutLogger
import com.github.minigdx.tiny.lua.WorkspaceLib
import com.github.minigdx.tiny.lua.errorLine
import com.github.minigdx.tiny.platform.glfw.GlfwPlatform
import com.github.minigdx.tiny.render.LwjglGLRender
import kotlinx.serialization.json.decodeFromStream
import org.luaj.vm2.LuaError
import java.io.File

class SfxCommand : CliktCommand(name = "sfx") {
    val gameDirectory by argument(help = "The directory containing all game information")
        .file(mustExist = true, canBeDir = true, canBeFile = false)
        .default(File("."))

    fun isOracleOrOpenJDK(): Boolean {
        val vendor = System.getProperty("java.vendor")?.lowercase()
        return vendor?.contains("oracle") == true || vendor?.contains("eclipse") == true || vendor?.contains("openjdk") == true
    }

    fun isMacOS(): Boolean {
        val os = System.getProperty("os.name").lowercase()
        return os.contains("mac") || os.contains("darwin")
    }

    override fun help(context: Context) = "Start the SFX Editor"

    override fun run() {
        if (isMacOS() && isOracleOrOpenJDK()) {
            echo("\uD83D\uDEA7 === The Tiny CLI on Mac with require a special option.")
            echo("\uD83D\uDEA7 === If the application crash ➡ use the command 'tiny-cli-mac' instead.")
        }

        try {
            val configFile = SfxCommand::class.java.getResourceAsStream("/sfx/_tiny.json")
            if (configFile == null) {
                echo(
                    "\uD83D\uDE2D No _tiny.json found! Can't run the game without. " +
                        "The tiny-cli command doesn't seems to be bundled correctly. You might want to report an issue.",
                )
                throw Abort()
            }
            val commandParameters = GameParameters.JSON.decodeFromStream<GameParameters>(configFile)

            val gameConfig = gameDirectory.resolve("_tiny.json")
            if (gameConfig.exists()) {
                val parameters = GameParameters.read(gameConfig)
                WorkspaceLib.DEFAULT =
                    parameters.toGameOptions().sounds.map {
                        JvmLocalFile(it, gameDirectory)
                    }
            } else {
                WorkspaceLib.DEFAULT = listOf(JvmLocalFile("sfx1.sfx", workingDirectory = gameDirectory))
            }

            val logger = StdOutLogger("tiny-cli")
            val vfs = CommonVirtualFileSystem()
            val commandOptions = commandParameters.toGameOptions()
            val gameEngine =
                GameEngine(
                    gameOptions = commandOptions,
                    platform =
                        GlfwPlatform(
                            commandOptions,
                            logger,
                            vfs,
                            File("."),
                            LwjglGLRender(logger, commandOptions),
                            jarResourcePrefix = "/sfx",
                        ),
                    vfs = vfs,
                    logger = logger,
                )
            Runtime.getRuntime().addShutdownHook(
                Thread {
                    gameEngine.end()
                    echo("\uD83D\uDC4B See you soon!")
                },
            )
            gameEngine.main()
        } catch (ex: Exception) {
            echo(
                "\uD83E\uDDE8 An unexpected exception occurred. " +
                    "The application will stop. " +
                    "It might be a bug in Tiny. " +
                    "If so, please report it.",
            )
            when (ex) {
                is LuaError -> {
                    val (nb, line) = ex.errorLine() ?: (null to null)
                    echo("Error found line $nb:$line")
                }
                is TinyException -> {
                    echo("Error found in the script ${ex.name} on line ${ex.lineNumber}:${ex.line}")
                }
            }
            echo()
            ex.printStackTrace()
        }
    }
}
