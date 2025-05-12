package com.github.minigdx.tiny.render.gl

import com.danielgergely.kgl.ByteBuffer
import com.github.minigdx.tiny.ColorIndex
import com.github.minigdx.tiny.Pixel
import com.github.minigdx.tiny.engine.GameOptions
import com.github.minigdx.tiny.graphic.PixelArray
import com.github.minigdx.tiny.graphic.PixelFormat
import com.github.minigdx.tiny.render.RenderFrame
import kotlin.math.max

class OpenGLFrame(
    private val buffer: ByteBuffer,
    private val gameOptions: GameOptions,
) : RenderFrame {
    private val tmp = ByteArray(PixelFormat.RGBA)

    /**
     * Convert the actual Frame (with RGBA) into a Pixel Array of Color index.
     */
    override fun copyInto(pixelArray: PixelArray) {
        check(pixelArray.pixelFormat == PixelFormat.INDEX) {
            "The copyInto is expecting to copy the buffer into a index pixel format."
        }

        buffer.position = 0

        // Read the buffer starting the last list as
        // the bottom line is the first line in the buffer
        for (x in 0 until gameOptions.width) {
            for (y in 0 until gameOptions.height) {
                buffer.position = (x + (max(0, gameOptions.height - 1 - y)) * gameOptions.width) * PixelFormat.RGBA

                readBytes(buffer, tmp)

                val colorIndex = gameOptions.colors().getColorIndex(tmp)
                pixelArray.set(x, y, colorIndex)
            }
        }

        // Reset buffer position so it can be reused.
        buffer.position = 0
    }

    override fun getPixel(
        x: Pixel,
        y: Pixel,
    ): ColorIndex {
        val invertedY = (gameOptions.height - 1) - y
        buffer.position = (x + invertedY * gameOptions.width) * PixelFormat.RGBA
        readBytes(buffer, tmp)
        val colorIndex = gameOptions.colors().getColorIndex(tmp)
        // Reset buffer position so it can be reused.
        buffer.position = 0

        return colorIndex
    }
}

expect fun readBytes(
    buffer: ByteBuffer,
    out: ByteArray,
)
