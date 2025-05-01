package com.github.minigdx.tiny.render.operations

import com.github.minigdx.tiny.Pixel
import com.github.minigdx.tiny.engine.GameResourceAccess
import com.github.minigdx.tiny.graphic.PixelArray
import com.github.minigdx.tiny.input.internal.ObjectPool
import com.github.minigdx.tiny.input.internal.PoolObject
import com.github.minigdx.tiny.render.OperationsRender
import com.github.minigdx.tiny.render.RenderContext
import com.github.minigdx.tiny.render.RenderUnit
import com.github.minigdx.tiny.resources.ResourceType
import com.github.minigdx.tiny.resources.SpriteSheet

class DrawSprite(
    var source: SpriteSheet? = null,
    // dither pattern
    var dither: Int = 0xFFFF,
    attributes: List<DrawSpriteAttribute> = emptyList(),
    override var pool: ObjectPool<DrawSprite>? = null,
) : RenderOperation, PoolObject<DrawSprite> {
    override val target = RenderUnit.GPU

    internal val _attributes = attributes.toMutableList()

    // TODO: set the uv/vertex data directly to avoid iterating twice ?
    val attributes: List<DrawSpriteAttribute>
        get() = _attributes

    constructor(
        source: SpriteSheet,
        sourceX: Pixel,
        sourceY: Pixel,
        sourceWidth: Pixel,
        sourceHeight: Pixel,
        destinationX: Pixel = 0,
        destinationY: Pixel = 0,
        flipX: Boolean = false,
        flipY: Boolean = false,
        // dither pattern
        dither: Int = 0xFFFF,
    ) : this(
        source,
        dither,
        listOf(
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
        ),
    )

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
        var sourceX: Pixel = 0,
        var sourceY: Pixel = 0,
        var sourceWidth: Pixel = 0,
        var sourceHeight: Pixel = 0,
        var destinationX: Pixel = 0,
        var destinationY: Pixel = 0,
        var flipX: Boolean = false,
        var flipY: Boolean = false,
        override var pool: ObjectPool<DrawSpriteAttribute>? = null,
    ) : PoolObject<DrawSpriteAttribute> {
        val positionLeft: Pixel
            get() = destinationX
        val positionRight: Int
            get() = destinationX + sourceWidth
        val positionUp: Pixel
            get() = destinationY
        val positionDown: Int
            get() = destinationY + sourceHeight
        val uvLeft: Pixel
            get() = sourceX
        val uvRight: Int
            get() = sourceX + sourceWidth
        val uvUp: Pixel
            get() = sourceY
        val uvDown: Int
            get() = sourceY + sourceHeight

        override fun release() {
            TODO("Not yet implemented")
        }
    }

    companion object {
        const val MAX_SPRITE_PER_COMMAND = 100

        fun from(
            resourceAccess: GameResourceAccess,
            name: String,
            tileset: PixelArray,
            attributes: List<DrawSpriteAttribute>,
        ): List<DrawSprite> {
            val spriteSheet =
                SpriteSheet(
                    0,
                    0,
                    name,
                    ResourceType.GAME_SPRITESHEET,
                    tileset,
                    tileset.width,
                    tileset.height,
                    false,
                )

            return attributes.chunked(MAX_SPRITE_PER_COMMAND).map { chunk ->
                val operation = resourceAccess.obtain(DrawSprite::class)
                operation.source = spriteSheet
                operation._attributes.addAll(chunk)
                operation.dither = resourceAccess.frameBuffer.blender.dithering
                operation
            }
        }
    }

    override fun release() {
        pool?.destroyInstance(this)
    }
}
