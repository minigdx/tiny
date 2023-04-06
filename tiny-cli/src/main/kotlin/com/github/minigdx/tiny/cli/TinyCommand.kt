package com.github.minigdx.tiny.cli

import com.github.minigdx.tiny.engine.GameEngine
import com.github.minigdx.tiny.engine.GameOption
import com.github.minigdx.tiny.file.CommonVirtualFileSystem
import com.github.minigdx.tiny.log.StdOutLogger
import com.github.minigdx.tiny.platform.glfw.GlfwPlatform
import picocli.CommandLine.Command
import picocli.CommandLine.Option
import picocli.CommandLine.Parameters
import java.io.File
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

    @Option(names = ["--create"], description = ["Create a new game in the game directory."])
    private var create: Boolean = false

    override fun call(): Int {
        val logger = StdOutLogger()

        val dir = gameDirectory ?: File(".")

        logger.info("TINY-CLI") { "Boot the Tiny cli to use the game folder ${dir.absolutePath}"}

        if(create) {
            logger.info("TINY-CLI") { "Create a new game in the game folder ${dir.absolutePath}"}
        }

        if(export) {
            logger.info("TINY-CLI") { "Export the game from the game folder ${dir.absolutePath}"}
        }

        try {
            val vfs = CommonVirtualFileSystem()
            // FIXME: how to store game options? JSON ?
            val gameOption = GameOption(
                256,
                256,
                2,
                spriteSize = 16 to 16
            )

            GameEngine(
                gameOption = gameOption,
                platform = GlfwPlatform(gameOption, logger, vfs),
                vfs = vfs,
                logger = logger,
            ).main()
        } catch (ex: Exception) {
            logger.error("TINY-CLI", ex) { "An unexpected exception occurred. The application will stop. It might be a bug in Tiny. If so, please report it."}
        }
        return 0
    }
}
