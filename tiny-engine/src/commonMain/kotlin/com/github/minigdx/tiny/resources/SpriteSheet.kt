package com.github.minigdx.tiny.resources

import com.github.minigdx.tiny.Pixel
import com.github.minigdx.tiny.graphic.PixelArray

class SpriteSheet(
    override val index: Int,
    override val name: String,
    override val type: ResourceType,
    var pixels: PixelArray,
    var width: Pixel,
    var height: Pixel,
    override var reload: Boolean = false,
) : GameResource
