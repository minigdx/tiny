package com.github.minigdx.tiny.render

interface Render : ReadRender, WriteRender {
    fun executeOffScreen(
        context: RenderContext,
        block: () -> Unit,
    ): RenderFrame
}
