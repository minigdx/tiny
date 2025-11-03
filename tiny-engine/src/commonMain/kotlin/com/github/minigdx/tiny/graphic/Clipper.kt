package com.github.minigdx.tiny.graphic

import com.github.minigdx.tiny.Pixel
import kotlin.math.max
import kotlin.math.min

/**
 * Limit the part of the screen that can be drawn.
 */
class Clipper(private val width: Pixel, private val height: Pixel) {
    var left = 0
        private set
    var right = width
        private set
    var top = 0
        private set
    var bottom = height
        private set

    fun set(
        x: Pixel,
        y: Pixel,
        width: Pixel,
        height: Pixel,
    ) {
        left = max(0, min(x, x + width))
        right = min(max(x, x + width), this.width)
        top = max(0, min(y, y + height))
        bottom = min(max(y, y + height), this.height)
    }

    fun isIn(
        x: Pixel,
        y: Pixel,
    ): Boolean {
        return x in left until right && y in top until bottom
    }

    fun reset() {
        left = 0
        right = width
        top = 0
        bottom = height
    }

    override fun equals(other: Any?): Boolean {
        val o = other as? Clipper ?: return false
        return width == o.width && height == o.height &&
            left == o.left && right == o.right &&
            top == o.top && bottom == o.bottom
    }

    override fun hashCode(): Int {
        var result = width
        result = 31 * result + height
        result = 31 * result + left
        result = 31 * result + right
        result = 31 * result + top
        result = 31 * result + bottom
        return result
    }
}
