package com.github.minigdx.tiny.graphic

import com.github.minigdx.tiny.ColorIndex
import com.github.minigdx.tiny.HexColor

/**
 * Color palette used by the game.
 *
 * Every colors from every resources will be aligned to use this color palette.
 * It means that if a color from a resource is not part of this color palette,
 * this color will be replaced will the closed color from the palette.
 *
 */
class ColorPalette(colors: List<HexColor>) {

    private val rgba: Map<Int, ByteArray>
    private val rgb: Map<Int, ByteArray>
    private val rgbForGif: Map<Int, Int>
    val size: Int

    init {
        val rgbaColors = listOf(TRANSPARENT) + colors.map { str -> hexStringToByteArray(str) }

        rgba = rgbaColors.mapIndexed { index, bytes -> index to bytes }.toMap()
        rgb = rgbaColors.mapIndexed { index, bytes ->
            val color = byteArrayOf(bytes[0], bytes[1], bytes[2])
            index to color
        }.toMap()

        rgbForGif = rgb.mapValues { (_, color) ->
            val r = color[0].toInt()
            val g = color[1].toInt()
            val b = color[2].toInt()
            val rgb = ((r and 0xFF) shl 16) or ((g and 0xFF) shl 8) or (b and 0xFF)
            rgb
        }

        size = rgba.size
    }

    private fun hexStringToByteArray(hexString: String): ByteArray {
        val color = hexString.replace("#", "") // remove the # character
        val red = color.substring(0, 2).toInt(16)
        val green = color.substring(2, 4).toInt(16)
        val blue = color.substring(4, 6).toInt(16)
        val alpha = 0xFF // set alpha to 255 (fully opaque)

        return byteArrayOf(red.toByte(), green.toByte(), blue.toByte(), alpha.toByte())
    }

    private fun dst(r1: Byte, g1: Byte, b1: Byte, r2: Byte, g2: Byte, b2: Byte): Int {
        val r = (r1 - r2) * (r1 - r2)
        val g = (g1 - g2) * (g1 - g2)
        val b = (b1 - b2) * (b1 - b2)
        return r + g + b
    }


    /**
     * Get the RGBA value attached to this color index.
     */
    fun getRGBA(index: ColorIndex): ByteArray {
        return rgba.getOrDefault(index, rgba.getValue(0))
    }

    /**
     * Get the RGB value attached to this color index.
     */
    fun getRGB(index: ColorIndex): ByteArray {
        return rgb.getOrDefault(index, rgb.getValue(0))
    }

    /**
     * Get the RGB value already packed for GIF attached to this color index.
     */
    fun getGifColor(index: ColorIndex): Int {
        return rgbForGif.getOrDefault(index, rgbForGif.getValue(0))
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
        return rgba.minBy { (_, palette) ->
            dst(
                palette[0], palette[1], palette[2],
                color[0], color[1], color[2],
            )
        }.key
    }

    /**
     * Is this color index transparent?
     */
    fun isTransparent(index: ColorIndex): Boolean {
        return index == 0
    }

    companion object {
        private val TRANSPARENT = byteArrayOf(0, 0, 0, 0)
    }
}
