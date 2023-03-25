package com.github.minigdx.tiny.graphic

import com.github.minigdx.tiny.ColorIndex
import com.github.minigdx.tiny.Pixel
import com.github.minigdx.tiny.util.PixelFormat
import kotlin.math.abs

class FrameBuffer(val width: Pixel, val height: Pixel) {

    internal val colorIndexBuffer: PixelArray = PixelArray(width, height, PixelFormat.INDEX)

    internal var buffer: ByteArray = ByteArray(height * width * PixelFormat.RGBA)

    internal var gifBuffer: IntArray = IntArray(0)

    fun pixel(x: Pixel, y: Pixel): ColorIndex {
        return colorIndexBuffer.getOne(x, y)
    }

    fun pixel(x: Pixel, y: Pixel, colorIndex: ColorIndex) {
        val index = abs(colorIndex) % gamePalette.size
        if (gamePalette.isTransparent(index)) return
        colorIndexBuffer.set(x, y, index)
    }

    fun clear(clearIndx: Int) {
        val clearIndex = abs(clearIndx) % gamePalette.size
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
                val index = colorIndexBuffer.getOne(x, y)
                val color = gamePalette.getRGBA(index)

                buffer[pos++] = color[0]
                buffer[pos++] = color[1]
                buffer[pos++] = color[2]
                buffer[pos++] = color[3]

                gifBuffer[posGif++] = gamePalette.getGifColor(index)
            }
        }
        return buffer
    }

    companion object {

        val gamePalette: ColorPalette = ColorPalette(
            listOf(
                "#000000",
                "#1D2B53",
                "#7E2553",
                "#008751",
                "#AB5236",
                "#5F574F",
                "#C2C3C7",
                "#FFF1E8",
                "#FF004D",
                "#FFA300",
                "#FFEC27",
                "#00E436",
                "#29ADFF",
                "#83769C",
                "#FF77A8",
                "#FFCCAA"
            )
        )
    }
}
