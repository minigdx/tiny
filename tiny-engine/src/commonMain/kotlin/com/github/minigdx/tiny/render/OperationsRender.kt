package com.github.minigdx.tiny.render

import com.github.minigdx.tiny.engine.DrawSprite

interface OperationsRender {
    fun drawSprite(
        context: RenderContext,
        op: DrawSprite,
    )
}
