package com.github.minigdx.tiny.render

import com.github.minigdx.tiny.Pixel
import com.github.minigdx.tiny.engine.Frame
import com.github.minigdx.tiny.engine.RenderOperation
import com.github.minigdx.tiny.platform.RenderContext
import com.github.minigdx.tiny.platform.WindowManager

interface Render {
    fun init(windowManager: WindowManager): RenderContext

    fun draw(
        context: RenderContext,
        image: ByteArray,
        width: Pixel,
        height: Pixel,
    )

    fun draw(
        context: RenderContext,
        ops: List<RenderOperation>,
    )

    fun drawOffscreen(
        context: RenderContext,
        ops: List<RenderOperation>,
    ): Frame
}
