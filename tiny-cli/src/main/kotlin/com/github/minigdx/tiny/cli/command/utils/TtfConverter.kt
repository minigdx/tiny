package com.github.minigdx.tiny.cli.command.utils

import java.awt.Color
import java.awt.Font
import java.awt.RenderingHints
import java.awt.image.BufferedImage
import java.io.File
import javax.imageio.ImageIO
import kotlin.math.ceil
import kotlin.math.sqrt

/**
 * Converts a TTF font file into a PNG spritesheet suitable for the Tiny engine.
 * Characters are rendered as white on transparent background in a grid layout.
 */
object TtfConverter {
    /** Default printable ASCII characters (excludes space, handled by spaceWidth). */
    val DEFAULT_CHARS = ('!'..'~').joinToString("")

    /**
     * Convert a TTF file to a PNG spritesheet.
     *
     * @param ttfFile The TTF font file
     * @param outputFile The PNG output file
     * @param chars Characters to include in the spritesheet
     * @param targetHeight Target cell height in pixels (determines font size). Null for auto (16px).
     * @return The cell width and height used
     */
    fun convert(
        ttfFile: File,
        outputFile: File,
        chars: String,
        targetHeight: Int?,
    ): ConversionResult {
        val baseFont = Font.createFont(Font.TRUETYPE_FONT, ttfFile)

        // Use target height to derive font size, or default to 16px
        val cellHeight = targetHeight ?: 16

        // Find the font size that fits within the target cell height
        val font = fitFontToHeight(baseFont, cellHeight)

        // Measure all characters to determine cell dimensions
        val metrics = measureCharacters(font, chars)
        val cellWidth = metrics.maxWidth

        // Calculate grid layout
        val cols = ceil(sqrt(chars.length.toDouble())).toInt().coerceAtLeast(1)
        val rows = ceil(chars.length.toDouble() / cols).toInt().coerceAtLeast(1)

        val imageWidth = cols * cellWidth
        val imageHeight = rows * cellHeight

        // Render the spritesheet
        val image = BufferedImage(imageWidth, imageHeight, BufferedImage.TYPE_INT_ARGB)
        val g2d = image.createGraphics()
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_OFF)
        g2d.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_OFF)
        g2d.font = font
        g2d.color = Color.WHITE

        val fm = g2d.fontMetrics
        val ascent = fm.ascent

        chars.forEachIndexed { index, char ->
            val col = index % cols
            val row = index / cols
            val x = col * cellWidth
            val y = row * cellHeight + ascent
            g2d.drawString(char.toString(), x, y)
        }

        g2d.dispose()

        // Post-process: threshold alpha to get crisp binary pixels.
        // macOS may ignore anti-aliasing hints, so this ensures clean output.
        thresholdAlpha(image)

        ImageIO.write(image, "PNG", outputFile)

        return ConversionResult(
            cellWidth = cellWidth,
            cellHeight = cellHeight,
            cols = cols,
            rows = rows,
            spaceWidth = metrics.spaceWidth,
            pngFile = outputFile,
        )
    }

    private fun fitFontToHeight(baseFont: Font, targetHeight: Int): Font {
        // Start from target height as point size and adjust
        var fontSize = targetHeight.toFloat()
        var font = baseFont.deriveFont(fontSize)

        val testImage = BufferedImage(1, 1, BufferedImage.TYPE_INT_ARGB)
        val g2d = testImage.createGraphics()

        // Binary search for the right font size
        var low = 1f
        var high = targetHeight * 2f
        var bestFont = font

        for (i in 0 until 20) {
            fontSize = (low + high) / 2f
            font = baseFont.deriveFont(fontSize)
            val fm = g2d.getFontMetrics(font)
            val h = fm.height

            if (h <= targetHeight) {
                bestFont = font
                low = fontSize
            } else {
                high = fontSize
            }
        }

        g2d.dispose()
        return bestFont
    }

    private fun measureCharacters(font: Font, chars: String): CharMetrics {
        val image = BufferedImage(1, 1, BufferedImage.TYPE_INT_ARGB)
        val g2d = image.createGraphics()
        g2d.font = font
        val fm = g2d.fontMetrics

        var maxWidth = 0
        chars.forEach { char ->
            val w = fm.charWidth(char)
            if (w > maxWidth) maxWidth = w
        }
        val spaceWidth = fm.charWidth(' ').coerceAtLeast(1)

        g2d.dispose()
        return CharMetrics(maxWidth = maxWidth.coerceAtLeast(1), spaceWidth = spaceWidth)
    }

    private fun thresholdAlpha(image: BufferedImage) {
        for (y in 0 until image.height) {
            for (x in 0 until image.width) {
                val argb = image.getRGB(x, y)
                val alpha = (argb ushr 24) and 0xFF
                if (alpha > 127) {
                    // Fully opaque white
                    image.setRGB(x, y, 0xFFFFFFFF.toInt())
                } else {
                    // Fully transparent
                    image.setRGB(x, y, 0x00000000)
                }
            }
        }
    }

    data class CharMetrics(val maxWidth: Int, val spaceWidth: Int)

    data class ConversionResult(
        val cellWidth: Int,
        val cellHeight: Int,
        val cols: Int,
        val rows: Int,
        val spaceWidth: Int,
        val pngFile: File,
    )
}
