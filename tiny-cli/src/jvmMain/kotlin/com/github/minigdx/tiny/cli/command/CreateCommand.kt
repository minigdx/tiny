package com.github.minigdx.tiny.cli.command

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.parameters.arguments.argument
import com.github.ajalt.clikt.parameters.arguments.default
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.options.prompt
import com.github.ajalt.clikt.parameters.options.validate
import com.github.ajalt.clikt.parameters.types.file
import com.github.ajalt.clikt.parameters.types.int
import com.github.minigdx.tiny.cli.GamePalette
import java.io.File

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
                    .all { f -> f.trim().endsWith(".png") }
            ) { "Invalid image file $it. Only *.png are supported" }
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
        echo("Game Resolution: $gameResolution")
        echo("Game Resolution: $spriteSize")
        echo("Sprite Sheet Filenames: $spritesheets")
        echo("Folder: ${gameDirectory.absolutePath}")
        echo("palette: $palette")
    }

    private fun generateRandomGameName(): String {
        val adjectives = listOf("Funny", "Awesome", "Crazy", "Epic", "Mystical", "Magical")
        val nouns = listOf("Unicorns", "Pandas", "Robots", "Dragons", "Ninjas", "Pirates")
        return "${adjectives.random()} ${nouns.random()} Game"
    }
}
