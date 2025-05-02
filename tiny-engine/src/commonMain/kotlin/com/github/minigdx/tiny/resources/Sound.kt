package com.github.minigdx.tiny.resources

import com.github.minigdx.tiny.platform.SoundData

data class Sound(
    override val version: Int,
    override val index: Int,
    override val name: String,
    val data: SoundData,
    override val type: ResourceType = ResourceType.GAME_SOUND,
    override var reload: Boolean = false,
) : GameResource
