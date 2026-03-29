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
         * Layout: 256×256 px, 4×4 cells.
         * Row 0: a-z, Row 1: 0-9, Row 2: !-/, Row 3: [-`, Row 4: {-~, Row 5: :-@
         */
        fun createBootDescriptor(): FontDescriptor {
            val charMap = mutableMapOf<Int, Pair<Int, Int>>()

            // Row 0: a-z (uppercase maps to same)
            for (c in 'a'..'z') {
                charMap[c.code] = (c - 'a') to 0
                charMap[c.uppercaseChar().code] = (c - 'a') to 0
            }
            // Row 1: 0-9
            for (c in '0'..'9') {
                charMap[c.code] = (c - '0') to 1
            }
            // Row 2: ! to / (ASCII 33-47)
            for (c in '!'..'/') {
                charMap[c.code] = (c - '!') to 2
            }
            // Row 3: [ to ` (ASCII 91-96)
            for (c in '['..'`') {
                charMap[c.code] = (c - '[') to 3
            }
            // Row 4: { to ~ (ASCII 123-126)
            for (c in '{'..'~') {
                charMap[c.code] = (c - '{') to 4
            }
            // Row 5: : to @ (ASCII 58-64)
            for (c in ':'..'@') {
                charMap[c.code] = (c - ':') to 5
            }

            val bank = FontBank(
                name = "ascii",
                charWidth = 4,
                charHeight = 4,
                x = 0,
                y = 0,
                charMap = charMap,
            )

            return FontDescriptor(
                name = "boot",
                spritesheet = "_boot",
                spaceWidth = 4,
                lineHeight = 6,
                banks = listOf(bank),
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
