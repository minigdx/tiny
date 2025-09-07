package com.github.minigdx.tiny.render.batch

import com.github.minigdx.tiny.Pixel
import kotlin.math.abs

class PrimitiveInstance(
    var parameters: Array<Int> = Array(7) { 0 },
) : Instance {
    private fun Boolean.asBitShit(): Int {
        if (!this) return 0x00
        return 0xF0
    }

    fun setRect(
        x: Pixel,
        y: Pixel,
        w: Pixel,
        h: Pixel,
        filled: Boolean = false,
    ): PrimitiveInstance {
        parameters[0] = 0 + filled.asBitShit()
        parameters[1] = x
        parameters[2] = y
        parameters[3] = w
        parameters[4] = h
        parameters[5] = 0
        parameters[6] = 0
        return this
    }

    fun setTriangle(
        x1: Pixel,
        y1: Pixel,
        x2: Pixel,
        y2: Pixel,
        x3: Pixel,
        y3: Pixel,
        filled: Boolean = false,
    ): PrimitiveInstance {
        parameters[0] = 1 + filled.asBitShit()
        parameters[1] = x1
        parameters[2] = y1
        parameters[3] = x2
        parameters[4] = y2
        parameters[5] = x3
        parameters[6] = y3
        return this
    }

    fun setCircle(
        x: Pixel,
        y: Pixel,
        radius: Pixel,
        filled: Boolean = false,
    ): PrimitiveInstance {
        parameters[0] = 2 + filled.asBitShit()
        parameters[1] = x
        parameters[2] = y
        parameters[3] = radius
        parameters[4] = 0
        parameters[5] = 0
        parameters[6] = 0
        return this
    }

    fun setLine(
        x1: Pixel,
        y1: Pixel,
        x2: Pixel,
        y2: Pixel,
    ): PrimitiveInstance {
        parameters[0] = 3
        parameters[1] = x1
        parameters[2] = y1
        parameters[3] = x2 - x1
        parameters[4] = y2 - y1
        parameters[5] = 0
        parameters[6] = 0
        return this
    }

    fun setPoint(
        x: Pixel,
        y: Pixel,
    ) {
        parameters[0] = 4
        parameters[1] = x
        parameters[2] = y
        parameters[3] = 0
        parameters[4] = 0
        parameters[5] = 0
        parameters[6] = 0
    }

    override fun reset() = Unit
}
