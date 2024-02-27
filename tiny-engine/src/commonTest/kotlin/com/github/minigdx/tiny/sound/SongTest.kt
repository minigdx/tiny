package com.github.minigdx.tiny.sound

import com.github.minigdx.tiny.sound.SoundManager.Companion.SAMPLE_RATE
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

class SongTest {

    @Test
    fun getPosition() {
        val pattern = Pattern2(1, listOf(Sine2(0f, null, null)))
        val track = Track(mapOf(1 to pattern), listOf(pattern, pattern, pattern), 1f, null, null)

        val position = track.getPosition(0)
        assertNotNull(position)
        val (patternIdx, beatIdx, sample) = position
        assertEquals(0, patternIdx)
        assertEquals(0, beatIdx)
        assertEquals(0, sample)
    }

    @Test
    fun getPositionOutOfTrack() {
        val pattern = Pattern2(1, listOf(Sine2(0f, null, null)))
        val track = Track(mapOf(1 to pattern), listOf(pattern, pattern, pattern), 1f, null, null)

        val position = track.getPosition(Int.MAX_VALUE)
        assertNull(position)
    }

    @Test
    fun getPositionInSecondPattern() {
        val pattern = Pattern2(1, listOf(Sine2(0f, null, null), Sine2(0f, null, null)))
        val track = Track(mapOf(1 to pattern), listOf(pattern, pattern, pattern), 1f, null, null)

        // 1 beats on the seconds pattern.
        val index = (33.beats * track.beatDuration * SAMPLE_RATE).toInt()
        val position = track.getPosition(index)
        assertNotNull(position)

        val (patternIdx, beatIdx, sample) = position

        assertEquals(1, patternIdx)
        assertEquals(1, beatIdx)
        assertEquals(0, sample)
    }

    val Int.beats: Int
        get() {
            return this
        }
}
