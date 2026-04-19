package com.github.minigdx.tiny.sound

import kotlinx.serialization.Serializable

/**
 * A musical pattern groups multiple [MusicalPhrase]s (tracks) that play simultaneously.
 *
 * In tracker music terminology, a pattern is a grid of notes across channels.
 * A [MusicalSequence] can reference patterns by index through its arrangement
 * to control playback order (e.g., arrangement [0, 1, 0, 2] plays pattern 0,
 * then 1, then 0 again, then 2).
 *
 * Each track in the pattern is a [MusicalPhrase] — the same type used for
 * standalone SFX. When played as part of a pattern, the phrase's own tempo
 * is ignored in favor of the [MusicalSequence]'s tempo.
 */
@Serializable
class MusicalPattern(
    val index: Int = 0,
    val tracks: Array<MusicalPhrase> = Array(4) { channelIndex ->
        MusicalPhrase(index = channelIndex, instrumentIndex = 0).also { phrase ->
            (0..32).forEach { beat ->
                phrase.addBeat(MusicalNote(null, beat.toFloat(), 1f, 1f))
            }
        }
    },
)
