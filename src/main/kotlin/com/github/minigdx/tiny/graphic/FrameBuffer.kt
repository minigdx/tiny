package com.github.minigdx.tiny.graphic

import com.github.minigdx.tiny.ColorIndex
import com.github.minigdx.tiny.HexValue
import com.github.minigdx.tiny.Pixel
import com.github.minigdx.tiny.util.PixelFormat
import kotlin.math.abs

class FrameBuffer(val width: Pixel, val height: Pixel) {

    internal val colorIndexBuffer: PixelArray = PixelArray(width, height, PixelFormat.INDEX)

    internal var buffer: ByteArray = ByteArray(height * width * RGBA)

    internal var gifBuffer: IntArray = IntArray(0)

    fun pixel(x: Pixel, y: Pixel): ColorIndex {
        return colorIndexBuffer.get(x, y)[0]
    }

    fun pixel(x: Pixel, y: Pixel, colorIndex: ColorIndex) {
        val index = colorIndex % defaultPalette.size
        colorIndexBuffer.set(x, y, index)
    }

    private operator fun Array<Array<ColorIndex>>.set(x: Pixel, y: Pixel, value: ColorIndex) {
        if (validCoordinates(x, y)) {
            colorIndexBuffer.set(x, y, value)
        }
    }

    fun clear(clearIndx: Int) {
        val clearIndex = abs(clearIndx) % defaultPalette.size
        for (x in 0 until width) {
            for (y in 0 until height) {
                colorIndexBuffer.set(x, y, clearIndex)
            }
        }
    }

    /**
     * Create a buffer using the colorIndexBuffer as reference.
     */
    fun generateBuffer(): ByteArray {
        // Reset the old buffer
        gifBuffer = IntArray(height * width)


        var pos = 0
        var posGif = 0
        for (x in 0 until width) {
            for (y in 0 until height) {
                val index = colorIndexBuffer.get(x, y)[0]
                val color = defaultPalette[index]

                buffer[pos++] = color[0]
                buffer[pos++] = color[1]
                buffer[pos++] = color[2]
                buffer[pos++] = color[3]

                val r = color[0].toInt()
                val g = color[1].toInt()
                val b = color[2].toInt()
                val rgb = ((r and 0xFF) shl 16) or ((g and 0xFF) shl 8) or (b and 0xFF)
                gifBuffer[posGif++] = rgb
            }
        }
        return buffer
    }

    private fun validCoordinates(x: Pixel, y: Pixel) =
        x in 0 until width && y in 0 until height

    companion object {
        // Color space. 1 byte per component. So 4 bytes per pixel.
        private const val RGBA = 4

        private const val DEFAULT_INDEX = 1

        val defaultPalette: Array<ByteArray> = arrayOf(
            color(0x00, 0x00, 0x00), // fake color so the palette start at 1
            color(0x00, 0x00, 0x00), // black
            color(0x1D, 0x2B, 0x53), // light black
            color(0x7E, 0x25, 0x53), // light black
            color(0x00, 0x87, 0x51), // green
            color(0xAB, 0x52, 0x36),
            color(0x5F, 0x57, 0x4F),
            color(0xC2, 0xC3, 0xC7),
            color(0xFF, 0xF1, 0xE8),
            color(0xFF, 0x00, 0x4D), // red
            color(0xFF, 0xA3, 0x00),
            color(0xFF, 0xEC, 0x27), // yellow
            color(0x00, 0xE4, 0x36),
            color(0x29, 0xAD, 0xFF),
            color(0x83, 0x76, 0x9C),
            color(0xFF, 0x77, 0xA8),
            color(0xFF, 0xCC, 0xAA),
        )

        val rgbPalette = defaultPalette.map { color ->
            val r = color[0].toInt()
            val g = color[1].toInt()
            val b = color[2].toInt()
            val rgb = ((r and 0xFF) shl 16) or ((g and 0xFF) shl 8) or (b and 0xFF)
            rgb
        }.toIntArray()

        private fun color(r: HexValue, g: HexValue, b: HexValue): ByteArray {
            return byteArrayOf(r.toByte(), g.toByte(), b.toByte(), 1.toByte())
        }
    }
}
