package com.github.minigdx.tiny.engine

import com.github.minigdx.tiny.render.VirtualFrameBuffer
import com.github.minigdx.tiny.resources.SpriteSheet
import kotlin.math.max

/**
 * Maps accented characters to their ASCII base letter for boot font fallback.
 */
val ACCENT_MAP =
    mapOf(
        'à' to 'a', 'á' to 'a', 'â' to 'a', 'ã' to 'a', 'ä' to 'a', 'å' to 'a',
        'ç' to 'c',
        'è' to 'e', 'é' to 'e', 'ê' to 'e', 'ë' to 'e',
        'ì' to 'i', 'í' to 'i', 'î' to 'i', 'ï' to 'i',
        'ñ' to 'n',
        'ò' to 'o', 'ó' to 'o', 'ô' to 'o', 'õ' to 'o', 'ö' to 'o',
        'ù' to 'u', 'ú' to 'u', 'û' to 'u', 'ü' to 'u',
        'ý' to 'y', 'ÿ' to 'y',
    )

/**
 * Maps emoji codepoints to (col, row) positions in the boot font emoji bank.
 */
val BOOT_EMOJI_MAP: Map<Int, Pair<Int, Int>> =
    mapOf(
        // Row 0: symbols and operators
        0x26A0 to (0 to 0), // ⚠ Warning
        0x2139 to (1 to 0), // ℹ Information
        0x2190 to (2 to 0), // → Right-pointing triangle
        0x2191 to (3 to 0), // ↑ Up-pointing triangle
        0x2192 to (4 to 0), // ← Left-pointing triangle
        0x2193 to (5 to 0), // ↓ Down-pointing triangle
        0x2295 to (6 to 0), // ⊕ Circled plus
        0x2297 to (7 to 0), // ⊗ Circled times
        0x00D7 to (8 to 0), // × Multiplication sign
        0x00F7 to (9 to 0), // ÷ Division sign
        0x00B1 to (10 to 0), // ± Plus-minus sign
        0x00AB to (11 to 0), // « Left guillemet
        0x25A0 to (12 to 0), // ■ Black square
        0x00BB to (13 to 0), // » Right guillemet
        0x00AC to (14 to 0), // ¬ Not sign
        0x00AF to (15 to 0), // ¯ Macron
        // Row 1: superscript digits, degree, shapes, currency
        0x2070 to (0 to 1), // ⁰ Superscript 0
        0x00B9 to (1 to 1), // ¹ Superscript 1
        0x00B2 to (2 to 1), // ² Superscript 2
        0x00B3 to (3 to 1), // ³ Superscript 3
        0x2074 to (4 to 1), // ⁴ Superscript 4
        0x2075 to (5 to 1), // ⁵ Superscript 5
        0x2076 to (6 to 1), // ⁶ Superscript 6
        0x2077 to (7 to 1), // ⁷ Superscript 7
        0x2078 to (8 to 1), // ⁸ Superscript 8
        0x2079 to (9 to 1), // ⁹ Superscript 9
        0x00B0 to (10 to 1), // ° Degree sign
        0x25A1 to (11 to 1), // □ White square
        0x25CB to (12 to 1), // ○ White circle
        0x20AC to (13 to 1), // € Euro sign
        0x00A5 to (14 to 1), // ¥ Yen sign
        0x2699 to (15 to 1), // ⚙ Gear
    )

data class CharResolution(
    val sourceX: Int,
    val sourceY: Int,
    val charWidth: Int,
    val charHeight: Int,
)

data class FontBank(
    val name: String,
    val charWidth: Int,
    val charHeight: Int,
    val x: Int,
    val y: Int,
    val charMap: Map<Int, Pair<Int, Int>>,
)

data class FontDescriptor(
    val name: String,
    val spritesheet: String,
    val spaceWidth: Int,
    val lineHeight: Int,
    val banks: List<FontBank>,
) {
    fun resolve(codepoint: Int): CharResolution? {
        for (bank in banks) {
            val coord = bank.charMap[codepoint]
            if (coord != null) {
                return CharResolution(
                    sourceX = bank.x + coord.first * bank.charWidth,
                    sourceY = bank.y + coord.second * bank.charHeight,
                    charWidth = bank.charWidth,
                    charHeight = bank.charHeight,
                )
            }
        }
        return null
    }

    companion object {
        fun fromConfig(config: GameConfigFont): FontDescriptor {
            var maxHeight = 0
            val banks = config.banks.map { bankConfig ->
                val bank = FontBank(
                    name = bankConfig.name,
                    charWidth = bankConfig.width,
                    charHeight = bankConfig.height,
                    x = bankConfig.x,
                    y = bankConfig.y,
                    charMap = buildCharMap(bankConfig.characters),
                )
                maxHeight = max(maxHeight, bankConfig.height)
                bank
            }
            val defaultSpaceWidth = config.banks.firstOrNull()?.let { it.width / 2 } ?: 4
            return FontDescriptor(
                name = config.name,
                spritesheet = config.spritesheet,
                spaceWidth = config.spaceWidth ?: defaultSpaceWidth,
                lineHeight = maxHeight,
                banks = banks,
            )
        }

        /**
         * Create a FontDescriptor for the boot font (_boot.png).
         * Layout: 96×96 px, 6×12 cells, 16 columns × 8 rows.
         * Rows 0–5: ASCII 32–127; Rows 6–7: emoji/icons.
         */
        fun createBootDescriptor(emojiMap: Map<Int, Pair<Int, Int>>): FontDescriptor {
            val asciiCharMap = mutableMapOf<Int, Pair<Int, Int>>()
            for (c in 32..126) {
                val offset = c - 32
                asciiCharMap[c] = (offset % 16) to (offset / 16)
            }

            val asciiBank = FontBank(
                name = "ascii",
                charWidth = 6,
                charHeight = 12,
                x = 0,
                y = 0,
                charMap = asciiCharMap,
            )

            val emojiBank = FontBank(
                name = "emoji",
                charWidth = 6,
                charHeight = 12,
                x = 0,
                y = 72,
                charMap = emojiMap,
            )

            return FontDescriptor(
                name = "boot",
                spritesheet = "_boot",
                spaceWidth = 6,
                lineHeight = 12,
                banks = listOf(asciiBank, emojiBank),
            )
        }
    }
}

/**
 * KMP-compatible codepoint iteration with surrogate pair handling.
 * Skips variation selectors (U+FE0E, U+FE0F).
 */
inline fun String.forEachCodepoint(action: (codepoint: Int) -> Unit) {
    var i = 0
    while (i < length) {
        val c = this[i]
        val codepoint = if (c.isHighSurrogate() && i + 1 < length && this[i + 1].isLowSurrogate()) {
            val low = this[i + 1]
            i += 2
            ((c.code - 0xD800) shl 10) + (low.code - 0xDC00) + 0x10000
        } else {
            i++
            c.code
        }
        if (codepoint != 0xFE0E && codepoint != 0xFE0F) {
            action(codepoint)
        }
    }
}

/**
 * Shared text rendering function using a FontDescriptor and boot spritesheet.
 * Handles newlines, spaces, regular characters, and accent fallback.
 */
fun renderText(
    descriptor: FontDescriptor,
    spritesheet: SpriteSheet,
    str: String,
    x: Int,
    y: Int,
    color: Int,
    virtualFrameBuffer: VirtualFrameBuffer,
) {
    var currentX = x
    var currentY = y

    str.forEachCodepoint { codepoint ->
        when (codepoint) {
            '\n'.code -> {
                currentY += descriptor.lineHeight
                currentX = x
            }
            ' '.code -> {
                currentX += descriptor.spaceWidth
            }
            else -> {
                val resolved = descriptor.resolve(codepoint)
                    ?: resolveAccentFallback(descriptor, codepoint)
                if (resolved != null) {
                    virtualFrameBuffer.drawMonocolor(
                        spritesheet,
                        color,
                        resolved.sourceX,
                        resolved.sourceY,
                        resolved.charWidth,
                        resolved.charHeight,
                        currentX,
                        currentY,
                        flipX = false,
                        flipY = false,
                    )
                    currentX += resolved.charWidth
                }
            }
        }
    }
}

private fun resolveAccentFallback(
    descriptor: FontDescriptor,
    codepoint: Int,
): CharResolution? {
    val char = codepoint.toChar()
    if (!char.isLetter()) return null
    val base = ACCENT_MAP[char.lowercaseChar()] ?: return null
    // Try lowercase base
    return descriptor.resolve(base.code)
        // Try uppercase base
        ?: descriptor.resolve(base.uppercaseChar().code)
}

fun buildCharMap(characters: List<String>): Map<Int, Pair<Int, Int>> {
    val map = mutableMapOf<Int, Pair<Int, Int>>()
    characters.forEachIndexed { row, line ->
        var col = 0
        line.forEachCodepoint { codepoint ->
            map[codepoint] = col to row
            col++
        }
    }
    return map
}
