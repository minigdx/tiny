package com.github.minigdx.tiny.cli.command.utils

import com.github.minigdx.tiny.engine.GameConfigFont
import com.github.minigdx.tiny.engine.GameConfigFontBank
import java.awt.image.BufferedImage
import java.io.File

data class BoundingBox(val minX: Int, val minY: Int, val maxX: Int, val maxY: Int) {
    val width get() = maxX - minX + 1
    val height get() = maxY - minY + 1
}

data class FontDetectionResult(
    val offsetX: Int,
    val offsetY: Int,
    val cellWidth: Int,
    val cellHeight: Int,
    val offsetDetected: Boolean,
    val sizeDetected: Boolean,
)

object FontAnalyzer {
    /**
     * Parse a size string like "8x12" or "8" into a width/height pair.
     */
    fun parseSize(size: String): Pair<Int, Int> {
        val parts = size.split("x", "X")
        return if (parts.size == 2) {
            parts[0].trim().toInt() to parts[1].trim().toInt()
        } else {
            val s = parts[0].trim().toInt()
            s to s
        }
    }

    /**
     * Parse an offset string like "8,12" or "8x12" into an x/y pair.
     */
    fun parseOffset(offset: String): Pair<Int, Int> {
        val parts = offset.split(",", "x", "X")
        return if (parts.size == 2) {
            parts[0].trim().toInt() to parts[1].trim().toInt()
        } else {
            val s = parts[0].trim().toInt()
            s to s
        }
    }

    /**
     * Derive a font name from a file path.
     * "fonts/big.png" → "big"
     */
    fun deriveFontName(filePath: String): String {
        return File(filePath).nameWithoutExtension
    }

    /**
     * Read a PNG image using ImageIO.
     */
    fun readImage(
        gameDirectory: File,
        filePath: String,
    ): BufferedImage {
        val file = gameDirectory.resolve(filePath)
        return javax.imageio.ImageIO.read(file)
            ?: throw IllegalArgumentException("Cannot read image: $filePath")
    }

    /**
     * Read PNG image dimensions using ImageIO.
     */
    fun readImageDimensions(
        gameDirectory: File,
        filePath: String,
    ): Pair<Int, Int> {
        val image = readImage(gameDirectory, filePath)
        return image.width to image.height
    }

    /**
     * Split a chars string into rows based on image width and character width.
     * Given image width 64px, char width 8px → 8 chars/row.
     * "abcdefghijklmnop" becomes ["abcdefgh", "ijklmnop"].
     */
    fun splitCharsIntoRows(
        imageWidth: Int,
        charWidth: Int,
        chars: String,
    ): List<String> {
        val charsPerRow = imageWidth / charWidth
        return chars.chunked(charsPerRow)
    }

    /**
     * Detect the bounding box of non-transparent pixels in an image.
     * Returns null if the image is fully transparent.
     */
    fun detectBoundingBox(image: BufferedImage): BoundingBox? {
        var minX = image.width
        var minY = image.height
        var maxX = -1
        var maxY = -1

        for (y in 0 until image.height) {
            for (x in 0 until image.width) {
                val alpha = (image.getRGB(x, y) ushr 24) and 0xFF
                if (alpha > 0) {
                    if (x < minX) minX = x
                    if (x > maxX) maxX = x
                    if (y < minY) minY = y
                    if (y > maxY) maxY = y
                }
            }
        }

        return if (maxX < 0) null else BoundingBox(minX, minY, maxX, maxY)
    }

    /**
     * Detect cell size by analyzing gaps (fully transparent columns/rows) within the bounding box.
     * Returns the cell width and height, or null if detection fails.
     */
    fun detectCellSize(
        image: BufferedImage,
        box: BoundingBox,
        totalChars: Int,
    ): Pair<Int, Int>? {
        val cellWidth = detectCellDimension(image, box, isHorizontal = true, totalChars)
        val cellHeight = detectCellDimension(image, box, isHorizontal = false, totalChars)

        if (cellWidth != null && cellHeight != null) {
            return cellWidth to cellHeight
        }

        // Fallback: try divisor-based detection
        return detectCellSizeByDivisors(box, totalChars)
    }

    private fun detectCellDimension(
        image: BufferedImage,
        box: BoundingBox,
        isHorizontal: Boolean,
        totalChars: Int,
    ): Int? {
        val size = if (isHorizontal) box.width else box.height

        // For each line (column if horizontal, row if vertical), check if it's fully transparent
        val isTransparent = BooleanArray(size) { i ->
            val pos = i + if (isHorizontal) box.minX else box.minY
            isLineTransparent(image, box, pos, isHorizontal)
        }

        // Find content bands (consecutive non-transparent lines)
        val bandWidths = mutableListOf<Int>()
        var i = 0
        while (i < size) {
            if (!isTransparent[i]) {
                val start = i
                while (i < size && !isTransparent[i]) i++
                bandWidths.add(i - start)
            } else {
                i++
            }
        }

        if (bandWidths.isEmpty()) return null

        // Check if the first band width repeats consistently (allowing the last band to be smaller)
        val candidateWidth = bandWidths.first()
        val consistent = bandWidths.dropLast(1).all { it == candidateWidth } &&
            bandWidths.last() <= candidateWidth

        if (!consistent || candidateWidth <= 0) return null

        // The cell size includes the gap: find the gap width after the first band
        val firstBandEnd = run {
            var pos = 0
            // skip to first content
            while (pos < size && isTransparent[pos]) pos++
            // skip content
            while (pos < size && !isTransparent[pos]) pos++
            pos
        }
        var gapWidth = 0
        var gapPos = firstBandEnd
        while (gapPos < size && isTransparent[gapPos]) {
            gapWidth++
            gapPos++
        }

        // If there's only one band, cell size = content width (no gap info)
        return if (bandWidths.size == 1) {
            // Can't determine cell size from a single band, try divisors instead
            null
        } else {
            candidateWidth + gapWidth
        }
    }

    private fun isLineTransparent(
        image: BufferedImage,
        box: BoundingBox,
        pos: Int,
        isHorizontal: Boolean,
    ): Boolean {
        val start = if (isHorizontal) box.minY else box.minX
        val end = if (isHorizontal) box.maxY else box.maxX

        for (i in start..end) {
            val x = if (isHorizontal) pos else i
            val y = if (isHorizontal) i else pos
            val alpha = (image.getRGB(x, y) ushr 24) and 0xFF
            if (alpha > 0) return false
        }
        return true
    }

    private fun detectCellSizeByDivisors(
        box: BoundingBox,
        totalChars: Int,
    ): Pair<Int, Int>? {
        val widthDivisors = (1..box.width).filter { box.width % it == 0 }
        val heightDivisors = (1..box.height).filter { box.height % it == 0 }

        var bestCellWidth = 0
        var bestCellHeight = 0

        for (cw in widthDivisors) {
            for (ch in heightDivisors) {
                val cols = box.width / cw
                val rows = box.height / ch
                if (cols * rows >= totalChars) {
                    val ratio = maxOf(cw, ch).toFloat() / maxOf(1, minOf(cw, ch)).toFloat()
                    if (ratio <= 3f && cw * ch > bestCellWidth * bestCellHeight) {
                        bestCellWidth = cw
                        bestCellHeight = ch
                    }
                }
            }
        }

        return if (bestCellWidth > 0 && bestCellHeight > 0) {
            bestCellWidth to bestCellHeight
        } else {
            null
        }
    }

    /**
     * Auto-detect font grid parameters from the image.
     * Uses explicit values when provided, auto-detects when not.
     * Returns null if size detection fails entirely.
     */
    fun autoDetect(
        image: BufferedImage,
        explicitSize: Pair<Int, Int>?,
        explicitOffset: Pair<Int, Int>?,
        totalChars: Int,
    ): FontDetectionResult? {
        if (explicitSize != null && explicitOffset != null) {
            return FontDetectionResult(
                offsetX = explicitOffset.first,
                offsetY = explicitOffset.second,
                cellWidth = explicitSize.first,
                cellHeight = explicitSize.second,
                offsetDetected = false,
                sizeDetected = false,
            )
        }

        val box = detectBoundingBox(image)

        val offsetX: Int
        val offsetY: Int
        val offsetDetected: Boolean

        if (explicitOffset != null) {
            offsetX = explicitOffset.first
            offsetY = explicitOffset.second
            offsetDetected = false
        } else if (box != null) {
            offsetX = box.minX
            offsetY = box.minY
            offsetDetected = true
        } else {
            offsetX = 0
            offsetY = 0
            offsetDetected = true
        }

        if (explicitSize != null) {
            return FontDetectionResult(
                offsetX = offsetX,
                offsetY = offsetY,
                cellWidth = explicitSize.first,
                cellHeight = explicitSize.second,
                offsetDetected = offsetDetected,
                sizeDetected = false,
            )
        }

        // Auto-detect size
        val effectiveBox = box ?: return null
        val cellSize = detectCellSize(image, effectiveBox, totalChars) ?: return null

        return FontDetectionResult(
            offsetX = offsetX,
            offsetY = offsetY,
            cellWidth = cellSize.first,
            cellHeight = cellSize.second,
            offsetDetected = offsetDetected,
            sizeDetected = true,
        )
    }

    /**
     * Build a GameConfigFont from the given parameters.
     */
    fun buildFontConfig(
        fontName: String,
        spritesheet: String,
        charWidth: Int,
        charHeight: Int,
        characters: List<String>,
        bankName: String = "default",
        offsetX: Int = 0,
        offsetY: Int = 0,
    ): GameConfigFont {
        return GameConfigFont(
            name = fontName,
            spritesheet = spritesheet,
            banks = listOf(
                GameConfigFontBank(
                    name = bankName,
                    width = charWidth,
                    height = charHeight,
                    characters = characters,
                    x = offsetX,
                    y = offsetY,
                ),
            ),
        )
    }
}
