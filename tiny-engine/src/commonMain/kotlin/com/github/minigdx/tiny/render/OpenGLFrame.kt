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

        // Read the buffer starting the last list as
        // the bottom line is the first line in the buffer
        for (x in 0 until gameOptions.width) {
            for (y in 0 until gameOptions.height) {
                val i = x + y * gameOptions.width

                buffer.position = i * PixelFormat.RGBA
                buffer.get(tmp)
                frame.colorIndexBuffer.set(x, (gameOptions.height - 1) - y, gameOptions.colors().getColorIndex(tmp))
            }
        }

        // Reset buffer position so it can be reused.
        buffer.position = 0

        saveAsScreenshot(buffer, frame, gameOptions.width, gameOptions.height)
        return frame
    }
}

expect fun saveAsScreenshot(
    buffer: ByteBuffer,
    frame: FrameBuffer,
    width: Int,
    height: Int,
)
