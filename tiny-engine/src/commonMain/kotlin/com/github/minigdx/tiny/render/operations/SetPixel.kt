package com.github.minigdx.tiny.render.operations

import com.github.minigdx.tiny.ColorIndex
import com.github.minigdx.tiny.Pixel
import com.github.minigdx.tiny.graphic.FrameBuffer
import com.github.minigdx.tiny.input.internal.ObjectPool
import com.github.minigdx.tiny.input.internal.PoolObject
import com.github.minigdx.tiny.render.RenderUnit

/**
 * Set a pixel [color] at the coordinates [x] and [y].
 */
data class SetPixel(
    var x: Pixel = 0,
    var y: Pixel = 0,
    var color: ColorIndex = 0,
    var frameBuffer: FrameBuffer? = null,
    override var pool: ObjectPool<SetPixel>? = null,
) : RenderOperation, PoolObject<SetPixel> {
    override val target = RenderUnit.CPU

    override fun executeCPU() {
        frameBuffer?.pixel(x, y, color)
    }

    override fun release() {
        pool?.destroyInstance(this)
        pool = null
    }
}
