package com.github.minigdx.tiny.render.batch

import com.github.minigdx.tiny.graphic.PixelArray
import com.github.minigdx.tiny.resources.ResourceType
import com.github.minigdx.tiny.resources.SpriteSheet
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class SpriteBatchTest {
    private fun createTestSpriteSheet(): SpriteSheet {
        return SpriteSheet(
            version = 1,
            index = 0,
            name = "test",
            type = ResourceType.GAME_SPRITESHEET,
            pixels = PixelArray(16, 16),
            width = 16,
            height = 16,
        )
    }

    private fun createTestPrimitiveSpriteSheet(): SpriteSheet {
        return SpriteSheet(
            version = 1,
            index = 0,
            name = "primitive",
            type = ResourceType.PRIMITIVE_SPRITESHEET,
            pixels = PixelArray(16, 16),
            width = 16,
            height = 16,
        )
    }

    private fun createTestBatchKey(): BatchKey {
        return BatchKey(
            dither = 0,
            palette = emptyArray(),
            camera = null,
            clipper = null,
        )
    }

    @Test
    fun canAddSprite_with_same_key_returns_true() {
        val spriteSheet = createTestSpriteSheet()
        val key = createTestBatchKey()
        val batch = SpriteBatch(key)

        val sameKey = createTestBatchKey()

        assertTrue(batch.canAddSprite(sameKey, spriteSheet))
    }

    @Test
    fun canAddSprite_with_max_instances_returns_false() {
        val spriteSheet = createTestSpriteSheet()
        val key = createTestBatchKey()
        val batch = SpriteBatch(_key = key)

        // Fill the batch to maximum capacity
        repeat(SpriteBatch.MAX_SPRITE_PER_BATCH) {
            batch.instances.add(SpriteInstance())
        }

        val sameKey = createTestBatchKey()

        assertFalse(batch.canAddSprite(sameKey, spriteSheet))
    }

    @Test
    fun canAddSprite_with_different_key_returns_false() {
        val spriteSheet = createTestSpriteSheet()

        val key1 = BatchKey(
            dither = 0,
            palette = emptyArray(),
            camera = null,
            clipper = null,
        )

        val key2 = BatchKey(
            // Different dither value makes keys different
            dither = 1,
            palette = emptyArray(),
            camera = null,
            clipper = null,
        )

        val batch = SpriteBatch(_key = key1)

        assertFalse(batch.canAddSprite(key2, spriteSheet))
    }

    @Test
    fun add_primitive_returns_true() {
        val primitiveSheet = createTestPrimitiveSpriteSheet()
        val key = createTestBatchKey()
        val batch = SpriteBatch(_key = key)

        assertTrue(batch.canAddSprite(key, primitiveSheet))
    }

    @Test
    fun add_primitive_then_primitive_returns_true_but_no_new_instance() {
        val primitiveSheet1 = createTestPrimitiveSpriteSheet()
        val primitiveSheet2 = SpriteSheet(
            version = 1,
            index = 1,
            name = "another_primitive",
            type = ResourceType.PRIMITIVE_SPRITESHEET,
            pixels = PixelArray(16, 16),
            width = 16,
            height = 16,
        )

        val key1 = createTestBatchKey()
        val batch = SpriteBatch()

        assertTrue(batch.addSprite(key1, primitiveSheet1, SpriteInstance()))
        assertTrue(batch.addSprite(key1, primitiveSheet2, SpriteInstance()))

        assertEquals(1, batch.instances.size)
    }

    @Test
    fun game_spritesheet_then_primitive_returns_true() {
        val gameSheet = createTestSpriteSheet()
        val primitiveSheet = createTestPrimitiveSpriteSheet()

        val gameKey = createTestBatchKey()
        val primitiveKey = createTestBatchKey()
        val batch = SpriteBatch(_key = gameKey)

        assertTrue(batch.canAddSprite(primitiveKey, primitiveSheet))
    }

    @Test
    fun game_spritesheet_then_primitive_then_game_spritesheet_returns_false() {
        val gameSheet = createTestSpriteSheet()
        val primitiveSheet = createTestPrimitiveSpriteSheet()

        val key = createTestBatchKey()
        val batch = SpriteBatch()

        // Add the game spritesheet batch
        batch.addSprite(key, gameSheet, SpriteInstance())

        // Add primitive to the game spritesheet batch
        batch.addSprite(key, primitiveSheet, SpriteInstance())

        // Now trying to add game spritesheet should return false
        assertFalse(batch.canAddSprite(key, gameSheet))
    }

    @Test
    fun primitive_then_game_spritesheet_returns_true() {
        val primitiveSheet = createTestPrimitiveSpriteSheet()
        val gameSheet = createTestSpriteSheet()

        val primitiveKey = createTestBatchKey()
        val gameKey = createTestBatchKey()
        val batch = SpriteBatch(_key = primitiveKey)

        assertTrue(batch.canAddSprite(gameKey, gameSheet))
    }

    @Test
    fun primitive_then_game_spritesheet_then_primitive_returns_false() {
        val primitiveSheet = createTestPrimitiveSpriteSheet()
        val gameSheet = createTestSpriteSheet()

        val gameKey = createTestBatchKey()
        val batch = SpriteBatch()

        // Add primitive spritesheet
        batch.addSprite(gameKey, primitiveSheet, SpriteInstance())

        // Add game spritesheet
        batch.addSprite(gameKey, gameSheet, SpriteInstance())

        // Now trying to add primitive should return false
        assertFalse(batch.canAddSprite(gameKey, primitiveSheet))
    }
}
