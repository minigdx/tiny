package com.github.minigdx.tiny.render

import com.github.minigdx.tiny.platform.WindowManager
import com.github.minigdx.tiny.render.gl.FrameBufferContext2
import com.github.minigdx.tiny.render.operations.RenderOperation

interface WriteRender {
    /**
     * Configure the rendering environnement.
     */
    fun init(windowManager: WindowManager): RenderContext

    /**
     * Render the list of operations into the frame buffer.
     */
    fun render(
        context: RenderContext,
        ops: List<RenderOperation>,
    )

    /**
     * Draw the frame buffer on the screen
     */
    fun drawOnScreen(context: RenderContext) = Unit

    fun draw(batch: FrameBufferContext2) {
        // 1. Set the context to draw the frame buffer on the screen
        // 2. Draw the frame buffer on the screen
        // 3. rollback the context so new draw will be on the frame buffer.
    }
}
