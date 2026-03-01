package com.github.minigdx.tiny.cli.command.utils

import com.github.minigdx.tiny.engine.GameConfigFont
import com.github.minigdx.tiny.engine.GameConfigFontBank
import java.io.File

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
     * Derive a font name from a file path.
     * "fonts/big.png" → "big"
     */
    fun deriveFontName(filePath: String): String {
        return File(filePath).nameWithoutExtension
    }

    /**
     * Read PNG image dimensions using ImageIO.
     */
    fun readImageDimensions(
        gameDirectory: File,
        filePath: String,
    ): Pair<Int, Int> {
        val file = gameDirectory.resolve(filePath)
        val image = javax.imageio.ImageIO.read(file)
            ?: throw IllegalArgumentException("Cannot read image: $filePath")
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
     * Build a GameConfigFont from the given parameters.
     */
    fun buildFontConfig(
        fontName: String,
        spritesheet: String,
        charWidth: Int,
        charHeight: Int,
        characters: List<String>,
        bankName: String = "default",
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
                ),
            ),
        )
    }
}
