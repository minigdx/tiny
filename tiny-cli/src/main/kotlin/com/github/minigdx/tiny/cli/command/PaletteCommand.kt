package com.github.minigdx.tiny.cli.command

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.Context
import com.github.ajalt.clikt.parameters.arguments.argument
import com.github.ajalt.clikt.parameters.arguments.optional
import com.github.ajalt.clikt.parameters.options.default
import com.github.ajalt.clikt.parameters.options.flag
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.types.file
import com.github.minigdx.tiny.cli.command.utils.ColorUtils
import com.github.minigdx.tiny.cli.command.utils.ColorUtils.brightness
import com.github.minigdx.tiny.cli.command.utils.PaletteImageGenerator
import com.github.minigdx.tiny.cli.config.GameParameters
import com.github.minigdx.tiny.cli.exception.MissingTinyConfigurationException
import com.github.minigdx.tiny.file.CommonVirtualFileSystem
import com.github.minigdx.tiny.graphic.PixelFormat
import com.github.minigdx.tiny.log.StdOutLogger
import com.github.minigdx.tiny.platform.glfw.GlfwPlatform
import kotlinx.coroutines.runBlocking
import java.io.File

class PaletteCommand : CliktCommand(name = "palette") {
    val gameDirectory by option("-d", "--directory", help = "The directory containing your game to be update.")
        .file(mustExist = true, canBeDir = true, canBeFile = false)
        .default(File("."))

    val image by argument(help = "The image used to extract the palette.")
        .file(mustExist = true, canBeFile = true, canBeDir = false)
        .optional()

    val append by option(help = "Append, instead of replacing, the palette information in the game file.")
        .flag()

    val print by option(help = "Print in the console the palette information, without updating the game.")
        .flag()

    val palette by option(help = "Generate a palette.png image file with all colors from the palette.")
        .flag()

    override fun help(context: Context) = "Extract the color palette from an image or display current colors."

    override fun run() {
        val tiny = gameDirectory.resolve("_tiny.json")
        if (!tiny.exists()) {
            throw MissingTinyConfigurationException(tiny)
        }

        // Open the _tiny.json
        val gameParameters = GameParameters.read(tiny)
        val gameOptions = gameParameters.toGameOptions()

        if (image == null) {
            // No image provided - display current colors
            displayCurrentColors(gameOptions.palette)

            // Generate palette image if requested
            if (palette) {
                val paletteFile = PaletteImageGenerator.generatePaletteImage(gameDirectory, gameOptions.palette)
                echo("🎨 Palette image generated: ${paletteFile.name}")
            }

            return
        }

        val homeDirectory = findHomeDirectory(gameParameters)

        // Image provided - extract colors and process
        val platform = GlfwPlatform(
            gameOptions = gameOptions,
            logger = StdOutLogger("whatever"),
            vfs = CommonVirtualFileSystem(),
            gameDirectory = gameDirectory,
            homeDirectory = homeDirectory,
        )
        val imageData = runBlocking {
            platform.createImageStream(image!!.relativeTo(gameDirectory).path).read()
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
            echo("\uD83C\uDFA8 ${extractedColors.size} colors extracted from the file ${image!!.name}:")
            extractedColors.forEachIndexed { index, color ->
                echo("- ${index + 1} \t-> \t$color")
            }
            // Display current colors at the end
            echo()
            displayCurrentColors(gameOptions.palette)
            return
        }

        val replacedColors = if (append) {
            gameOptions.palette + extractedColors
        } else {
            extractedColors
        }

        val sortedColors = replacedColors.sortedBy { brightness(it) }
        gameParameters.setPalette(sortedColors).write(tiny)
        echo("\uD83C\uDFA8 Game has been updated with the new color palette (with ${replacedColors.size} colors)")

        // Display current colors at the end
        echo()
        displayCurrentColors(sortedColors)

        // Generate palette image if requested
        if (palette) {
            val paletteFile = PaletteImageGenerator.generatePaletteImage(gameDirectory, sortedColors)
            echo("🎨 Palette image generated: ${paletteFile.name}")
        }
    }

    private fun displayCurrentColors(colors: List<String>) {
        echo("\uD83C\uDFA8 Current colors:")
        echo(ColorUtils.formatCurrentPaletteDisplay(colors, withIndex = true))
    }
}
