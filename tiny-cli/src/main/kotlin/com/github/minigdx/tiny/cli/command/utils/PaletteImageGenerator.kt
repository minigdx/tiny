package com.github.minigdx.tiny.cli.command.utils

import java.awt.Color
import java.awt.image.BufferedImage
import java.io.File
import java.io.IOException
import javax.imageio.ImageIO
import kotlin.math.ceil
import kotlin.math.sqrt

object PaletteImageGenerator {
    private const val CELL_SIZE = 32
    private const val NUMBER_WIDTH = 4
    private const val NUMBER_HEIGHT = 4

    fun generatePaletteImage(
        gameDirectory: File,
        colors: List<String>,
    ): File {
        // Calculate grid dimensions (square grid)
        val colorsWithTransparent = listOf("#00000000") + colors // Add transparent color at index 0
        val gridSize = ceil(sqrt(colorsWithTransparent.size.toDouble())).toInt()
        val imageSize = gridSize * CELL_SIZE

        // Create buffered image
        val image = BufferedImage(imageSize, imageSize, BufferedImage.TYPE_INT_ARGB)
        val graphics = image.createGraphics()

        // Load number sprites from _boot.png
        val numberSprites = loadNumberSprites()

        // Fill each cell with its color and index
        colorsWithTransparent.forEachIndexed { index, colorString ->
            val row = index / gridSize
            val col = index % gridSize
            val x = col * CELL_SIZE
            val y = row * CELL_SIZE

            // Parse color
            val color = if (index == 0) {
                Color(0, 0, 0, 0) // Transparent
            } else {
                parseColor(colorString)
            }

            // Fill cell background
            graphics.color = color
            graphics.fillRect(x, y, CELL_SIZE, CELL_SIZE)

            // Draw border
            graphics.color = Color.BLACK
            graphics.drawRect(x, y, CELL_SIZE - 1, CELL_SIZE - 1)

            // Draw index number
            drawNumber(graphics, index, x + 2, y + 2, numberSprites)
        }

        graphics.dispose()

        // Find available filename
        val paletteFile = findAvailableFilename(gameDirectory, "palette", "png")

        // Save image
        try {
            ImageIO.write(image, "PNG", paletteFile)
        } catch (e: IOException) {
            throw RuntimeException("Failed to write palette image: ${e.message}", e)
        }

        return paletteFile
    }

    private fun loadNumberSprites(): BufferedImage? {
        return try {
            val inputStream = PaletteImageGenerator::class.java.getResourceAsStream("/_boot.png")
            if (inputStream != null) {
                ImageIO.read(inputStream)
            } else {
                null
            }
        } catch (e: Exception) {
            null
        }
    }

    private fun drawNumber(
        graphics: java.awt.Graphics2D,
        number: Int,
        x: Int,
        y: Int,
        numberSprites: BufferedImage?,
    ) {
        if (numberSprites == null) {
            // Fallback: draw text
            graphics.color = Color.WHITE
            graphics.drawString(number.toString(), x, y + 12)
            return
        }

        val numberString = number.toString()
        var currentX = x

        for (digit in numberString) {
            val digitValue = digit.toString().toInt()
            val spriteX = digitValue * NUMBER_WIDTH
            val spriteY = 4 // Numbers are at y=6 in _boot.png

            try {
                val digitSprite = numberSprites.getSubimage(spriteX, spriteY, NUMBER_WIDTH, NUMBER_HEIGHT)
                graphics.drawImage(digitSprite, currentX, y, null)
                currentX += NUMBER_WIDTH
            } catch (e: Exception) {
                // Fallback: draw text if sprite extraction fails
                graphics.color = Color.WHITE
                graphics.drawString(digit.toString(), currentX, y + 12)
                currentX += 8
            }
        }
    }

    private fun parseColor(colorString: String): Color {
        val hex = colorString.removePrefix("#")
        return when (hex.length) {
            6 -> {
                val r = hex.substring(0, 2).toInt(16)
                val g = hex.substring(2, 4).toInt(16)
                val b = hex.substring(4, 6).toInt(16)
                Color(r, g, b)
            }
            8 -> {
                val r = hex.substring(0, 2).toInt(16)
                val g = hex.substring(2, 4).toInt(16)
                val b = hex.substring(4, 6).toInt(16)
                val a = hex.substring(6, 8).toInt(16)
                Color(r, g, b, a)
            }
            else -> Color.BLACK
        }
    }

    private fun findAvailableFilename(
        directory: File,
        baseName: String,
        extension: String,
    ): File {
        var file = directory.resolve("$baseName.$extension")
        var counter = 1

        while (file.exists()) {
            file = directory.resolve("$baseName$counter.$extension")
            counter++
        }

        return file
    }
}
