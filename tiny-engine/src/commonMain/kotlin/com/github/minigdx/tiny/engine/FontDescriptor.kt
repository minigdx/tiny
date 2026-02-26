package com.github.minigdx.tiny.engine

import kotlin.math.max

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
