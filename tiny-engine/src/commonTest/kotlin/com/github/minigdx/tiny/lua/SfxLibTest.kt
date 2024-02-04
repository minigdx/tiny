package com.github.minigdx.tiny.lua

import com.github.minigdx.tiny.engine.GameResourceAccess
import com.github.minigdx.tiny.graphic.ColorPalette
import com.github.minigdx.tiny.graphic.FrameBuffer
import com.github.minigdx.tiny.resources.GameLevel
import com.github.minigdx.tiny.resources.GameScript
import com.github.minigdx.tiny.resources.Sound
import com.github.minigdx.tiny.resources.SpriteSheet
import com.github.minigdx.tiny.sound.SilenceWave
import com.github.minigdx.tiny.sound.SineWave
import com.github.minigdx.tiny.sound.WaveGenerator
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class SfxLibTest {

    private val mockResources = object : GameResourceAccess {
        override val bootSpritesheet: SpriteSheet? = null
        override val frameBuffer: FrameBuffer = FrameBuffer(10, 10, ColorPalette(listOf("#FFFFFF")))
        override fun spritesheet(index: Int): SpriteSheet? = null
        override fun spritesheet(sheet: SpriteSheet) = Unit
        override fun level(index: Int): GameLevel? = null
        override fun sound(index: Int): Sound? = null
        override fun script(name: String): GameScript? = null
        override fun note(wave: WaveGenerator) = Unit
        override fun sfx(waves: List<WaveGenerator>) = Unit
    }

    fun trim(str: String): String {
        val lastIndex = str.lastIndexOf(')')
        if (lastIndex < 0) return str
        return str.substring(0, lastIndex + 2)
    }

    @Test
    fun trimMusic() {
        val str = "*-*-sine(Eb)-*-*-"

        assertEquals("*-*-sine(Eb)-", trim(str))
    }

    @Test
    fun scoreToSong() {
        val score = """tiny-sfx 2 120
    |0101FF:0202FF 0101FF:0202FF 
    |0101FF:0202FF 0101FF:0202FF
    |1 2 1
        """.trimMargin()

        val lib = SfxLib(mockResources)
        val song = lib.convertScoreToSong(score)

        assertEquals(120, song.bpm)
        // patterns by index
        assertEquals(2, song.patterns.size)
        // patterns ordered by usage
        assertEquals(3, song.music.size)
    }

    @Test
    fun convertToNote() {
        val lib = SfxLib(mockResources)
        val wave = lib.convertToWave("0101FF", 0.1f)
        assertTrue(wave::class == SineWave::class)
        assertEquals(Note.C0, wave.note)
    }

    @Test
    fun convertToNoteWithSilence() {
        val lib = SfxLib(mockResources)
        val wave = lib.convertToWave("FFFFFF", 0.1f)
        assertTrue(wave::class == SilenceWave::class)
        assertTrue(wave.isSilence)
        assertEquals(0.1f, wave.duration)
    }
}
