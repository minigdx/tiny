package com.github.minigdx.tiny.cli.command

import com.github.ajalt.clikt.core.Abort
import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.subcommands
import com.github.ajalt.clikt.parameters.arguments.argument
import com.github.ajalt.clikt.parameters.arguments.default
import com.github.ajalt.clikt.parameters.types.file
import com.github.minigdx.tiny.cli.config.GameParameters
import com.github.minigdx.tiny.cli.config.GameParameters.Companion.JSON
import com.github.minigdx.tiny.engine.GameEngine
import com.github.minigdx.tiny.file.CommonVirtualFileSystem
import com.github.minigdx.tiny.log.StdOutLogger
import com.github.minigdx.tiny.platform.glfw.GlfwPlatform
import com.github.minigdx.tiny.render.LwjglGLRender
import kotlinx.serialization.json.decodeFromStream
import java.io.File
import java.io.FileInputStream

class MainCommand : CliktCommand(invokeWithoutSubcommand = true) {

    val gameDirectory by argument(help = "The directory containing all game information")
        .file(mustExist = true, canBeDir = true, canBeFile = false)
        .default(File("."))

    init {
        subcommands(CreateCommand(), AddCommand(), ExportCommand(), ServeCommand())
    }

    override fun run() {
        echo("\uD83E\uDDF8 ʕ　·ᴥ·ʔ TINY - The Tiny Virtual Gaming Console")
        val subcommand = currentContext.invokedSubcommand
        if (subcommand == null) {
            try {
                val configFile = gameDirectory.resolve("_tiny.json")
                if (!configFile.exists()) {
                    echo("No _tiny.json")
                    throw Abort()
                }
                val gameParameters = JSON.decodeFromStream<GameParameters>(FileInputStream(configFile))

                val logger = StdOutLogger("tiny-cli")
                val vfs = CommonVirtualFileSystem()
                val gameOption = gameParameters.toGameOptions()
                GameEngine(
                    gameOptions = gameOption,
                    platform = GlfwPlatform(gameOption, logger, vfs, gameDirectory, LwjglGLRender(logger, gameOption)),
                    vfs = vfs,
                    logger = logger,
                ).main()
            } catch (ex: Exception) {
                echo("An unexpected exception occurred. The application will stop. It might be a bug in Tiny. If so, please report it.")
                ex.printStackTrace()
            }
        }
    }
}