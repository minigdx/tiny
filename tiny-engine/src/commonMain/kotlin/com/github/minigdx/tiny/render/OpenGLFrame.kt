package com.github.minigdx.tiny.render

import com.danielgergely.kgl.ByteBuffer
import com.github.minigdx.tiny.engine.Frame
import com.github.minigdx.tiny.engine.GameOptions
import com.github.minigdx.tiny.graphic.FrameBuffer
import com.github.minigdx.tiny.graphic.PixelFormat

class OpenGLFrame(
    private val buffer: ByteBuffer,
    private val gameOptions: GameOptions,
) : Frame {
    /**
     * Convert the actual Frame (with RGBA) into a Pixel Array of Color index.
     */
    override fun toFrameBuffer(): FrameBuffer {
        val tmp = ByteArray(PixelFormat.RGBA)
        val frame = FrameBuffer(gameOptions.width, gameOptions.height, gameOptions.colors())

        buffer.position = 0
        var index = 0

        (0 until gameOptions.width * gameOptions.height).forEach { i ->
            buffer.position = i * PixelFormat.RGBA
            buffer.get(tmp)
            frame.colorIndexBuffer.pixels[index++] = gameOptions.colors().getColorIndex(tmp).toByte()
        }

        buffer.position = 0

        return frame
    }
}
