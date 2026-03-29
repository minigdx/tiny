package com.github.minigdx.tiny.engine

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.test.assertTrue

class GameConfigTest {
    @Test
    fun parse_v1_config_with_all_fields() {
        val json = """
        {
            "version": "V1",
            "name": "my-game",
            "id": "game-123",
            "resolution": { "width": 256, "height": 256 },
            "sprites": { "width": 16, "height": 16 },
            "zoom": 2,
            "colors": ["#000000", "#FFFFFF", "#FF0000"],
            "scripts": ["game.lua", "utils.lua"],
            "spritesheets": ["sprites.png"],
            "levels": ["level1.ldtk"],
            "sound": "sfx.json",
            "hideMouseCursor": true
        }
        """.trimIndent()

        val config = GameConfig.parse(json)

        assertIs<GameConfigV1>(config)
        assertEquals("my-game", config.name)
        assertEquals("game-123", config.id)
        assertEquals(256, config.resolution.width)
        assertEquals(256, config.resolution.height)
        assertEquals(16, config.sprites.width)
        assertEquals(16, config.sprites.height)
        assertEquals(2, config.zoom)
        assertEquals(listOf("#000000", "#FFFFFF", "#FF0000"), config.colors)
        assertEquals(listOf("game.lua", "utils.lua"), config.scripts)
        assertEquals(listOf("sprites.png"), config.spritesheets)
        assertEquals(listOf("level1.ldtk"), config.levels)
        assertEquals("sfx.json", config.sound)
        assertTrue(config.hideMouseCursor)
    }

    @Test
    fun parse_v1_config_with_defaults() {
        val json = """
        {
            "version": "V1",
            "name": "minimal",
            "id": "min-1",
            "resolution": { "width": 128, "height": 128 },
            "sprites": { "width": 8, "height": 8 },
            "zoom": 1,
            "colors": ["#FFFFFF", "#000000"]
        }
        """.trimIndent()

        val config = GameConfig.parse(json)

        assertIs<GameConfigV1>(config)
        assertEquals(emptyList(), config.scripts)
        assertEquals(emptyList(), config.spritesheets)
        assertEquals(emptyList(), config.levels)
        assertNull(config.sound)
        assertEquals(false, config.hideMouseCursor)
    }

    @Test
    fun toGameOptions_maps_all_fields() {
        val config = GameConfigV1(
            name = "test",
            id = "t-1",
            resolution = GameConfigSize(width = 256, height = 128),
            sprites = GameConfigSize(width = 16, height = 16),
            zoom = 3,
            colors = listOf("#FF0000"),
            scripts = listOf("game.lua"),
            spritesheets = listOf("spr.png"),
            levels = listOf("lvl.ldtk"),
            sound = "sfx.json",
            hideMouseCursor = true,
        )

        val options = config.toGameOptions()

        assertEquals(256, options.width)
        assertEquals(128, options.height)
        assertEquals(listOf("#FF0000"), options.palette)
        assertEquals(16 to 16, options.spriteSize)
        assertEquals(listOf("game.lua"), options.gameScripts)
        assertEquals(listOf("spr.png"), options.spriteSheets)
        assertEquals(listOf("lvl.ldtk"), options.gameLevels)
        assertEquals(3, options.zoom)
        assertEquals("sfx.json", options.sound)
        assertTrue(options.hideMouseCursor)
    }

    @Test
    fun parse_ignores_unknown_keys() {
        val json = """
        {
            "version": "V1",
            "name": "test",
            "id": "t-1",
            "resolution": { "width": 128, "height": 128 },
            "sprites": { "width": 8, "height": 8 },
            "zoom": 1,
            "colors": ["#FFFFFF"],
            "unknownField": "should be ignored",
            "anotherUnknown": 42
        }
        """.trimIndent()

        val config = GameConfig.parse(json)
        assertIs<GameConfigV1>(config)
        assertEquals("test", config.name)
    }
}
