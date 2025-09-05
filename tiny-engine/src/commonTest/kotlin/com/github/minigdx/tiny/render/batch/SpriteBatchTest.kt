package com.github.minigdx.tiny.render.batch

import com.github.minigdx.tiny.graphic.PixelArray
import com.github.minigdx.tiny.resources.ResourceType
import com.github.minigdx.tiny.resources.SpriteSheet

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

    private fun createTestBatchKey(): SpriteBatchKey {
        return SpriteBatchKey(
            dither = 0,
            palette = emptyArray(),
            camera = null,
            clipper = null,
        )
    }
}
