package com.github.minigdx.tiny.engine

import com.github.minigdx.tiny.graphic.FrameBuffer
import com.github.minigdx.tiny.resources.GameLevel
import com.github.minigdx.tiny.resources.Sound
import com.github.minigdx.tiny.resources.SpriteSheet

/**
 * Descriptor to access the game resource
 */
interface GameResourceAccess {
    /**
     * The boot script sprite sheet.
     * Only the engine / bootscript can control what to do with it.
     */
    val bootSpritesheet: SpriteSheet?

    /**
     * Frame buffer of the game engine.
     */
    val frameBuffer: FrameBuffer

    /**
     * Acces a sprite sheet by it's index.
     */
    fun spritesheet(index: Int): SpriteSheet?

    fun spritesheet(sheet: SpriteSheet)

    /**
     * Access a level by it's index.
     */
    fun level(index: Int): GameLevel?

    fun sound(index: Int): Sound?
}
