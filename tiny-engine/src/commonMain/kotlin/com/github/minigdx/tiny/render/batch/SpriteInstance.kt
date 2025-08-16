package com.github.minigdx.tiny.render.batch

import com.github.minigdx.tiny.Pixel

/**
 * Represents a single sprite instance within a batch.
 * Contains the source region coordinates from the spritesheet and destination
 * coordinates on screen, along with flip flags for horizontal/vertical mirroring.
 * Instances with the same rendering state are grouped together in SpriteBatch objects.
 */
class SpriteInstance(
    var sourceX: Pixel = 0,
    var sourceY: Pixel = 0,
    var sourceWidth: Pixel = 0,
    var sourceHeight: Pixel = 0,
    var destinationX: Pixel = 0,
    var destinationY: Pixel = 0,
    var flipX: Boolean = false,
    var flipY: Boolean = false,
) {
    fun set(
        sourceX: Pixel,
        sourceY: Pixel,
        sourceWidth: Pixel,
        sourceHeight: Pixel,
        destinationX: Pixel,
        destinationY: Pixel,
        flipX: Boolean,
        flipY: Boolean,
    ): SpriteInstance {
        this.sourceX = sourceX
        this.sourceY = sourceY
        this.sourceWidth = sourceWidth
        this.sourceHeight = sourceHeight
        this.destinationX = destinationX
        this.destinationY = destinationY
        this.flipX = flipX
        this.flipY = flipY

        return this
    }
}
