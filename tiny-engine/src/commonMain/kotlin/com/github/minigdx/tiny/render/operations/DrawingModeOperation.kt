package com.github.minigdx.tiny.render.operations

import com.github.minigdx.tiny.platform.DrawingMode
import com.github.minigdx.tiny.render.OperationsRender
import com.github.minigdx.tiny.render.RenderContext
import com.github.minigdx.tiny.render.RenderUnit

data class DrawingModeOperation(var mode: DrawingMode) : RenderOperation {
    override val target: RenderUnit = RenderUnit.GPU

    override fun executeGPU(
        context: RenderContext,
        renderUnit: OperationsRender,
    ) {
        renderUnit.setDrawingMode(context, this)
    }
}
