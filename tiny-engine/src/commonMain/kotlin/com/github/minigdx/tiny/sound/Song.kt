package com.github.minigdx.tiny.sound

import com.github.minigdx.tiny.Seconds

/**
 * A pattern is a part of a song. A song is composed of multiple pattern played in a specific order.
 */
data class Pattern(val index: Int, val notes: List<WaveGenerator>)

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
