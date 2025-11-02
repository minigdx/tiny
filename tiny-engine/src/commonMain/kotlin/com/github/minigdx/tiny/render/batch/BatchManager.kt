package com.github.minigdx.tiny.render.batch

import com.github.minigdx.tiny.input.internal.ObjectPool

/**
 * Manages the creation and lifecycle of sprite batches for efficient rendering.
 * Groups sprites with identical rendering state into batches to minimize GPU state changes.
 * Handles automatic batching when sprites are submitted, creating new batches as needed
 * and flushing full batches to the render queue. Integrates with the object pooling
 * system for memory efficiency.
 */
class BatchManager<K : BatchKey, T : Instance, B : Batch<T>>(
    private val keyGenerator: () -> K,
    private val instanceGenerator: () -> T,
    private val batchGenerator: () -> B,
) {
    private val batches = mutableMapOf<K, MutableList<B>>()

    private val batchKeyPool = object : ObjectPool<K>(DEFAULT_SPRITE_POOL_SIZE) {
        override fun newInstance(): K {
            return keyGenerator()
        }

        override fun destroyInstance(obj: K) {
            obj.reset()
        }
    }

    private val instancePool = object : ObjectPool<T>(DEFAULT_SPRITE_POOL_SIZE) {
        override fun newInstance(): T {
            return instanceGenerator()
        }

        override fun destroyInstance(obj: T) {
            obj.reset()
        }
    }

    private val batchPool = object : ObjectPool<B>(DEFAULT_BATH_POOL_SIZE) {
        override fun newInstance(): B {
            return batchGenerator()
        }

        override fun destroyInstance(obj: B) {
            obj.reset()
        }
    }

    fun createKey(): K {
        return batchKeyPool.obtain()
    }

    fun createInstance(): T {
        return instancePool.obtain()
    }

    fun submit(
        key: K,
        instance: T,
    ) {
        val spriteBatches = batches.getOrPut(key) { mutableListOf() }
        val spriteBatch = spriteBatches.lastOrNull()
            ?.takeIf { it.canAddInto() }
            ?: batchPool.obtain().also { spriteBatches.add(it) }

        spriteBatch.add(instance)

        instancePool.free(instance)
    }

    fun consumeAllBatches(action: (K, B) -> Unit) {
        batches.forEach { (key, spriteBatches) ->
            spriteBatches.forEach { spriteBatch ->
                action.invoke(key, spriteBatch)
            }
        }
        clear()
    }

    fun clear() {
        batchPool.free(batches.values.flatten())
        batchKeyPool.free(batches.keys)

        batches.clear()
    }

    companion object {
        private const val DEFAULT_SPRITE_POOL_SIZE = 1000
        private const val DEFAULT_BATH_POOL_SIZE = 100
    }
}
