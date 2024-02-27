package com.github.minigdx.tiny.sound

import com.github.minigdx.tiny.Seconds
import com.github.minigdx.tiny.sound.SoundManager.Companion.SAMPLE_RATE

/**
 * A pattern is a part of a song. A song is composed of multiple pattern played in a specific order.
 */
data class Pattern(val index: Int, val notes: List<WaveGenerator>)
data class Pattern2(val index: Int, val notes: List<SoundGenerator>)

/**
 * A song is a group of pattern.
 */
data class Song(val bpm: Int, val volume: Float, val patterns: Map<Int, Pattern>, val music: List<Pattern>) {

    val durationOfBeat: Seconds = (60f / bpm / 8f)

    val numberOfBeats = music.count() * 32

    override fun toString(): String {
        val header = "tiny-sfx ${patterns.size} $bpm ${(volume * 255).toInt()}\n"
        val patternsInOrder = patterns.map { it }.sortedBy { it.key }.map { it.value }
        val patternsStr = patternsInOrder.joinToString("\n") { pattern ->
            pattern.notes.joinToString(" ") { wave ->
                wave.index.toString(16) +
                    wave.note.index.toString(16) +
                    (wave.volume * 255).toInt().toString(16)
            }
        }.ifBlank { "\n" }
        val patternOrder = music.map { it.index }.joinToString(" ")
        return header + patternsStr + patternOrder
    }
}

data class Track(
    val patterns: Map<Int, Pattern2>,
    val music: List<Pattern2>,
    val beatDuration: Seconds,
    val envelope: Envelope? = null,
    val modulation: Modulation? = null,
) {

    private val samplePerBeat = (beatDuration * SAMPLE_RATE).toInt()
    private val samplePerPattern = samplePerBeat * 32

    fun getSample(index: Int): Float {
        val position = getPosition(index) ?: return 0f

        val (patternIdx, beatIdx, sampleIdx) = position
        val pattern = music[patternIdx]

        val sample = pattern.notes[beatIdx].generate(sampleIdx, samplePerBeat)
        return sample
    }

    /**
     * Regarding an absolute index of a sample, get the relative position into the track.
     */
    fun getPosition(index: Int): Triple<Int, Int, Int>? {
        val patternIndex = index / samplePerPattern
        if (patternIndex >= music.size) return null

        val beatIndex = (index - patternIndex * samplePerPattern) / samplePerBeat
        val pattern = music[patternIndex]

        if (beatIndex >= pattern.notes.size) return null

        val sampleIndex = (index - patternIndex * samplePerPattern - beatIndex * samplePerBeat)

        return Triple(patternIndex, beatIndex, sampleIndex)
    }
}

class Song2(
    val bpm: Int,
    val volume: Float,
    val tracks: Array<Track>,
) {
    val durationOfBeat: Seconds = (60f / bpm / 8f)

    val numberOfBeats = tracks.maxOf { it.music.size } * 32

    val numberOfTotalSample = (durationOfBeat * SAMPLE_RATE * numberOfBeats).toLong()
}
