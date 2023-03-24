package com.github.minigdx.tiny.resources

import com.github.minigdx.tiny.ColorIndex
import com.github.minigdx.tiny.Pixel
import com.github.minigdx.tiny.graphic.FrameBuffer
import kotlin.math.min

class SpriteSheet(
    var pixels: Array<Array<ColorIndex>>,
    var width: Pixel,
    var height: Pixel,
    override val type: ResourceType,
    override var reload: Boolean = true,
    override var isLoaded: Boolean = false,

    ) : GameResource {
    fun copy(dstX: Pixel, dstY: Pixel, dst: FrameBuffer, x: Pixel, y: Pixel, width: Pixel, height: Pixel) {
        (0 until width).forEach { offsetX ->
            (0 until height).forEach { offsetY ->
                val yy = min(y + offsetY, pixels.size - 1)
                val xx = min(x + offsetX, pixels[yy].size - 1)
                val colorIndex = pixels[yy][xx]
                dst.pixel(dstX + offsetX, dstY + offsetY, colorIndex)
            }
        }
    }
}
