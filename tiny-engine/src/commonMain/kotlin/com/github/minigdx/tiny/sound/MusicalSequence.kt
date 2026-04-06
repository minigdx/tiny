package com.github.minigdx.tiny.sound

import com.github.minigdx.tiny.BPM
import kotlinx.serialization.Serializable

/**
 * A musical sequence represents a playable piece of music.
 *
 * It holds [patterns] — each pattern is a group of 4 tracks ([MusicalPhrase]s)
 * that play simultaneously. The [arrangement] defines which patterns to play
 * and in what order (e.g., [0, 1, 0, 2] plays pattern 0, then 1, then 0 again,
 * then 2, then loops or stops).
 */
@Serializable
class MusicalSequence(
    val index: Int,
    val patterns: Array<MusicalPattern> = arrayOf(MusicalPattern(0)),
    var tempo: BPM = 120,
    var name: String? = null,
    val arrangement: List<Int> = listOf(0),
)
