package com.github.minigdx.tiny.render.batch

import com.github.minigdx.tiny.graphic.PixelArray
import com.github.minigdx.tiny.resources.ResourceType
import com.github.minigdx.tiny.resources.SpriteSheet
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse

class BatchManagerTest {
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

    @Test
    fun submitSprite_can_submit_sprite() {
        val batchManager = BatchManager()
        val spriteSheet = createTestSpriteSheet()

        val needsRender = batchManager.submitSprite(
            source = spriteSheet,
            sourceX = 0,
            sourceY = 0,
            sourceWidth = 16,
            sourceHeight = 16,
            destinationX = 10,
            destinationY = 10,
        )

        // Basic functionality test - should not require immediate render for first sprite
        assertFalse(needsRender)
        assertEquals(1, batchManager.getActiveBatchCount())
    }

    @Test
    fun submitSprite_different_dither_behaves_correctly() {
        val batchManager = BatchManager()
        val spriteSheet = createTestSpriteSheet()

        // First sprite with default dither
        batchManager.submitSprite(
            source = spriteSheet,
            sourceX = 0,
            sourceY = 0,
            sourceWidth = 16,
            sourceHeight = 16,
            destinationX = 10,
            destinationY = 10,
            dither = 0xFFFF,
        )

        // Second sprite with different dither should work
        val needsRender = batchManager.submitSprite(
            source = spriteSheet,
            sourceX = 0,
            sourceY = 0,
            sourceWidth = 16,
            sourceHeight = 16,
            destinationX = 20,
            destinationY = 20,
            dither = 0x0000,
        )

        // Should handle different dither values
        assertFalse(needsRender)
        assertEquals(2, batchManager.getActiveBatchCount())
    }
}
