package com.github.minigdx.tiny.render

import com.github.minigdx.tiny.engine.RenderOperation
import com.github.minigdx.tiny.platform.WindowManager

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
}
