package com.github.minigdx.tiny.render.batch

import com.github.minigdx.tiny.resources.ResourceType
import com.github.minigdx.tiny.resources.SpriteSheet

/**
 * A batch of sprite instances that share the same rendering state.
 * Sprites are grouped by their BatchKey to minimize GPU state changes during rendering.
 * Each batch can hold up to MAX_SPRITE_PER_BATCH sprites before requiring a new batch.
 * Implements efficient merging logic to combine compatible batches when possible.
 */
class SpriteBatch(
    internal var _key: BatchKey? = null,
    internal val instances: MutableList<SpriteInstance> = mutableListOf(),
    internal val sheets: MutableList<SpriteSheet> = mutableListOf(),
    internal val pendingTextureBinds: MutableList<SpriteSheet> = mutableListOf()
) {
    val key: BatchKey
        get() = _key!!

    val spriteSheets: List<SpriteSheet>
        get() = sheets

    val sprites: List<SpriteInstance> get() = instances

    private var hasMixedTypes = false
    private var lastSpritesheetType: ResourceType? = null

    fun canAddSprite(
        currentKey: BatchKey,
        currentSpriteSheet: SpriteSheet,
    ): RejectReason? {
        // First add in this batch
        if (_key == null) {
            return null
        }
        // Check capacity first
        if (instances.size >= MAX_SPRITE_PER_BATCH) {
            return RejectReason.BATCH_FULL
        }

        // Check if the key is identical
        if (currentKey != key) {
            return RejectReason.BATCH_DIFFEREND_PARAMETERS
        }

        val currentIsPrimitive = currentSpriteSheet.type == ResourceType.PRIMITIVE_SPRITESHEET
        val lastIsPrimitive = lastSpritesheetType == ResourceType.PRIMITIVE_SPRITESHEET

        // if both are same nature
        if (currentIsPrimitive && lastIsPrimitive) {
            return null
        } else if (!currentIsPrimitive && !!lastIsPrimitive) {
            return null
        }

        return if (hasMixedTypes) {
            // Different non-primitive cannot be mixed
            RejectReason.BATCH_MIXED
        } else {
            null
        }
    }

    fun addSprite(
        key: BatchKey,
        spriteSheet: SpriteSheet,
        instance: SpriteInstance,
    ): RejectReason? {
        val rejectReason = canAddSprite(key, spriteSheet)
        if (rejectReason != null) return rejectReason

        if (_key == null) {
            _key = key
        }
        if (lastSpritesheetType != spriteSheet.type) {
            hasMixedTypes = true
        }
        // The type is different. As we can add the sprite, we can mix the instances
        val effectiveAdd = lastSpritesheetType != spriteSheet.type ||
            // Is not a primitive
            !spriteSheet.isPrimitives

        lastSpritesheetType = spriteSheet.type

        if (effectiveAdd) {
            sheets.add(spriteSheet)
            instances.add(instance)
        }
        return null
    }

    companion object {
        const val MAX_SPRITE_PER_BATCH = 100
    }

    enum class RejectReason {
        BATCH_FULL,
        BATCH_MIXED,
        BATCH_DIFFEREND_PARAMETERS,
    }
}
