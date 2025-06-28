package com.github.minigdx.tiny.render

import com.github.minigdx.tiny.render.operations.DrawSprite
import com.github.minigdx.tiny.render.operations.DrawingModeOperation

interface OperationsRender {
    fun drawSprite(
        context: RenderContext,
        op: DrawSprite,
    )

    fun setDrawingMode(
        context: RenderContext,
        op: DrawingModeOperation,
    )
}
