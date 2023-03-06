package com.github.minigdx.tiny.graphic

import com.github.minigdx.tiny.ColorIndex
import com.github.minigdx.tiny.HexValue
import com.github.minigdx.tiny.Pixel

class FrameBuffer(val width: Pixel, val height: Pixel) {

    private val defaultPalette: Array<ByteArray> = arrayOf(
        color(0, 0, 0), // black
        color(255, 0, 77), // red
        color(255, 236, 39), // yellow
    )

    private val colorIndexBuffer: Array<Array<ColorIndex>> = Array(height) { line ->
        Array(width) { 0 }
    }

    private val buffer: ByteArray = ByteArray(height * width * RGBA)

    private fun color(r: HexValue, g: HexValue, b: HexValue): ByteArray {
        return byteArrayOf(r.toByte(), g.toByte(), b.toByte(), 1.toByte())
    }

    fun pixel(x: Pixel, y: Pixel): ColorIndex {
        return colorIndexBuffer[x, y]
    }

    fun pixel(x: Pixel, y: Pixel, colorIndex: ColorIndex) {
        val index = colorIndex % defaultPalette.size
        colorIndexBuffer[x, y] = index
    }

    private operator fun Array<Array<ColorIndex>>.get(x: Pixel, y: Pixel): ColorIndex {
        return if (validCoordinates(x, y)) {
            colorIndexBuffer[y][x]
        } else {
            DEFAULT_INDEX
        }
    }


    private operator fun Array<Array<ColorIndex>>.set(x: Pixel, y: Pixel, value: ColorIndex) {
        if (validCoordinates(x, y)) {
            colorIndexBuffer[y][x] = value
        }
    }

    /**
     * Create a buffer using the colorIndexBuffer as reference.
     */
    fun generateBuffer(): ByteArray {
        for(x in 0 until width) {
            for(y in 0 until height) {
                val index = colorIndexBuffer[y][x]
                val color = defaultPalette[index]

                val offset = x * RGBA + y * width
                buffer[offset + 0] = color[0]
                buffer[offset + 1] = color[1]
                buffer[offset + 2] = color[2]
                buffer[offset + 3] = color[3]
            }
        }
        return buffer
    }

    private fun validCoordinates(x: Pixel, y: Pixel) =
        x in 0 until width && y in 0 until height

    companion object {
        // Color space. 1 byte per component. So 4 bytes per pixel.
        private const val RGBA = 4

        private const val DEFAULT_INDEX = 0
    }
}
