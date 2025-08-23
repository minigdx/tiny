package com.github.minigdx.tiny.render

import com.github.minigdx.tiny.render.operations.DrawingModeOperation

interface OperationsRender {
    fun setDrawingMode(
        context: RenderContext,
        op: DrawingModeOperation,
    )
}
