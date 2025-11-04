package com.github.minigdx.tiny.graphic

import com.github.minigdx.tiny.Pixel
import kotlin.math.max
import kotlin.math.min

/**
 * Limit the part of the screen that can be drawn.
 */
class Clipper(private val screenWidth: Pixel, private val screenHeight: Pixel) {
    var left = 0
        private set
    var right = screenWidth
        private set
    var top = 0
        private set
    var bottom = screenHeight
        private set

    var width = screenWidth
        private set

    var height = screenHeight
        private set

    internal var updated: Boolean = true

    fun set(
        x: Pixel,
        y: Pixel,
        width: Pixel,
        height: Pixel,
    ): Clipper {
        // Left corner of the screen area
        left = max(0, min(x, x + width))
        // Right corner of the screen area
        right = min(max(x, x + width), this.screenWidth)
        // Top corner of the screen area (bottom of the screen)
        top = max(0, min(y, y + height))
        // Bottom corner of the screen area
        bottom = min(max(y, y + height), this.screenHeight)

        this.width = right - left
        this.height = bottom - top

        updated = true

        return this
    }

    fun isIn(
        x: Pixel,
        y: Pixel,
    ): Boolean {
        return x in left until right && y in top until bottom
    }

    fun reset() {
        left = 0
        right = screenWidth
        top = 0
        bottom = screenHeight

        width = screenWidth
        height = screenHeight

        updated = true
    }

    override fun equals(other: Any?): Boolean {
        val o = other as? Clipper ?: return false
        return screenWidth == o.screenWidth && screenHeight == o.screenHeight &&
            left == o.left && right == o.right &&
            top == o.top && bottom == o.bottom
    }

    override fun hashCode(): Int {
        var result = screenWidth
        result = 31 * result + screenHeight
        result = 31 * result + left
        result = 31 * result + right
        result = 31 * result + top
        result = 31 * result + bottom
        return result
    }
}
