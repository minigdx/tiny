package com.github.minigdx.tiny.cli

import com.github.minigdx.tiny.cli.config.GameParameters
import com.github.minigdx.tiny.cli.config.GameParametersV1
import com.github.minigdx.tiny.cli.config.Size
import com.github.minigdx.tiny.engine.GameEngine
import com.github.minigdx.tiny.file.CommonVirtualFileSystem
import com.github.minigdx.tiny.log.StdOutLogger
import com.github.minigdx.tiny.platform.glfw.GlfwPlatform
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.decodeFromStream
import kotlinx.serialization.json.encodeToStream
import picocli.CommandLine.Command
import picocli.CommandLine.Option
import picocli.CommandLine.Parameters
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.util.concurrent.Callable


@Command(
    name = "tiny",
    mixinStandardHelpOptions = true,
    description = ["Start the tiny game engine. Create or run game using it!"]
)
class TinyCommand : Callable<Int> {

    @Parameters(
        index = "0",
        arity = "0..1",
        description = ["The game directory containing all game resources."]
    )
    private val gameDirectory: File? = null

    @Option(names = ["--export"], description = ["Export the game as a web game."])
    private var export: Boolean = false

    @Option(
        names = ["--create"],
        description = ["Create a new game in the game directory."]
    )
    private var create: Boolean = false

    private val logger = StdOutLogger()

    private val json = Json {
        ignoreUnknownKeys = true
    }

    override fun call(): Int {

        val dir = gameDirectory ?: File(".")

        logger.info("TINY-CLI") { "Boot the Tiny cli to use the game folder ${dir.absolutePath}" }

        if (create) {
            createGame(dir)
        }

        val configFile = dir.resolve("_tiny.json")
        if (!configFile.exists()) {
            logger.error("TINY-CLI") {
                "The directory ${dir.absolutePath} doesn't contains _tiny.json file. " +
                    "It means that the game directory is invalid or corrupted. " +
                    "Check that the game directory is valid and is a tiny game, " +
                    "containing a valid _tiny.json file."
            }
            return -1
        }


        val gameParameters = json.decodeFromStream<GameParameters>(FileInputStream(configFile))

        if (export) {
            exportGame(dir, gameParameters)
            return 0
        }

        try {
            val vfs = CommonVirtualFileSystem()
            val gameOption = gameParameters.toGameOptions()
            GameEngine(
                gameOption = gameOption,
                platform = GlfwPlatform(gameOption, logger, vfs, dir),
                vfs = vfs,
                logger = logger,
            ).main()
        } catch (ex: Exception) {
            logger.error(
                "TINY-CLI",
                ex
            ) { "An unexpected exception occurred. The application will stop. It might be a bug in Tiny. If so, please report it." }
        }
        return 0
    }

    private fun exportGame(dir: File, gameParameters: GameParameters) {
        logger.info("TINY-CLI") { "Export the game from the game folder ${dir.absolutePath}" }
        // copy files from dir - only game.lua / game folder / game.png
        // copy files from the JS project
        // update the index.html : the <tiny-game> tag should reflect the gameParameters.
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
        json.encodeToStream(parameters, FileOutputStream(config))

        // TODO: - copy game.lua, README.MD from jar.
        logger.info("TINY-CLI") { "Game configuration created!" }
    }
}

class GamePalette(val name: String, val colors: List<String>) {
    companion object {
        val PICO8 = GamePalette("pico8", listOf(
            "#000000",
            "#1D2B53",
            "#7E2553",
            "#008751",
            "#AB5236",
            "#5F574F",
            "#C2C3C7",
            "#FFF1E8",
            "#FF004D",
            "#FFA300",
            "#FFEC27",
            "#00E436",
            "#29ADFF",
            "#83769C",
            "#FF77A8",
            "#FFCCAA"
        ))

        val GAMEBOY = GamePalette("gameboy", listOf(
            "#E0F8D0", // Lightest
            "#88C070",
            "#346856",
            "#081820"  // Darkest
        ))

        val ALL = listOf(PICO8, GAMEBOY)
    }
}
