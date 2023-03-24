package com.github.minigdx.tiny.resources

import com.github.minigdx.tiny.Pixel
import com.github.minigdx.tiny.graphic.FrameBuffer
import com.github.minigdx.tiny.graphic.PixelArray
import kotlin.math.min

class SpriteSheet(
    var pixels: PixelArray,
    var width: Pixel,
    var height: Pixel,
    override val type: ResourceType,
    override var reload: Boolean = true,
    override var isLoaded: Boolean = false,
) : GameResource {
}
