package com.github.minigdx.tiny.lua

import com.github.minigdx.tiny.engine.GameResourceAccess
import com.github.minigdx.tiny.graphic.ColorPalette
import com.github.minigdx.tiny.graphic.FrameBuffer
import com.github.minigdx.tiny.graphic.PixelArray
import com.github.minigdx.tiny.graphic.PixelFormat
import com.github.minigdx.tiny.resources.GameLevel
import com.github.minigdx.tiny.resources.GameScript
import com.github.minigdx.tiny.resources.ResourceType
import com.github.minigdx.tiny.resources.Sound
import com.github.minigdx.tiny.resources.SpriteSheet
import com.github.minigdx.tiny.sound.Song2
import com.github.minigdx.tiny.sound.WaveGenerator
import kotlin.test.Test
import kotlin.test.assertEquals

class SfxLibTest {

    private val mockResources = object : GameResourceAccess {
        override val bootSpritesheet: SpriteSheet = SpriteSheet(
            0,
            0,
            "boot",
            ResourceType.BOOT_SPRITESHEET,
            PixelArray(1, 1, PixelFormat.INDEX),
            1,
            1,
        )
        override val frameBuffer: FrameBuffer = FrameBuffer(10, 10, ColorPalette(emptyList()))
        override fun spritesheet(index: Int): SpriteSheet? = null
        override fun spritesheet(name: String): Int? = null

        override fun spritesheet(sheet: SpriteSheet) = Unit
        override fun newSpritesheetIndex(): Int = 0

        override fun level(index: Int): GameLevel? = null
        override fun sound(index: Int): Sound? = null
        override fun script(name: String): GameScript? = null
        override fun note(wave: WaveGenerator) = Unit
        override fun sfx(song: Song2) = Unit
    }

    @Test
    fun scoreToSong2() {
        val score = """tiny-sfx 120 255
            |02 00 00 00 00 00 00 00 00 00 00
    |0101FF 0101FF 
    |0101FF 0101FF
    |1 2 1
    |00 00 00 00 00 00 00 00 00 00 00
    |00 00 00 00 00 00 00 00 00 00 00
    |00 00 00 00 00 00 00 00 00 00 00
        """.trimMargin()

        val song = SfxLib.convertScoreToSong2(score)

        assertEquals(120, song.bpm)
        assertEquals(1f, song.volume)
        assertEquals(4, song.tracks.size)
        assertEquals(2, song.tracks[0].patterns.size)
        // patterns by index
        assertEquals(2, song.tracks[0].patterns[1]!!.notes.size)
        // patterns ordered by usage
        assertEquals(3, song.tracks[0].music.size)
    }

    @Test
    fun toTable() {
        val lib = SfxLib(mockResources, false)
        val table = lib.toTable().call(lib.emptyScore().call())
        val r = table["tracks"][1]["patterns"][1].checktable()!!.keys()
        assertEquals(16, r.size)
    }

    @Test
    fun toScore() {
        val lib = SfxLib(mockResources, false)
        val score = lib.toScore().call(lib.toTable().call(lib.emptyScore().call()))
        val expectedScore = """tiny-sfx 120 127
    |1 01 19 00 FF 19 00 00 00 00 00
    |000100 000100 000100 000100 000100 000100 000100 000100 000100 000100 000100 000100 000100 000100 000100 000100
    |1
    |0 01 19 00 FF 19 00 00 00 00 00
    |0 01 19 00 FF 19 00 00 00 00 00
    |0 01 19 00 FF 19 00 00 00 00 00
        """.trimMargin()

        assertEquals(expectedScore, score.tojstring())
    }

    @Test
    fun createEmptyScore() {
        val expectedScore = """tiny-sfx 120 127
    |1 01 19 00 FF 19 00 00 00 00 00
    |000100 000100 000100 000100 000100 000100 000100 000100 000100 000100 000100 000100 000100 000100 000100 000100
    |1
    |0 01 19 00 FF 19 00 00 00 00 00
    |0 01 19 00 FF 19 00 00 00 00 00
    |0 01 19 00 FF 19 00 00 00 00 00
        """.trimMargin()

        val score = SfxLib(mockResources, false).emptyScore().call()
        assertEquals(expectedScore, score.tojstring())
    }
}
