package com.github.minigdx.tiny.sound

import com.github.minigdx.tiny.lua.Note
import kotlin.test.Test
import kotlin.test.assertEquals

class SoundConverterTest {

    @Test
    fun createStrip() {
        val result = SoundConverter().createStrip(
            5,
            arrayListOf(
                PulseWave(Note.A4, 0.1f),
                PulseWave(Note.A4, 0.1f),
                SilenceWave(0.1f),
            ),
        )

        assertEquals(15, result.size)
    }

    @Test
    fun prepareStrip() {
        val sine = SineWave(Note.C0, 0.1f)
        val pulse = PulseWave(Note.C0, 0.1f)
        val pattern = Pattern(1, listOf(Beat(1, listOf(sine, pulse))))
        val song = Song(120, mapOf(pattern.index to pattern), listOf(pattern, pattern))

        val result = SoundConverter().prepateStrip(song)

        assertEquals(2, result.size)
        assertEquals(65, result[sine.name]!!.size)
        assertEquals(65, result[pulse.name]!!.size)
    }
}
