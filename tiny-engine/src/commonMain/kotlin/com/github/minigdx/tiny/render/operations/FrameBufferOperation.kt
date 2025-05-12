package com.github.minigdx.tiny.render.operations

import com.github.minigdx.tiny.render.RenderUnit

data object FrameBufferOperation : RenderOperation {
    override val target: RenderUnit = RenderUnit.CPU

    override fun mergeWith(previousOperation: RenderOperation?): Boolean {
        return previousOperation == FrameBufferOperation
    }
}
