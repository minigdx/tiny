package com.github.minigdx.tiny.sound

import com.github.minigdx.tiny.Seconds
import com.github.minigdx.tiny.lua.Note
import com.github.minigdx.tiny.sound.SoundManager.Companion.SAMPLE_RATE

/**
 * A pattern is a part of a song. A song is composed of multiple pattern played in a specific order.
 */
data class Pattern2(val index: Int, val notes: List<SoundGenerator>)

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

    private fun Float.toHex(): String {
        val r = (this * 255).toInt().toString(16)
        return if (r.length == 1) {
            "0$r"
        } else {
            r
        }.uppercase()
    }

    private fun Int.toHex(): String {
        val r = toString(16)
        return if (r.length == 1) {
            "0$r"
        } else {
            r
        }.uppercase()
    }

    private fun Boolean.toHex(): String {
        return if (this) 255.toHex() else 0.toHex()
    }

    override fun toString(): String {
        val header = "tiny-sfx $bpm ${(volume * 255).toInt()}\n"

        val tracks = tracks.map { track ->

            var trackHeader = "${track.patterns.size} "
            trackHeader += if (track.envelope == null) {
                "00 00 00 00 00"
            } else {
                "01 " +
                    "${track.envelope.attack.toHex()} " +
                    "${track.envelope.decay.toHex()} " +
                    "${track.envelope.sustain.toHex()} " +
                    "${track.envelope.release.toHex()} "
            }

            trackHeader += when (track.modulation) {
                is Sweep -> "01 ${Note.fromFrequency(track.modulation.sweep).index.toHex()} ${track.modulation.acceleration.toHex()} 00 00"
                is Vibrato -> "02 ${Note.fromFrequency(track.modulation.vibratoFrequency).index.toHex()} ${track.modulation.depth.toHex()} 00 00"
                else -> "00 00 00 00 00"
            }

            val patternsInOrder = track.patterns.map { it }.sortedBy { it.key }.map { it.value }
            val patternsStr = patternsInOrder.joinToString("\n") { pattern ->
                pattern.notes.joinToString(" ") { wave ->
                    wave.index.toHex() +
                        wave.note.index.toHex() +
                        wave.volume.toHex()
                }
            }

            val patternOrder = track.music.map { it.index }.joinToString(" ")
            trackHeader + ("\n" + patternsStr + "\n" + patternOrder).ifBlank { "" }
        }.joinToString("\n")
        return header + tracks
    }
}
