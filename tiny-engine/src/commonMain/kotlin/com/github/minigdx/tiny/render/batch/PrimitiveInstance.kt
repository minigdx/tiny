package com.github.minigdx.tiny.render.batch

import com.github.minigdx.tiny.ColorIndex
import com.github.minigdx.tiny.Pixel
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

class PrimitiveInstance(
    var type: Int = 0,
    var color: ColorIndex = 0,
    var meshX: Pixel = 0,
    var meshY: Pixel = 0,
    var meshWidth: Pixel = 0,
    var meshHeight: Pixel = 0,
    var parameters: Array<Int> = Array(6) { 0 },
    var filled: Boolean = false,
) : Instance {
    fun setRect(
        x: Pixel,
        y: Pixel,
        width: Pixel,
        height: Pixel,
        color: ColorIndex,
        filled: Boolean = false,
    ): PrimitiveInstance {
        type = 0
        meshX = x
        meshY = y
        meshWidth = width
        meshHeight = height
        this.filled = filled
        this.color = color
        return this
    }

    fun setTriangle(
        x1: Pixel,
        y1: Pixel,
        x2: Pixel,
        y2: Pixel,
        x3: Pixel,
        y3: Pixel,
        color: ColorIndex,
        filled: Boolean = false,
    ): PrimitiveInstance {
        type = 1
        // Find the most left x coordinate
        meshX = min(min(x1, x2), x3)
        // Find the most botton coordinate
        meshY = min(min(y1, y2), y3)
        meshWidth = max(max(x1, x2), x3) - meshX
        meshHeight = max(max(y1, y2), y3) - meshY
        // Set the triangle inside the mesh
        parameters[0] = x1
        parameters[1] = y1
        parameters[2] = x2
        parameters[3] = y2
        parameters[4] = x3
        parameters[5] = y3

        this.filled = filled
        this.color = color
        return this
    }

    fun setCircle(
        x: Pixel,
        y: Pixel,
        radius: Pixel,
        color: ColorIndex,
        filled: Boolean = false,
    ): PrimitiveInstance {
        type = 2
        meshX = x - radius
        meshY = y - radius
        meshWidth = 2 * radius + 1
        meshHeight = 2 * radius + 1
        this.color = color
        this.filled = filled
        return this
    }

    fun setLine(
        x1: Pixel,
        y1: Pixel,
        x2: Pixel,
        y2: Pixel,
        color: ColorIndex,
    ): PrimitiveInstance {
        type = 3
        this.color = color
        meshX = min(x1, x2)
        meshY = min(y1, y2)
        meshWidth = (1 + abs(x2 - x1))
        meshHeight = (1 + abs(y2 - y1))
        val (a, b, c, d) = if (x1 < x2) {
            listOf(x1, y1, x2, y2)
        } else {
            listOf(x2, y2, x1, y1)
        }
        parameters[0] = a
        parameters[1] = b
        parameters[2] = c
        parameters[3] = d

        this.filled = false
        return this
    }

    fun setPoint(
        x: Pixel,
        y: Pixel,
        color: ColorIndex,
    ) {
        type = 4
        this.color = color
        this.filled = false
        meshX = x
        meshY = y
        meshWidth = 1
        meshHeight = 1
    }

    override fun reset() = Unit
}
