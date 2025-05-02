package com.github.minigdx.tiny.engine

import com.github.minigdx.tiny.ColorIndex
import com.github.minigdx.tiny.graphic.FrameBuffer
import com.github.minigdx.tiny.input.internal.PoolObject
import com.github.minigdx.tiny.render.operations.RenderOperation
import com.github.minigdx.tiny.resources.GameLevel
import com.github.minigdx.tiny.resources.GameScript
import com.github.minigdx.tiny.resources.Sound
import com.github.minigdx.tiny.resources.SpriteSheet
import com.github.minigdx.tiny.sound.MusicalBar
import kotlin.reflect.KClass

sealed interface DebugAction

data class DebugMessage(val mesage: String, val color: String) : DebugAction

data class DebugRect(
    val x: Int,
    val y: Int,
    val width: Int,
    val height: Int,
    val color: String,
    val filed: Boolean = false,
) : DebugAction

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
     * Read the color pixel at the [x, y] coordinates.
     */
    fun readPixel(
        x: Int,
        y: Int,
    ): ColorIndex

    /**
     * Access a sprite sheet by its index.
     */
    fun spritesheet(index: Int): SpriteSheet?

    /**
     * Find a sprite sheet by its name
     */
    fun spritesheet(name: String): Int?

    fun newSpritesheetIndex(): Int

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
     * Access sound by its name
     */
    fun sound(name: String): Sound?

    /**
     * Play a musical bar. Should only be used for tools
     * as it will generating the sound on the fly
     * (which can be CPU intensive during a game)
     */
    fun play(musicalBar: MusicalBar)

    /**
     * Save the content into the file named `filename`.
     * Might be a NO-OP on some platform (ie: web)
     *
     * Should only be used for tools (for now)
     */
    fun save(
        filename: String,
        content: String,
    )

    /**
     * Find a script by its name.
     */
    fun script(name: String): GameScript?

    /**
     * Perform an action (draw message, rect, ...) over the current screen,
     * after the game rendered.
     */
    fun debug(action: DebugAction) = Unit

    /**
     * Add an Ops to be executed by the shader
     */
    fun addOp(op: RenderOperation) = Unit

    /**
     * Obtain a new instance of the operation.
     */
    fun <T : PoolObject<T>> obtain(type: KClass<T>): T

    /**
     * Release this instance. This instance will be reused later.
     */
    fun <T : PoolObject<T>> releaseOperation(
        operation: T,
        type: KClass<T>,
    )
}
