package com.github.minigdx.tiny.graphic

import com.github.minigdx.tiny.ColorIndex
import com.github.minigdx.tiny.HexColor
import com.github.minigdx.tiny.util.Assert.assert
import kotlin.math.abs

/**
 * Color palette used by the game.
 *
 * Every color from every resource will be aligned to use this color palette.
 * It means that if a color from a resource is not part of this color palette,
 * this color will be replaced will the closed color from the palette.
 *
 */
class ColorPalette(colors: List<HexColor>) {
    private val rgba: Array<ByteArray>
    private val rgb: Array<ByteArray>
    private val rgbForGif: Array<Int>

    private val hexToColorCache: MutableMap<String, Int> = mutableMapOf()

    private val indexOfRgba: List<Int>
    private val indexOfColor: List<ColorIndex>

    val size: Int

    init {
        val rgbaColors = listOf(TRANSPARENT) + colors.map { str -> hexStringToByteArray(str) }

        rgba = Array(rgbaColors.size) { index -> rgbaColors[index] }
        rgb = Array(rgbaColors.size) { index ->
            val bytes = rgbaColors[index]
            byteArrayOf(bytes[0], bytes[1], bytes[2])
        }

        rgbForGif = rgb.map { color ->
            val r = color[0].toInt()
            val g = color[1].toInt()
            val b = color[2].toInt()
            val rgb = ((r and 0xFF) shl 16) or ((g and 0xFF) shl 8) or (b and 0xFF)
            rgb
        }.toTypedArray()

        val rgbaToColor = rgbaColors.mapIndexed { index, color -> rgbaToInt(color) to index }.sortedBy { it.first }
        indexOfRgba = List(rgbaToColor.size) { index -> rgbaToColor[index].first }
        indexOfColor = List(rgbaToColor.size) { index -> rgbaToColor[index].second }
        size = rgba.size
    }

    fun check(color: ColorIndex): ColorIndex {
        return abs(color) % size
    }

    private fun rgbaToInt(rgba: ByteArray): Int {
        val r = rgba[0].toInt() and 0xFF
        val g = rgba[1].toInt() and 0xFF
        val b = rgba[2].toInt() and 0xFF
        val a = rgba[3].toInt() and 0xFF

        return (r shl 24) or (g shl 16) or (b shl 8) or a
    }

    private fun hexStringToByteArray(hexString: String): ByteArray {
        val color = hexString.replace("#", "") // remove the # character
        val red = color.substring(0, 2).toInt(16)
        val green = color.substring(2, 4).toInt(16)
        val blue = color.substring(4, 6).toInt(16)
        val alpha = 0xFF // set alpha to 255 (fully opaque)

        return byteArrayOf(red.toByte(), green.toByte(), blue.toByte(), alpha.toByte())
    }

    private fun dst(
        r1: Byte,
        g1: Byte,
        b1: Byte,
        a1: Byte,
        r2: Byte,
        g2: Byte,
        b2: Byte,
        a2: Byte,
    ): Int {
        val r = (r1.toUByte() - r2.toUByte()) * (r1.toUByte() - r2.toUByte())
        val g = (g1.toUByte() - g2.toUByte()) * (g1.toUByte() - g2.toUByte())
        val b = (b1.toUByte() - b2.toUByte()) * (b1.toUByte() - b2.toUByte())
        val a = (a1.toUByte() - a2.toUByte()) * (a1.toUByte() - a2.toUByte())
        return (r + g + b + a).toInt()
    }

    /**
     * Get the RGBA value attached to this color index.
     */
    fun getRGBA(index: ColorIndex): ByteArray {
        return rgba[check(index)]
    }

    /**
     * Get the RGB value attached to this color index.
     */
    fun getRGB(index: ColorIndex): ByteArray {
        return rgb[check(index)]
    }

    /**
     * Get the RGB value already packed for GIF attached to this color index.
     */
    fun getRGAasInt(index: ColorIndex): Int {
        return rgbForGif[check(index)]
    }

    /**
     * Return the color index of the closest color matching the hexadecimal color.
     */
    fun getColorIndex(hexString: String): ColorIndex {
        return hexToColorCache.getOrPut(hexString) { fromRGBA(hexStringToByteArray(hexString)) }
    }

    /**
     * Return the color index of the color. Throw exception if the color
     * is not part of the color palette.
     */
    fun getColorIndex(color: ByteArray): ColorIndex {
        fun rgbaBytesToString(colorBytes: ByteArray): String {
            val r = colorBytes[0].toInt() and 0xFF
            val g = colorBytes[1].toInt() and 0xFF
            val b = colorBytes[2].toInt() and 0xFF
            val a = colorBytes[3].toInt() and 0xFF

            return "R: $r, G: $g, B: $b, A: $a"
        }

        val a = color[3].toInt() and 0xFF
        // The color is transparent. We do know already the result.
        if (a == 0) {
            return TRANSPARENT_INDEX
        }

        val index = indexOfRgba.binarySearch(rgbaToInt(color))
        if (index < 0) {
            throw IllegalArgumentException(
                "Color ${rgbaBytesToString(color)} is not part of the color palette",
            )
        }
        return indexOfColor[index]
    }

    /**
     * Get the color index of the closet color from palette.
     */
    fun fromRGBA(color: ByteArray): ColorIndex {
        assert(color.size == 4) { "The color is not a RGBA color as it has ${color.size} components" }
        // Transparent color
        if (color[3] == 0x00.toByte()) {
            return 0
        }
        // Remove the transparent color
        var current = 999999999
        var index = 0
        // Look for the index with the closest color.
        rgba.forEachIndexed { i, palette ->
            val d = dst(
                palette[0],
                palette[1],
                palette[2],
                palette[3],
                color[0],
                color[1],
                color[2],
                color[3],
            )
            if (d < current) {
                index = i
                current = d
            }
        }
        return index
    }

    /**
     * Is this color index transparent?
     */
    fun isTransparent(index: ColorIndex): Boolean {
        return index == TRANSPARENT_INDEX
    }

    companion object {
        private val TRANSPARENT = byteArrayOf(0, 0, 0, 0)
        const val TRANSPARENT_INDEX = 0
    }
}
