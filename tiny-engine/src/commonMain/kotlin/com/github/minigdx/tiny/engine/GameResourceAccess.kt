package com.github.minigdx.tiny.engine

import com.github.minigdx.tiny.graphic.FrameBuffer
import com.github.minigdx.tiny.resources.GameLevel
import com.github.minigdx.tiny.resources.GameScript
import com.github.minigdx.tiny.resources.Sound
import com.github.minigdx.tiny.resources.SpriteSheet

data class DebugMessage(val mesage: String, val color: String)

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
     * Access a sprite sheet by its index.
     */
    fun spritesheet(index: Int): SpriteSheet?

    fun spritesheet(sheet: SpriteSheet)

    /**
     * Access a level by its index.
     */
    fun level(index: Int): GameLevel?

    fun sound(index: Int): Sound?

    /**
     * Find a script by its name.
     */
    fun script(name: String): GameScript?

    /**
     * Print a message over the current screen,
     * after the game rendered.
     */
    fun debug(str: DebugMessage) = Unit
}
