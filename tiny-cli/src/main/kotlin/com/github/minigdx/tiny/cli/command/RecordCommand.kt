package com.github.minigdx.tiny.cli.command

import com.github.ajalt.clikt.core.Abort
import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.Context
import com.github.ajalt.clikt.parameters.arguments.argument
import com.github.ajalt.clikt.parameters.arguments.default
import com.github.ajalt.clikt.parameters.options.default
import com.github.ajalt.clikt.parameters.options.flag
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.types.file
import com.github.ajalt.clikt.parameters.types.int
import com.github.ajalt.clikt.parameters.types.long
import com.github.minigdx.tiny.cli.config.GameParameters
import com.github.minigdx.tiny.engine.GameEngine
import com.github.minigdx.tiny.engine.GameEngineListener
import com.github.minigdx.tiny.file.CommonVirtualFileSystem
import com.github.minigdx.tiny.log.LogLevel
import com.github.minigdx.tiny.log.StdOutLogger
import com.github.minigdx.tiny.platform.glfw.GlfwPlatform
import com.github.minigdx.tiny.resources.GameScript
import java.io.File

class RecordCommand : CliktCommand(name = "record") {
    val gameDirectory by argument(help = "The directory containing your game to record.")
        .file(mustExist = true, canBeDir = true, canBeFile = false)
        .default(File("."))

    val duration by option("--duration", "-d", help = "Duration in seconds (default: 5)")
        .int()
        .default(5)

    val frames by option("--frames", "-f", help = "Number of frames to capture (overrides --duration)")
        .long()

    val output by option("--output", "-o", help = "Output file path (extension determines format: .png for screenshot, .gif for animation)")

    val headless by option("--headless", help = "Run without displaying the game window")
        .flag()

    val includeBoot by option("--include-boot", help = "Include the boot animation in the recording")
        .flag()

    override fun help(context: Context) = "Record your game as a GIF or PNG screenshot."

    override fun run() {
        val configFile = gameDirectory.resolve("_tiny.json")
        if (!configFile.exists()) {
            echo("No _tiny.json found in ${gameDirectory.absolutePath}! Can't record the game without it.")
            throw Abort()
        }

        val gameParameters = GameParameters.read(configFile)
        val maxFrames = frames ?: (duration * 60L)
        val outputFile = File(output ?: gameDirectory.resolve("recording.gif").path)
        val isScreenshot = outputFile.extension.lowercase() == "png"

        val logger = StdOutLogger("tiny-cli", level = LogLevel.INFO)
        val homeDirectory = findHomeDirectory(gameParameters)
        val vfs = CommonVirtualFileSystem()

        val baseOptions = gameParameters.toGameOptions()
        val recordSeconds = (maxFrames / 60f) + 1f
        val gameOption = baseOptions.copy(
            headless = headless,
            maxFrames = maxFrames,
            record = recordSeconds,
        )

        val platform = GlfwPlatform(
            gameOption,
            logger,
            vfs,
            gameDirectory,
            homeDirectory,
        )

        val gameEngine = GameEngine(
            gameOptions = gameOption,
            platform = platform,
            vfs = vfs,
            logger = logger,
            listener = object : GameEngineListener {
                override fun switchScript(
                    before: GameScript?,
                    after: GameScript?,
                ) {
                    if (!includeBoot) {
                        platform.clearRecordingCache()
                    }
                }

                override fun reload(gameScript: GameScript?) = Unit
            },
        )

        echo("Recording ${if (isScreenshot) "screenshot" else "GIF"} ($maxFrames frames)...")

        gameEngine.main()

        if (isScreenshot) {
            platform.screenshotSync(outputFile)
        } else {
            platform.recordSync(outputFile)
        }

        echo("Saved to ${outputFile.absolutePath}")

        // Force exit as the sound manager may keep background threads alive.
        @Suppress("ExitProcess")
        kotlin.system.exitProcess(0)
    }
}
