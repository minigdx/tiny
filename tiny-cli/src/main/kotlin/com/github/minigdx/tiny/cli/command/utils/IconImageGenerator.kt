package com.github.minigdx.tiny.cli.command.utils

import java.awt.Color
import java.awt.Font
import java.awt.RenderingHints
import java.awt.image.BufferedImage
import java.io.File
import javax.imageio.ImageIO
import kotlin.math.abs

object IconImageGenerator {
    private const val ICON_SIZE = 256
    private const val GRID_SIZE = 8
    private const val CELL_SIZE = ICON_SIZE / GRID_SIZE

    /**
     * Generate a game icon as a 256x256 PNG using the game's color palette.
     *
     * The icon is a mosaic of palette colors arranged in a radial brightness pattern
     * (darker colors on outer edges, lighter colors toward center) with the game name's
     * first letter rendered in the center.
     */
    fun generateIcon(
        gameDirectory: File,
        colors: List<String>,
        gameName: String,
    ): File {
        val image = BufferedImage(ICON_SIZE, ICON_SIZE, BufferedImage.TYPE_INT_ARGB)
        val graphics = image.createGraphics()
        graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)

        val sortedColors = colors.sortedBy { ColorUtils.brightness(it) }

        fillMosaic(graphics, sortedColors)
        drawCenterLetter(graphics, gameName, sortedColors)

        graphics.dispose()

        val iconFile = gameDirectory.resolve("icon.png")
        ImageIO.write(image, "PNG", iconFile)
        return iconFile
    }

    private fun fillMosaic(
        graphics: java.awt.Graphics2D,
        sortedColors: List<String>,
    ) {
        if (sortedColors.isEmpty()) return

        val center = (GRID_SIZE - 1) / 2.0
        for (row in 0 until GRID_SIZE) {
            for (col in 0 until GRID_SIZE) {
                val distFromCenter = maxOf(
                    abs(row - center),
                    abs(col - center),
                )
                val normalizedDist = distFromCenter / center
                val colorIndex = ((1.0 - normalizedDist) * (sortedColors.size - 1))
                    .toInt()
                    .coerceIn(0, sortedColors.lastIndex)

                graphics.color = ColorUtils.parseColor(sortedColors[colorIndex])
                graphics.fillRect(col * CELL_SIZE, row * CELL_SIZE, CELL_SIZE, CELL_SIZE)
            }
        }
    }

    private fun drawCenterLetter(
        graphics: java.awt.Graphics2D,
        gameName: String,
        sortedColors: List<String>,
    ) {
        if (sortedColors.isEmpty()) return

        val letter = gameName.firstOrNull()?.uppercase() ?: "T"

        // Use the darkest color for the letter on the lightest center background
        graphics.color = ColorUtils.parseColor(sortedColors.first())
        graphics.font = Font(Font.MONOSPACED, Font.BOLD, 96)

        val metrics = graphics.fontMetrics
        val x = (ICON_SIZE - metrics.stringWidth(letter)) / 2
        val y = (ICON_SIZE - metrics.height) / 2 + metrics.ascent
        graphics.drawString(letter, x, y)
    }
}
