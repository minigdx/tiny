package com.github.minigdx.tiny.cli

import com.github.ajalt.clikt.core.Abort
import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.subcommands
import com.github.ajalt.clikt.parameters.arguments.argument
import com.github.ajalt.clikt.parameters.arguments.default
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.options.prompt
import com.github.ajalt.clikt.parameters.options.validate
import com.github.ajalt.clikt.parameters.types.file
import com.github.ajalt.clikt.parameters.types.int
import com.github.minigdx.tiny.cli.config.GameParameters
import com.github.minigdx.tiny.engine.GameEngine
import com.github.minigdx.tiny.file.CommonVirtualFileSystem
import com.github.minigdx.tiny.log.StdOutLogger
import com.github.minigdx.tiny.platform.glfw.GlfwPlatform
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.decodeFromStream
import java.io.File
import java.io.FileInputStream

class CreateCommand : CliktCommand(name = "create") {

    val gameDirectory by argument(help = "The directory containing all game information")
        .file(mustExist = false, canBeDir = true, canBeFile = false)
        .default(File("."))

    private val gameName by option(help = "🏷 The name of the game")
        .prompt(default = generateRandomGameName())

    private val gameResolution by option(help = "🖥 The game resolution (e.g., 800x600)")
        .prompt(default = "256x256")
        .validate { require(it.matches(Regex("\\d+x\\d+"))) { "Invalid resolution format: $it" } }

    private val spriteSize by option(help = "📐 The sprite size (e.g., 16x16)")
        .prompt(default = "16x16")
        .validate { require(it.matches(Regex("\\d+x\\d+"))) { "Invalid resolution format: $it" } }

    private val spritesheets by option(help = "The filenames of the sprite sheets, separated by a comma (e.g., file1.png, file2.png)")
        .prompt(default = "")
        .validate {
            require(
                it.split(",")
                    .all { f -> f.trim().endsWith(".png") }) { "Invalid image file $it. Only *.png are supported" }
        }

    private val palette by option(help = "🎨 The Color palette to use")
        .int()
        .prompt(
            """Please choose a game color palette:
${
                GamePalette.ALL.mapIndexed { index, gamePalette ->
                    "[$index] ${gamePalette.name}"
                }.joinToString("\n")
            }
"""
        )

    override fun run() {
        echo("Welcome to the game wizard!")
        echo("Let's create a new game...")
        echo()

        echo("Game Name: $gameName")
        echo("Game Resolution: ${gameResolution}")
        echo("Game Resolution: ${spriteSize}")
        echo("Sprite Sheet Filenames: ${spritesheets}")
        echo("Folder: ${gameDirectory.absolutePath}")
        echo("palette: $palette")
    }

    private fun generateRandomGameName(): String {
        val adjectives = listOf("Funny", "Awesome", "Crazy", "Epic", "Mystical", "Magical")
        val nouns = listOf("Unicorns", "Pandas", "Robots", "Dragons", "Ninjas", "Pirates")
        return "${adjectives.random()} ${nouns.random()} Game"
    }
}

class MainCommand : CliktCommand(invokeWithoutSubcommand = true) {

    val gameDirectory by argument(help = "The directory containing all game information")
        .file(mustExist = true, canBeDir = true, canBeFile = false)
        .default(File("."))

    init {
        subcommands(CreateCommand())
    }

    internal val json = Json {
        ignoreUnknownKeys = true
    }

    override fun run() {
        val subcommand = currentContext.invokedSubcommand
        if (subcommand == null) {
            try {
                val configFile = gameDirectory.resolve("_tiny.json")
                if (!configFile.exists()) {
                    echo("No _tiny.json")
                    throw Abort()
                }
                val gameParameters = json.decodeFromStream<GameParameters>(FileInputStream(configFile))

                val logger = StdOutLogger()
                val vfs = CommonVirtualFileSystem()
                val gameOption = gameParameters.toGameOptions()
                GameEngine(
                    gameOptions = gameOption,
                    platform = GlfwPlatform(gameOption, logger, vfs, gameDirectory),
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


fun main(vararg args: String) {
    MainCommand().main(args)
}
