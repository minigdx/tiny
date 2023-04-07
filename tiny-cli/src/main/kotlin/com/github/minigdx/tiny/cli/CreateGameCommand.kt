package com.github.minigdx.tiny.cli

import com.github.minigdx.tiny.cli.config.GameParameters
import com.github.minigdx.tiny.cli.config.GameParametersV1
import com.github.minigdx.tiny.cli.config.Size
import com.github.minigdx.tiny.log.StdOutLogger
import kotlinx.serialization.json.encodeToStream
import picocli.CommandLine
import java.io.File
import java.io.FileOutputStream
import java.util.concurrent.Callable

@CommandLine.Command(name = "--create", description = ["I'm a subcommand of `foo`"])
class CreateGameCommand : Callable<Int> {
    @CommandLine.ParentCommand
    private val parent: TinyCommand? = null // picocli injects reference to parent command

    lateinit var logger: StdOutLogger

    override fun call(): Int {
        logger = parent!!.logger
        // println("hi form bar, size=$w x $h")
        createGame(parent.gameDirectory)
        return 23
    }

    private fun createGame(dir: File) {
        fun ask(
            question: String,
            defaultValue: String?,
            options: List<String> = emptyList()
        ): String {
            var input: String?

            do {
                print(question)
                if(options.isEmpty()) {
                    print(defaultValue?.let { " [$it]" } ?: "")
                } else {
                    println(defaultValue?.let { " [$it]" } ?: "")
                }
                options.forEachIndexed { index, option ->
                    println("(${index}): $option")
                }
                input = readlnOrNull()

                input = if (input?.isNotBlank() == true) {
                    input
                } else {
                    defaultValue
                }
            } while (input == null)

            return input
        }
        logger.info("TINY-CLI") { "Create a new game in the game folder $dir" }

        if (!dir.exists()) {
            dir.mkdirs()
        }


        val name = ask("🏷 Name of your game?","My tiny game")
        val gameResolution = ask("🖥 Game resolution?" , "256x256")
        val gameSprite = ask("📐 Size of a sprite?" , "16x16")
        val palettes = GamePalette.ALL
        val paletteAnswer = ask(
            "🎨 Which color palette?",
            "0",
            palettes.map { it.name }
        )

        // Custom chose
        val colors = if(paletteAnswer.toInt() == palettes.size) {
            val colors = ask(
                "🎨 Please type colors separated by coma (ie: #FFFFFF, #ABCDEF)",
                null
            )

            colors.split(",").map { it.trim() }
        } else {
            GamePalette.ALL[paletteAnswer.toInt()].colors
        }

        val parameters: GameParameters = GameParametersV1(
            name = name,
            resolution = gameResolution.split("x").let {
                val (x, y) = it
                Size(x.toInt(), y.toInt())
            },
            sprites = gameSprite.split("x").let {
                val (x, y) = it
                Size(x.toInt(), y.toInt())
            },
            zoom = 2,
            colors = colors
        )

        val config = dir.resolve("_tiny.json")
        parent!!.json.encodeToStream(parameters, FileOutputStream(config))


        CreateGameCommand::class.java.getResourceAsStream("/game.lua")
        logger.info("TINY-CLI") { "Game configuration created!" }

        parent.runGame(parameters, dir)
    }
}
