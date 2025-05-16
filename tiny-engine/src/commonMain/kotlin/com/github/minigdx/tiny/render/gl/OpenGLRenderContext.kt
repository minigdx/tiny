package com.github.minigdx.tiny.render.gl

import com.danielgergely.kgl.ByteBuffer
import com.danielgergely.kgl.Framebuffer
import com.danielgergely.kgl.Texture
import com.github.minigdx.tiny.platform.WindowManager
import com.github.minigdx.tiny.render.RenderContext

data class OpenGLRenderContext(
    val windowManager: WindowManager,
    /**
     * Reference to the framebuffer used for drawing operation.
     * Should be reference to [offscreenFrameBuffer] or [onscreenFrameBuffer]
     */
    var currentFrameBuffer: FrameBufferContext,
    /**
     * Framebuffer used for offscreen operations.
     */
    val offscreenFrameBuffer: FrameBufferContext,
    /**
     * Framebuffer used to render on the screen later.
     */
    val onscreenFrameBuffer: FrameBufferContext,
) : RenderContext {
    val fbo: Framebuffer
        get() {
            return currentFrameBuffer.fbo
        }

    val fboBuffer: ByteBuffer
        get() {
            return currentFrameBuffer.fboBuffer
        }

    val fboTexture: Texture
        get() {
            return currentFrameBuffer.fboTexture
        }

    fun useOffscreen() {
        currentFrameBuffer = offscreenFrameBuffer
    }

    fun useOnscreen() {
        currentFrameBuffer = onscreenFrameBuffer
    }
}

data class FrameBufferContext(
    /**
     * Reference to the framebuffer
     */
    val fbo: Framebuffer,
    /**
     * Buffer that contains the rendering, when reading pixels from the framebuffer.
     */
    val fboBuffer: ByteBuffer,
    /**
     * Reference to the texture, used to render the frame buffer on the screen.
     */
    val fboTexture: Texture,
)
