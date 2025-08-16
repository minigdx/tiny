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
) {
    // FIXME: add immediate rendering? if immediateRendering -> render the sprite, reset the primitie ?
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
    ): Boolean {
        // First add in this batch
        if (_key == null) {
            return true
        }
        // Check capacity first
        if (instances.size >= MAX_SPRITE_PER_BATCH) {
            return false
        }

        // Check if the key is identical
        if (currentKey != key) {
            return false
        }

        val currentIsPrimitive = currentSpriteSheet.type == ResourceType.PRIMITIVE_SPRITESHEET
        val lastIsPrimitive = lastSpritesheetType == ResourceType.PRIMITIVE_SPRITESHEET

        // if both are same nature
        if (currentIsPrimitive && lastIsPrimitive) {
            return true
        } else if (!currentIsPrimitive && !!lastIsPrimitive) {
            return true
        }

        // Different non-primitive keys cannot be mixed
        return !hasMixedTypes
    }

    fun addSprite(
        key: BatchKey,
        spriteSheet: SpriteSheet,
        instance: SpriteInstance,
    ): Boolean {
        if (!canAddSprite(key, spriteSheet)) return false

        if (_key == null) {
            _key = key
        }
        if (lastSpritesheetType != spriteSheet.type) {
            hasMixedTypes = true
        }
        // The type is different. As we can add the sprite, we can mix the instances
        val effectiveAdd = lastSpritesheetType != spriteSheet.type ||
            // Is not a primitive
            spriteSheet.type != ResourceType.PRIMITIVE_SPRITESHEET

        lastSpritesheetType = spriteSheet.type

        if (effectiveAdd) {
            sheets.add(spriteSheet)
            instances.add(instance)
        }
        return true
    }

    companion object {
        const val MAX_SPRITE_PER_BATCH = 100
    }
}
