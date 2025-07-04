package com.github.minigdx.tiny.cli.command

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.Context
import com.github.ajalt.clikt.parameters.arguments.argument
import com.github.ajalt.clikt.parameters.arguments.default
import com.github.ajalt.clikt.parameters.arguments.optional
import com.github.ajalt.clikt.parameters.options.flag
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.types.file
import com.github.ajalt.mordant.rendering.TextColors
import com.github.minigdx.tiny.cli.command.utils.ColorUtils.brightness
import com.github.minigdx.tiny.cli.config.GameParameters
import com.github.minigdx.tiny.cli.exception.MissingTinyConfigurationException
import com.github.minigdx.tiny.file.CommonVirtualFileSystem
import com.github.minigdx.tiny.graphic.PixelFormat
import com.github.minigdx.tiny.log.StdOutLogger
import com.github.minigdx.tiny.platform.glfw.GlfwPlatform
import kotlinx.coroutines.runBlocking
import java.io.File

class PaletteCommand : CliktCommand(name = "palette") {
    val gameDirectory by argument(help = "The directory containing your game to be update.")
        .file(mustExist = true, canBeDir = true, canBeFile = false)
        .default(File("."))

    val image by argument(help = "The image used to extract the palette.")
        .file(mustExist = true, canBeFile = true, canBeDir = false)
        .optional()

    val append by option(help = "Append, instead of replacing, the palette information in the game file.")
        .flag()

    val print by option(help = "Print in the console the palette information, without updating the game.")
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
            return
        }

        // Image provided - extract colors and process
        val platform = GlfwPlatform(
            gameOptions = gameOptions,
            logger = StdOutLogger("whatever"),
            vfs = CommonVirtualFileSystem(),
            workdirectory = gameDirectory,
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
    }

    private fun displayCurrentColors(colors: List<String>) {
        echo("\uD83C\uDFA8 Current colors:")
        echo(formatCurrentPaletteDisplay(colors))
    }

    private fun formatCurrentPaletteDisplay(colors: List<String>): String {
        return colors.mapIndexed { index, hexColor ->
            // Remove the '#' prefix and parse RGB components
            val colorWithoutHash = hexColor.removePrefix("#")
            val r = colorWithoutHash.substring(0, 2).toInt(16)
            val g = colorWithoutHash.substring(2, 4).toInt(16)
            val b = colorWithoutHash.substring(4, 6).toInt(16)

            // Calculate brightness to determine text color for optimal contrast
            val brightness = (r * 0.299 + g * 0.587 + b * 0.114) / 255.0
            val textColor = if (brightness > 0.5) TextColors.black else TextColors.white
            val backgroundColor = TextColors.rgb(r / 255.0, g / 255.0, b / 255.0)

            // Create numbered square with optimal contrast
            val number = (index + 1).toString()
            backgroundColor(textColor(number.padStart(2)))
        }.joinToString("")
    }
}
