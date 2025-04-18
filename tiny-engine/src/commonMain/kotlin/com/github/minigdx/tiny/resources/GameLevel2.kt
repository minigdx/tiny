package com.github.minigdx.tiny.resources

import com.github.minigdx.tiny.graphic.PixelArray
import com.github.minigdx.tiny.resources.ldtk.Ldtk

class GameLevel2(
    override val version: Int,
    override val index: Int,
    override val name: String,
    override val type: ResourceType,
    override var reload: Boolean,
    val ldtk: Ldtk,
    val tilesset: Map<String, PixelArray>,
) : GameResource
