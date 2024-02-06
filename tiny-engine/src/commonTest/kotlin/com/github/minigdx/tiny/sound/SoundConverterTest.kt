package com.github.minigdx.tiny.sound

import com.github.minigdx.tiny.lua.Note
import kotlin.test.Test
import kotlin.test.assertEquals

class SoundConverterTest {

    @Test
    fun createStrip() {
        val result = SoundConverter().createStrip(
            5,
            arrayOf(
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
        val song = Song(120, 1f, mapOf(pattern.index to pattern), listOf(pattern, pattern))

        val (lastBeat, result) = SoundConverter().prepareStrip(song)

        assertEquals(1, lastBeat)
        assertEquals(2, result.size)
        assertEquals(65, result[sine.name]!!.size)
        assertEquals(65, result[pulse.name]!!.size)
    }

    @Test
    fun prepareStripWithSilence() {
        val sine = SineWave(Note.C0, 0.1f)
        val silence = SilenceWave(0.1f)
        val pulse = PulseWave(Note.C0, 0.1f)
        val pattern = Pattern(
            1,
            listOf(
                Beat(1, listOf(sine)),
                Beat(2, listOf(silence)),
                Beat(3, listOf(pulse)),
            ),
        )
        val song = Song(120, 1f, mapOf(pattern.index to pattern), listOf(pattern, pattern))

        val (lastBeat, result) = SoundConverter().prepareStrip(song)

        assertEquals(6, lastBeat)
        assertEquals(2, result.size)
        assertEquals(65, result[sine.name]!!.size)
        assertEquals(65, result[pulse.name]!!.size)
    }
}
