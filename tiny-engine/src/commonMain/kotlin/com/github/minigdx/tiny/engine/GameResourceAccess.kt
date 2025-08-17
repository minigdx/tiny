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
import com.github.minigdx.tiny.sound.MusicalSequence
import com.github.minigdx.tiny.sound.SoundHandler
import kotlin.reflect.KClass

/**
 * Descriptor to access the game resource
 */
@Deprecated("To be replaced with GameResourceAccess2")
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
     * Read the full screen
     */
    fun readFrame(): FrameBuffer

    fun renderAsBuffer(block: () -> Unit): FrameBuffer

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
    fun play(musicalBar: MusicalBar): SoundHandler

    /**
     * Play a musical sequence.
     */
    fun play(musicalSequence: MusicalSequence): SoundHandler

    /**
     * Play a track
     */
    fun play(track: MusicalSequence.Track): SoundHandler

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
     * Add an Ops to be executed by the shader
     */
    fun addOp(op: RenderOperation) = Unit

    /**
     * Obtain a new instance of the operation.
     */
    @Deprecated("To be removed")
    fun <T : PoolObject<T>> obtain(type: KClass<T>): T

    /**
     * Release this instance. This instance will be reused later.
     */
    @Deprecated("To be removed")
    fun <T : PoolObject<T>> releaseOperation(
        operation: T,
        type: KClass<T>,
    )

    fun exportAsSound(sequence: MusicalSequence)
}
