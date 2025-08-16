package com.github.minigdx.tiny.render.batch

import com.github.minigdx.tiny.ColorIndex
import com.github.minigdx.tiny.Pixel
import com.github.minigdx.tiny.engine.GameResourceAccess
import com.github.minigdx.tiny.graphic.Camera
import com.github.minigdx.tiny.graphic.Clipper
import com.github.minigdx.tiny.input.internal.ObjectPool
import com.github.minigdx.tiny.resources.SpriteSheet

/**
 * Manages the creation and lifecycle of sprite batches for efficient rendering.
 * Groups sprites with identical rendering state into batches to minimize GPU state changes.
 * Handles automatic batching when sprites are submitted, creating new batches as needed
 * and flushing full batches to the render queue. Integrates with the object pooling
 * system for memory efficiency.
 */
class BatchManager(
    private val resourceAccess: GameResourceAccess,
) {
    private val activeBatches = mutableListOf<SpriteBatch>()

    private val batchKeyPool = object : ObjectPool<BatchKey>(DEFAULT_SPRITE_POOL_SIZE) {
        override fun newInstance(): BatchKey {
            return BatchKey()
        }

        override fun destroyInstance(obj: BatchKey) = Unit
    }

    private val spriteInstancePool = object : ObjectPool<SpriteInstance>(DEFAULT_SPRITE_POOL_SIZE) {
        override fun newInstance(): SpriteInstance {
            return SpriteInstance()
        }

        override fun destroyInstance(obj: SpriteInstance) = Unit
    }

    private val spriteBatchPool = object : ObjectPool<SpriteBatch>(DEFAULT_BATH_POOL_SIZE) {
        override fun newInstance(): SpriteBatch {
            return SpriteBatch()
        }

        override fun destroyInstance(obj: SpriteBatch) {
            obj.instances.clear()
            obj.sheets.clear()
            obj._key = null
        }
    }

    /**
     * Submit a new sprite.
     *
     * @return the actual batch that needs to be drawn
     */
    fun submitSprite(
        source: SpriteSheet,
        sourceX: Pixel,
        sourceY: Pixel,
        sourceWidth: Pixel,
        sourceHeight: Pixel,
        destinationX: Pixel,
        destinationY: Pixel,
        flipX: Boolean = false,
        flipY: Boolean = false,
        dither: Int = 0xFFFF,
        palette: Array<ColorIndex> = emptyArray(),
        camera: Camera? = null,
        clipper: Clipper? = null,
    ): SpriteBatch? {
        val key = batchKeyPool.obtain().set(
            dither = dither,
            palette = palette,
            camera = camera,
            clipper = clipper,
        )

        val instance = spriteInstancePool.obtain().set(
            sourceX = sourceX,
            sourceY = sourceY,
            sourceWidth = sourceWidth,
            sourceHeight = sourceHeight,
            destinationX = destinationX,
            destinationY = destinationY,
            flipX = flipX,
            flipY = flipY,
        )

        // Try to add to existing batch
        val existingBatch = activeBatches.lastOrNull()
        if (existingBatch?.addSprite(key, source, instance) == true) {
            return null
        }

        // Create new batch
        val newBatch = spriteBatchPool.obtain()

        newBatch.addSprite(key, source, instance)
        activeBatches.add(newBatch)

        return existingBatch
    }

    fun flushAllBatches() {
        spriteBatchPool.free(activeBatches)
        activeBatches.clear()
    }

    fun getActiveBatchCount(): Int = activeBatches.size

    fun getTotalSpriteCount(): Int = activeBatches.sumOf { it.sprites.size }

    companion object {
        private const val DEFAULT_SPRITE_POOL_SIZE = 1000
        private const val DEFAULT_BATH_POOL_SIZE = 100
    }
}
