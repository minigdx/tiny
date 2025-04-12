package com.github.minigdx.tiny.render

import com.github.minigdx.tiny.Pixel
import com.github.minigdx.tiny.engine.RenderOperation
import com.github.minigdx.tiny.graphic.FrameBuffer
import com.github.minigdx.tiny.platform.WindowManager

interface Render {
    /**
     * Configure the rendering environnement.
     */
    fun init(windowManager: WindowManager): RenderContext

    /**
     * Draw the actual framebuffer on the user screen.
     */
    fun draw(
        context: RenderContext,
        image: ByteArray,
        width: Pixel,
        height: Pixel,
    )

    /**
     * Draw the list of operations into the frame buffer.
     */
    fun drawToFrameBuffer(
        context: RenderContext,
        frameBuffer: FrameBuffer,
        ops: List<RenderOperation>,
    ): FrameBuffer
}
