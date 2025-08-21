package com.github.minigdx.tiny.resources

import com.github.minigdx.tiny.graphic.PixelArray
import com.github.minigdx.tiny.resources.ldtk.Ldtk

class GameLevel(
    override val version: Int,
    override val index: Int,
    override val name: String,
    override val type: ResourceType = ResourceType.GAME_LEVEL,
    override var reload: Boolean,
    val ldtk: Ldtk,
    val tilesset: Map<String, SpriteSheet>,
) : GameResource
