package com.github.minigdx.tiny.render.gl

import com.danielgergely.kgl.ByteBuffer
import com.danielgergely.kgl.Framebuffer
import com.danielgergely.kgl.Texture

data class FrameBufferContext(
    /**
     * Framebuffer texture.
     * Use to draw the framebuffer on the screen.
     */
    var frameBufferTexture: Texture,
    /**
     * Reference to the framebuffer.
     * Used to bind the framebuffer in the GPU context.
     */
    var frameBuffer: Framebuffer,
    /**
     * Data in which the Framebuffer will be written.
     * Used to read the rendered framebuffer.
     */
    var frameBufferData: ByteBuffer,
)
