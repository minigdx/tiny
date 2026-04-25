package com.github.minigdx.tiny.sound

import com.github.minigdx.tiny.BPM
import kotlinx.serialization.Serializable
import kotlin.math.roundToInt

/**
 * A musical sequence represents a playable piece of music.
 *
 * It holds [patterns] — each pattern is a group of 4 tracks ([MusicalPhrase]s)
 * that play simultaneously. The [arrangement] defines which patterns to play
 * and in what order (e.g., [0, 1, 0, 2] plays pattern 0, then 1, then 0 again,
 * then 2, then loops or stops).
 *
 * [loopFrom] controls where playback restarts when looping:
 * - 0 (default): loop from the beginning of the arrangement
 * - positive value: loop from that position in the arrangement
 *   (e.g., loopFrom=2 with arrangement [0,1,2,3] plays all four patterns,
 *   then loops back to pattern at arrangement position 2)
 * - -1: do not loop (play once and stop)
 */
@Serializable
class MusicalSequence(
    val index: Int,
    var patterns: Array<MusicalPattern> = arrayOf(MusicalPattern(0)),
    var tempo: BPM = 120,
    var name: String? = null,
    /**
     * Index of the pattern in which order the sequence is played.
     */
    var arrangement: List<Int> = listOf(0),
    /**
     * After the last pattern of the arrangement is played,
     * when the sequence is looped, it will start back at [loopFrom] index.
     */
    var loopFrom: Int = -1,
) {
    /**
     * Number of beats per pattern at arrangement position [position],
     * defined as the max beat count across the pattern's tracks.
     * Returns 0 when the position or referenced pattern is missing.
     */
    private fun beatsAt(position: Int): Int {
        val patternIndex = arrangement.getOrNull(position) ?: return 0
        val pattern = patterns.getOrNull(patternIndex) ?: return 0
        return pattern.tracks.maxOfOrNull { it.beats.size } ?: 0
    }

    /**
     * Total number of beats played across the full arrangement.
     */
    val totalBeats: Int
        get() = arrangement.indices.sumOf { beatsAt(it) }

    /**
     * Compute the sample index where looping should restart.
     *
     * Patterns in the arrangement play sequentially, so the sample offset is the
     * sum of rendered samples for arrangement positions before [loopFrom].
     *
     * Returns 0 if loopFrom <= 0 (loop from start).
     */
    val loopStartSample: Int
        get() {
            if (loopFrom <= 0) return 0
            val secondsPerBeat = 60f / tempo
            val upTo = loopFrom.coerceAtMost(arrangement.size)
            var samples = 0
            for (i in 0 until upTo) {
                val beats = beatsAt(i)
                if (beats > 0) {
                    samples += (beats * secondsPerBeat * SoundManager.SAMPLE_RATE).roundToInt()
                }
            }
            return samples
        }
}
