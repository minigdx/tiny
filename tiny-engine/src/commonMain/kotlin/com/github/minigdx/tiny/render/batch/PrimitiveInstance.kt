package com.github.minigdx.tiny.render.batch

import com.github.minigdx.tiny.ColorIndex
import com.github.minigdx.tiny.Pixel
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

class PrimitiveInstance(
    var type: Int = 0,
    var color: ColorIndex = 0,
    var dither: Int = 0,
    var meshX: Pixel = 0,
    var meshY: Pixel = 0,
    var meshWidth: Pixel = 0,
    var meshHeight: Pixel = 0,
    var parameters: Array<Int> = Array(6) { 0 },
    var filled: Boolean = false,
    var depth: Float = 0f,
) : Instance {
    fun setRect(
        x: Pixel,
        y: Pixel,
        width: Pixel,
        height: Pixel,
        color: ColorIndex,
        dither: Int,
        filled: Boolean = false,
        depth: Float,
    ): PrimitiveInstance {
        type = 0
        meshX = x
        meshY = y
        meshWidth = width
        meshHeight = height
        this.filled = filled
        this.color = color
        this.dither = dither
        this.depth = depth
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
        dither: Int,
        filled: Boolean = false,
        depth: Float,
    ): PrimitiveInstance {
        type = 1
        // Find the most left x coordinate
        meshX = min(min(x1, x2), x3)
        // Find the topmost y coordinate (minimum y value)
        meshY = min(min(y1, y2), y3)
        meshWidth = max(max(x1, x2), x3) - meshX + 1
        meshHeight = max(max(y1, y2), y3) - meshY + 1

        // The shader creates three edges from vertices: p0→p1, p0→p2, p1→p2
        // We need to arrange the original triangle vertices (v1, v2, v3) as (p0, p1, p2)
        // such that all three shader edges go from left to right

        // Sort vertices by x-coordinate to get leftmost, middle, rightmost
        val sortedVertices = listOf(x1 to y1, x2 to y2, x3 to y3).sortedBy { it.first }
        val left = sortedVertices[0]
        val middle = sortedVertices[1]
        val right = sortedVertices[2]

        // Assign p0=leftmost ensures p0→p1 and p0→p2 go left-to-right
        // Assign p1=middle, p2=right ensures p1→p2 goes left-to-right
        // All three shader edges (p0→p1, p0→p2, p1→p2) now go left-to-right
        parameters[0] = left.first
        parameters[1] = left.second
        parameters[2] = middle.first
        parameters[3] = middle.second
        parameters[4] = right.first
        parameters[5] = right.second

        this.filled = filled
        this.color = color
        this.dither = dither
        this.depth = depth
        return this
    }

    fun setCircle(
        x: Pixel,
        y: Pixel,
        radius: Pixel,
        color: ColorIndex,
        dither: Int,
        filled: Boolean = false,
        depth: Float,
    ): PrimitiveInstance {
        type = 2
        meshX = x - radius
        meshY = y - radius
        meshWidth = 2 * radius + 1
        meshHeight = 2 * radius + 1
        this.color = color
        this.dither = dither
        this.filled = filled

        parameters[0] = x + 1
        parameters[1] = y + 1
        parameters[2] = radius
        this.depth = depth
        return this
    }

    fun setLine(
        x1: Pixel,
        y1: Pixel,
        x2: Pixel,
        y2: Pixel,
        dither: Int,
        color: ColorIndex,
        depth: Float,
    ): PrimitiveInstance {
        type = 3
        this.color = color
        this.dither = dither
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
        this.depth = depth
        return this
    }

    fun setPoint(
        x: Pixel,
        y: Pixel,
        dither: Int,
        color: ColorIndex,
        depth: Float,
    ): PrimitiveInstance {
        type = 4
        this.color = color
        this.dither = dither
        this.filled = false
        meshX = x
        meshY = y
        meshWidth = 1
        meshHeight = 1
        this.depth = depth
        return this
    }

    override fun reset() = Unit
}
