package com.github.minigdx.tiny.sound

import com.github.minigdx.tiny.Seconds

/**
 * A beat is the representation of one moment in a music. It can be composed of multiple notes of multiple
 * kind of wave.
 */
data class Beat(val index: Int, val notes: List<WaveGenerator>)

/**
 * A pattern is a part of a song. A song is compose of multiple pattern played in a specific order.
 */
data class Pattern(val index: Int, val beats: List<Beat>)

/**
 * A song is a group of pattern.
 */
data class Song(val bpm: Int, val volume: Float, val patterns: Map<Int, Pattern>, val music: List<Pattern>) {

    val durationOfBeat: Seconds = (60f / bpm / 8f)

    val numberOfBeats = music.count() * 32
}
