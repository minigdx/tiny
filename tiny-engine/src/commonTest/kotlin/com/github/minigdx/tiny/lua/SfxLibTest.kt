package com.github.minigdx.tiny.lua

import com.github.minigdx.tiny.sound.SilenceWave
import com.github.minigdx.tiny.sound.SineWave
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class SfxLibTest {

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
        val score = """tiny-sfx 2 120 255
    |0101FF:0202FF 0101FF:0202FF 
    |0101FF:0202FF 0101FF:0202FF
    |1 2 1
        """.trimMargin()

        val song = SfxLib.convertScoreToSong(score)

        assertEquals(120, song.bpm)
        assertEquals(1f, song.volume)
        // patterns by index
        assertEquals(2, song.patterns.size)
        // patterns ordered by usage
        assertEquals(3, song.music.size)

        assertEquals(song.patterns[1]!!.beats.size, 2)
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
}
