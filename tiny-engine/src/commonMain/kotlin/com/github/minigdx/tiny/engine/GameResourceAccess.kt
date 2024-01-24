package com.github.minigdx.tiny.engine

import com.github.minigdx.tiny.graphic.FrameBuffer
import com.github.minigdx.tiny.resources.GameLevel
import com.github.minigdx.tiny.resources.GameScript
import com.github.minigdx.tiny.resources.Sound
import com.github.minigdx.tiny.resources.SpriteSheet
import com.github.minigdx.tiny.sound.WaveGenerator

sealed interface DebugAction
data class DebugMessage(val mesage: String, val color: String) : DebugAction
data class DebugRect(val x: Int, val y: Int, val width: Int, val height: Int, val color: String, val filed: Boolean = false) : DebugAction
data class DebugPoint(val x: Int, val y: Int, val color: String) : DebugAction
data class DebugLine(val x1: Int, val y1: Int, val x2: Int, val y2: Int, val color: String) : DebugAction

data class DebugEnabled(val enabled: Boolean) : DebugAction

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

    /**
     * Access sound by its index
     */
    fun sound(index: Int): Sound?

    /**
     * Play a note represented by a wave.
     *
     * All notes added in the same update loop will be played at the same time
     * at the end of the update loop.
     */
    fun note(wave: WaveGenerator)

    /**
     * Find a script by its name.
     */
    fun script(name: String): GameScript?

    /**
     * Perform an action (draw message, rect, ...) over the current screen,
     * after the game rendered.
     */
    fun debug(action: DebugAction) = Unit
}
