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
import com.github.minigdx.tiny.sound.SilenceWave
import com.github.minigdx.tiny.sound.SineWave
import com.github.minigdx.tiny.sound.Song
import com.github.minigdx.tiny.sound.WaveGenerator
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

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
        override fun spritesheet(sheet: SpriteSheet) = Unit
        override fun level(index: Int): GameLevel? = null
        override fun sound(index: Int): Sound? = null
        override fun script(name: String): GameScript? = null
        override fun note(wave: WaveGenerator) = Unit
        override fun sfx(song: Song) = Unit
    }

    @Test
    fun scoreToSong() {
        val score = """tiny-sfx 2 120 255
    |0101FF 0101FF 
    |0101FF 0101FF
    |1 2 1
        """.trimMargin()

        val song = SfxLib.convertScoreToSong(score)

        assertEquals(120, song.bpm)
        assertEquals(1f, song.volume)
        // patterns by index
        assertEquals(2, song.patterns.size)
        // patterns ordered by usage
        assertEquals(3, song.music.size)

        assertEquals(song.patterns[1]!!.notes.size, 2)
    }

    @Test
    fun convertToNote() {
        val wave = SfxLib.convertToWave("0101FF", 0.1f)
        assertTrue(wave::class == SineWave::class)
        assertEquals(Note.C0, wave.note)
    }

    @Test
    fun convertToNoteWithSilence() {
        val wave = SfxLib.convertToWave("FFFFFF", 0.1f)
        assertTrue(wave::class == SilenceWave::class)
        assertTrue(wave.isSilence)
        assertEquals(0.1f, wave.duration)
    }

    @Test
    fun toTable() {
        val lib = SfxLib(mockResources, false)
        val table = lib.toTable().call(lib.emptyScore().call())
        val r = table["patterns"][1].checktable()!!.keys()
        assertTrue(r.isEmpty())
    }

    @Test
    fun createEmptyScore() {
        val expectedScore = """tiny-sfx 1 120 127
    |
    |1
        """.trimMargin()

        val score = SfxLib(mockResources, false).emptyScore().call()
        assertEquals(expectedScore, score.tojstring())
    }
}
