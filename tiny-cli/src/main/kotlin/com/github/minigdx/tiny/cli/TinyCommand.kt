package com.github.minigdx.tiny.cli

import com.github.minigdx.tiny.cli.config.GameParameters
import com.github.minigdx.tiny.engine.GameEngine
import com.github.minigdx.tiny.file.CommonVirtualFileSystem
import com.github.minigdx.tiny.log.StdOutLogger
import com.github.minigdx.tiny.platform.glfw.GlfwPlatform
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.decodeFromStream
import picocli.CommandLine.Command
import picocli.CommandLine.Option
import picocli.CommandLine.Parameters
import java.io.File
import java.io.FileInputStream
import java.util.concurrent.Callable


@Command(
    name = "tiny",
    mixinStandardHelpOptions = true,
    description = ["Start the tiny game engine. Create or run game using it!"],
    subcommands = [CreateGameCommand::class]
)
class TinyCommand : Callable<Int> {

    @Parameters(
        index = "0",
        arity = "0..1",
        description = ["The game directory containing all game resources."]
    )
    internal val gameDirectory: File = File(".")

    @Option(names = ["--export"], description = ["Export the game as a web game."])
    private var export: Boolean = false

    /*
    @Option(
        names = ["--create"],
        description = ["Create a new game in the game directory."]
    )
    private var create: Boolean = false
*/
    internal val logger = StdOutLogger()

    internal val json = Json {
        ignoreUnknownKeys = true
    }

    override fun call(): Int {
        logger.info("TINY-CLI") { "Boot the Tiny cli to use the game folder ${gameDirectory.absolutePath}" }
/*
        if (create) {
            createGame(gameDirectory)
        }
*/
        val configFile = gameDirectory.resolve("_tiny.json")
        if (!configFile.exists()) {
            logger.error("TINY-CLI") {
                "The directory ${gameDirectory.absolutePath} doesn't contains _tiny.json file. " +
                    "It means that the game directory is invalid or corrupted. " +
                    "Check that the game directory is valid and is a tiny game, " +
                    "containing a valid _tiny.json file."
            }
            return -1
        }


        val gameParameters = json.decodeFromStream<GameParameters>(FileInputStream(configFile))

        if (export) {
            exportGame(gameDirectory, gameParameters)
            return 0
        }

        runGame(gameParameters, gameDirectory)
        return 0
    }

    internal fun runGame(gameParameters: GameParameters, dir: File) {
        try {
            val vfs = CommonVirtualFileSystem()
            val gameOption = gameParameters.toGameOptions()
            GameEngine(
                gameOptions = gameOption,
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
    }

    private fun exportGame(dir: File, gameParameters: GameParameters) {
        logger.info("TINY-CLI") { "Export the game from the game folder ${dir.absolutePath}" }
        // copy files from dir - only game.lua / game folder / game.png
        // copy files from the JS project
        // update the index.html : the <tiny-game> tag should reflect the gameParameters.
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
