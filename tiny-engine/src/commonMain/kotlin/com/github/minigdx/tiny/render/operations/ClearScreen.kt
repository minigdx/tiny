package com.github.minigdx.tiny.render.operations

import com.github.minigdx.tiny.ColorIndex
import com.github.minigdx.tiny.graphic.FrameBuffer
import com.github.minigdx.tiny.input.internal.ObjectPool
import com.github.minigdx.tiny.input.internal.PoolObject
import com.github.minigdx.tiny.render.RenderUnit

/**
 * Clear the full screen by filling it with [color].
 */
data class ClearScreen(
    var color: ColorIndex = 0,
    var frameBuffer: FrameBuffer? = null,
    override var pool: ObjectPool<ClearScreen>? = null,
) : RenderOperation, PoolObject<ClearScreen> {
    override val target = RenderUnit.CPU

    override fun executeCPU() {
        frameBuffer?.clear(color)
    }

    override fun release() {
        pool?.destroyInstance(this)
        pool = null
    }
}
