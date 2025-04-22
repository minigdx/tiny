package com.github.minigdx.tiny.render.operations

import com.github.minigdx.tiny.ColorIndex
import com.github.minigdx.tiny.graphic.FrameBuffer
import com.github.minigdx.tiny.render.OperationsRender
import com.github.minigdx.tiny.render.RenderContext
import com.github.minigdx.tiny.render.RenderUnit

data class SwapPalette(
    val origin: ColorIndex,
    val destination: ColorIndex,
    private val frameBuffer: FrameBuffer,
) : RenderOperation {
    override val target = RenderUnit.BOTH

    override fun executeGPU(
        context: RenderContext,
        renderUnit: OperationsRender,
    ) {
        TODO()
    }
}
