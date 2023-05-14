package com.github.minigdx.tiny.cli.command

import com.github.ajalt.clikt.core.Abort
import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.parameters.arguments.argument
import com.github.ajalt.clikt.parameters.arguments.default
import com.github.ajalt.clikt.parameters.types.file
import com.github.minigdx.tiny.cli.config.GameParameters
import com.github.minigdx.tiny.engine.GameEngine
import com.github.minigdx.tiny.file.CommonVirtualFileSystem
import com.github.minigdx.tiny.log.StdOutLogger
import com.github.minigdx.tiny.platform.glfw.GlfwPlatform
import com.github.minigdx.tiny.render.LwjglGLRender
import kotlinx.serialization.json.decodeFromStream
import java.io.File
import java.io.FileInputStream

class RunCommand : CliktCommand("run") {

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

    override fun run() {
        if (isMacOS() && isOracleOrOpenJDK()) {
            echo("\uD83D\uDEA7 === The Tiny CLI on Mac with require a special option.")
            echo("\uD83D\uDEA7 === If the application crash ➡ use the command 'tiny-cli-mac' instead.")
        }

        try {
            val configFile = gameDirectory.resolve("_tiny.json")
            if (!configFile.exists()) {
                echo("\uD83D\uDE2D No _tiny.json found! Can't run the game without.")
                throw Abort()
            }
            val gameParameters = GameParameters.JSON.decodeFromStream<GameParameters>(FileInputStream(configFile))

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
            echo("\uD83E\uDDE8 An unexpected exception occurred. The application will stop. It might be a bug in Tiny. If so, please report it.")
            ex.printStackTrace()
        }
    }
}
