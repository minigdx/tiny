package com.github.minigdx.tiny.sound

import com.github.minigdx.tiny.lua.Note
import kotlin.test.Test
import kotlin.test.assertEquals

class SoundConverterTest {

    @Test
    fun createStrip() {
        val result = SoundConverter().createStrip(
            1f,
            5,
            arrayOf(
                PulseWave(Note.A4, 0.1f),
                PulseWave(Note.A4, 0.1f),
                SilenceWave(0.1f),
            ),
        )

        assertEquals(15, result.samples.size)
    }
}
