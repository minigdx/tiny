package com.github.minigdx.tiny.render.batch

import com.github.minigdx.tiny.ColorIndex
import com.github.minigdx.tiny.Pixel
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
class BatchManager {
    private val batches = mutableMapOf<BatchKey, MutableList<SpriteBatch>>()

    private val batchKeyPool = object : ObjectPool<BatchKey>(DEFAULT_SPRITE_POOL_SIZE) {
        override fun newInstance(): BatchKey {
            return BatchKey()
        }

        override fun destroyInstance(obj: BatchKey) {
            obj.reset()
        }
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
            obj.reset()
        }
    }

    /**
     * Submit a new sprite.
     *
     * @return is the actual batches need to be rendered.
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
    ) {
        val key = batchKeyPool.obtain().set(
            spriteSheet = source,
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

        val spriteBatches = batches.getOrPut(key) { mutableListOf() }
        val spriteBatch = spriteBatches.lastOrNull() ?: spriteBatchPool.obtain().also { spriteBatches.add(it) }

        // Try to add to existing batch
        if (!spriteBatch.addSprite(instance)) {
            // Create new batch
            val newBatch = spriteBatchPool.obtain().also { spriteBatches.add(it) }
            newBatch.addSprite(instance)
        }

        spriteInstancePool.free(instance)
    }

    fun consumeAllBatches(action: (BatchKey, SpriteBatch) -> Unit) {
        batches.forEach { (key, spriteBatches) ->
            spriteBatches.forEach { spriteBatch ->
                action.invoke(key, spriteBatch)
            }
        }
        clear()
    }

    fun clear() {
        spriteBatchPool.free(batches.values.flatten())
        batchKeyPool.free(batches.keys)

        batches.clear()
    }

    companion object {
        private const val DEFAULT_SPRITE_POOL_SIZE = 1000
        private const val DEFAULT_BATH_POOL_SIZE = 100
    }
}
