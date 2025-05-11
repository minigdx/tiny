package com.github.minigdx.tiny.cli.command

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.Context
import com.github.ajalt.clikt.parameters.arguments.argument
import com.github.ajalt.clikt.parameters.options.default
import com.github.ajalt.clikt.parameters.options.flag
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.types.file
import com.github.minigdx.tiny.cli.config.GameParameters
import com.github.minigdx.tiny.cli.exception.MissingTinyConfigurationException
import com.github.minigdx.tiny.file.CommonVirtualFileSystem
import com.github.minigdx.tiny.graphic.PixelFormat
import com.github.minigdx.tiny.log.StdOutLogger
import com.github.minigdx.tiny.platform.glfw.GlfwPlatform
import kotlinx.coroutines.runBlocking
import java.io.File

class PaletteCommand : CliktCommand(name = "palette") {
    val game by option(
        help = "The directory containing all game information",
    )
        .file(mustExist = true, canBeDir = true, canBeFile = false)
        .default(File("."))

    val image by argument(
        help = "The image used to extract the palette.",
    ).file(mustExist = true, canBeFile = true, canBeDir = false)

    val append by option(help = "Append, instead of replacing, the palette information in the game file.")
        .flag()

    val print by option(help = "Print in the console the palette information, without updating the game.")
        .flag()

    override fun help(context: Context) = "Extract the color palette from an image."

    override fun run() {
        val tiny = game.resolve("_tiny.json")
        if (!tiny.exists()) {
            throw MissingTinyConfigurationException(tiny)
        }
        // Open the _tiny.json
        val gameParameters = GameParameters.read(tiny)
        val gameOptions = gameParameters.toGameOptions()
        val platform = GlfwPlatform(gameOptions, StdOutLogger("whatever"), CommonVirtualFileSystem(), game)
        val imageData =
            runBlocking {
                platform.createImageStream(image.relativeTo(game).path).read()
            }

        val colors = mutableSetOf<String>()
        if (append) {
            // Append only new colors
            colors.addAll(gameOptions.palette)
        }
        var extractedColors = emptyList<String>()

        for (index in 0 until (imageData.height * imageData.width * PixelFormat.RGBA) step 4) {
            val r = imageData.data[index].toInt() and 0xFF
            val g = imageData.data[index + 1].toInt() and 0xFF
            val b = imageData.data[index + 2].toInt() and 0xFF
            val a = imageData.data[index + 3].toInt() and 0xFF

            // Keep only non-transparent colors
            if (a > 0) {
                // Convert to hex string
                val hexString = String.format("#%02x%02x%02x", r, g, b).uppercase()
                if (colors.add(hexString)) {
                    extractedColors = extractedColors + hexString
                }
            }
        }

        if (print) {
            echo("\uD83C\uDFA8 ${extractedColors.size} colors extracted from the file ${image.name}:")
            extractedColors.forEachIndexed { index, color ->
                echo("- ${index + 1} \t-> \t$color")
            }
            return
        }

        val replacedColors =
            if (append) {
                gameOptions.palette + extractedColors
            } else {
                extractedColors
            }

        gameParameters.setPalette(replacedColors).write(tiny)
        echo("\uD83C\uDFA8 Game has been updated with the new color palette (with ${replacedColors.size} colors)")
    }
}
