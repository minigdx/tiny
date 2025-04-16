package com.github.minigdx.tiny.engine

import com.github.minigdx.tiny.ColorIndex
import com.github.minigdx.tiny.Pixel
import com.github.minigdx.tiny.graphic.FrameBuffer
import com.github.minigdx.tiny.render.OperationsRender
import com.github.minigdx.tiny.render.RenderContext
import com.github.minigdx.tiny.resources.GameLevel
import com.github.minigdx.tiny.resources.GameScript
import com.github.minigdx.tiny.resources.Sound
import com.github.minigdx.tiny.resources.SpriteSheet
import com.github.minigdx.tiny.sound.Song2
import com.github.minigdx.tiny.sound.WaveGenerator

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
     * Play a note represented by a wave.
     *
     * All notes added in the same update loop will be played at the same time
     * at the end of the update loop.
     */
    fun note(wave: WaveGenerator)

    fun sfx(song: Song2)

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
}

/**
 * Specifies the target processing unit for rendering operations.
 */
enum class RenderUnit {
    /**
     * Target the Central Processing Unit (CPU).
     * Often used for tasks less suitable for massive parallelism or requiring complex logic.
     */
    CPU,

    /**
     * Target the Graphics Processing Unit (GPU).
     * Ideal for highly parallel rendering tasks like processing large numbers of pixels.
     */
    GPU,

    /**
     * Target both unit (CPU/GPU).
     *
     * Can be applied in any unit. Because can do both or because it's altering the state of the next operation.
     *
     * Selecting the right unit might depend on the operation by itself or the previous operation, to keep the
     * same unit.
     */
    BOTH,

    ;

    fun compatibleWith(target: RenderUnit): Boolean {
        return when (this) {
            CPU -> target == CPU || target == BOTH
            GPU -> target == GPU || target == BOTH
            BOTH -> true
        }
    }
}

sealed interface RenderOperation {
    val target: RenderUnit

    /**
     * Render the operation on the CPU, by updating the current frame.
     */
    fun executeCPU(): Unit = invalidTarget(RenderUnit.CPU)

    /**
     * Render the operation on the GPU, by using a shader.
     */
    fun executeGPU(
        context: RenderContext,
        renderUnit: OperationsRender,
    ): Unit = invalidTarget(RenderUnit.GPU)

    /**
     * Try to merge the current operation with the previous one, to batch operations.
     */
    fun mergeWith(previousOperation: RenderOperation?): Boolean = false

    private fun invalidTarget(renderUnit: RenderUnit): Nothing =
        throw IllegalStateException(
            "The operation ${this::class.simpleName} does not support $renderUnit render operations. ",
        )
}

/**
 * Set a pixel [color] at the coordinates [x] and [y].
 */
data class SetPixel(
    val x: Pixel,
    val y: Pixel,
    val color: ColorIndex,
    private val frameBuffer: FrameBuffer,
) : RenderOperation {
    override val target = RenderUnit.CPU

    override fun executeCPU() {
        frameBuffer.pixel(x, y, color)
    }
}

/**
 * Clear the full screen by filling it with [color].
 */
data class ClearScreen(
    val color: ColorIndex,
    private val frameBuffer: FrameBuffer,
) : RenderOperation {
    override val target = RenderUnit.CPU

    override fun executeCPU() {
        frameBuffer.clear(color)
    }
}

data class SwapPalette(
    val origin: ColorIndex,
    val destination: ColorIndex,
    private val frameBuffer: FrameBuffer,
) : RenderOperation {
    override val target = RenderUnit.BOTH

    override fun executeCPU() {
        frameBuffer.blender.pal(origin, destination)
    }

    override fun executeGPU(
        context: RenderContext,
        renderUnit: OperationsRender,
    ) {
        TODO()
    }
}

class DrawSprite(
    val source: SpriteSheet,
    sourceX: Pixel,
    sourceY: Pixel,
    sourceWidth: Pixel,
    sourceHeight: Pixel,
    destinationX: Pixel = 0,
    destinationY: Pixel = 0,
    flipX: Boolean = false,
    flipY: Boolean = false,
) : RenderOperation {
    override val target = RenderUnit.GPU

    private val _attributes =
        mutableListOf(
            DrawSpriteAttribute(
                sourceX,
                sourceY,
                sourceWidth,
                sourceHeight,
                destinationX,
                destinationY,
                flipX,
                flipY,
            ),
        )

    val attributes: List<DrawSpriteAttribute>
        get() = _attributes

    override fun executeGPU(
        context: RenderContext,
        renderUnit: OperationsRender,
    ) {
        renderUnit.drawSprite(context, this)
    }

    override fun mergeWith(previousOperation: RenderOperation?): Boolean {
        val operation = previousOperation as? DrawSprite ?: return false
        if (operation.source != source) {
            return false
        }
        // Too many elements in this operation, lets create a new one.
        if (operation._attributes.size >= MAX_SPRITE_PER_COMMAND) {
            return false
        }
        operation._attributes.addAll(_attributes)
        return true
    }

    data class DrawSpriteAttribute(
        val sourceX: Pixel,
        val sourceY: Pixel,
        val sourceWidth: Pixel,
        val sourceHeight: Pixel,
        val destinationX: Pixel,
        val destinationY: Pixel,
        val flipX: Boolean,
        val flipY: Boolean,
    ) {
        val positionLeft = destinationX
        val positionRight = destinationX + sourceWidth
        val positionUp = destinationY
        val positionDown = destinationY + sourceHeight
        val uvLeft = sourceX
        val uvRight = sourceX + sourceWidth
        val uvUp = sourceY
        val uvDown = sourceY + sourceHeight
    }

    companion object {
        const val MAX_SPRITE_PER_COMMAND = 100
    }
}
