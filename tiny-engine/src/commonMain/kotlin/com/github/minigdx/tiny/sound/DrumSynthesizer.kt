package com.github.minigdx.tiny.sound

import kotlin.math.PI
import kotlin.math.exp
import kotlin.math.ln
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.random.Random

/**
 * Stateless drum synthesizer that generates different drum sounds
 * based on the pitch class derived from the input frequency.
 *
 * Note-to-drum mapping:
 * - C (pitch class 0) = bass drum
 * - D (pitch class 2) = snare
 * - E (pitch class 4) = hi-hat closed
 * - F (pitch class 5) = hi-hat open
 * - G (pitch class 7) = crash cymbal
 * - A (pitch class 9) = tom 1
 * - B (pitch class 11) = tom 2
 *
 * Sharps/flats map to the nearest natural note.
 */
object DrumSynthesizer {
    private const val TWO_PI = PI.toFloat() * 2f
    private const val C0_FREQ = 16.3516f

    /**
     * Determines which drum part to play from the pitch class of [freq],
     * then synthesises one sample at the given [time] (in seconds).
     *
     * @param freq  The note frequency in Hz (used to detect pitch class).
     * @param time  Elapsed time in seconds since note-on.
     * @param random A [Random] instance for noise generation.
     * @return A sample value clamped to [-1, 1].
     */
    fun generate(
        freq: Float,
        time: Float,
        random: Random,
    ): Float {
        val drumPart = pitchClassToDrum(freq)
        return when (drumPart) {
            DrumPart.BASS -> bassDrum(time)
            DrumPart.SNARE -> snare(time, random)
            DrumPart.HIHAT_CLOSED -> hihatClosed(time, random)
            DrumPart.HIHAT_OPEN -> hihatOpen(time, random)
            DrumPart.CRASH -> crash(time, random)
            DrumPart.TOM1 -> tom1(time)
            DrumPart.TOM2 -> tom2(time)
        }.coerceIn(-1f, 1f)
    }

    private enum class DrumPart {
        BASS,
        SNARE,
        HIHAT_CLOSED,
        HIHAT_OPEN,
        CRASH,
        TOM1,
        TOM2,
    }

    /**
     * Derives the pitch class (0..11) from a frequency and maps it
     * to the nearest natural note drum part.
     */
    private fun pitchClassToDrum(freq: Float): DrumPart {
        if (freq <= 0f) return DrumPart.BASS
        val semitones = 12f * (ln(freq / C0_FREQ) / ln(2f))
        val pitchClass = ((semitones.roundToInt() % 12) + 12) % 12
        return when (pitchClass) {
            0, 1 -> DrumPart.BASS // C, C#
            2, 3 -> DrumPart.SNARE // D, D#
            4 -> DrumPart.HIHAT_CLOSED // E
            5, 6 -> DrumPart.HIHAT_OPEN // F, F#
            7, 8 -> DrumPart.CRASH // G, G#
            9, 10 -> DrumPart.TOM1 // A, A#
            11 -> DrumPart.TOM2 // B
            else -> DrumPart.BASS
        }
    }

    // ---- Individual drum synthesis functions ----

    /** Bass drum: sine with pitch sweep 150->50 Hz + click transient, fast decay. */
    private fun bassDrum(time: Float): Float {
        val decay = exp(-time * 15f)
        val sweepFreq = 50f + 100f * exp(-time * 40f)
        val body = sin(TWO_PI * sweepFreq * time) * decay
        val click = exp(-time * 200f) * 0.8f
        return body + click
    }

    /** Snare: 30% sine body at 180 Hz + 70% white noise, medium-fast decay. */
    private fun snare(
        time: Float,
        random: Random,
    ): Float {
        val bodyDecay = exp(-time * 20f)
        val noiseDecay = exp(-time * 15f)
        val body = sin(TWO_PI * 180f * time) * bodyDecay * 0.3f
        val noise = (random.nextFloat() * 2f - 1f) * noiseDecay * 0.7f
        return body + noise
    }

    /** Hi-hat closed: metallic inharmonic tones + noise, very short decay (~17ms). */
    private fun hihatClosed(
        time: Float,
        random: Random,
    ): Float {
        val decay = exp(-time * 60f) // ~17ms effective duration
        val metallic = sin(TWO_PI * 3527f * time) * sin(TWO_PI * 4735f * time)
        val noise = random.nextFloat() * 2f - 1f
        return (metallic * 0.6f + noise * 0.4f) * decay
    }

    /** Hi-hat open: same metallic character, longer decay (~125ms). */
    private fun hihatOpen(
        time: Float,
        random: Random,
    ): Float {
        val decay = exp(-time * 8f) // ~125ms effective duration
        val metallic = sin(TWO_PI * 3527f * time) * sin(TWO_PI * 4735f * time)
        val noise = random.nextFloat() * 2f - 1f
        return (metallic * 0.6f + noise * 0.4f) * decay
    }

    /** Crash cymbal: three metallic frequencies + dominant noise, long decay. */
    private fun crash(
        time: Float,
        random: Random,
    ): Float {
        val decay = exp(-time * 3f)
        val m1 = sin(TWO_PI * 4200f * time)
        val m2 = sin(TWO_PI * 5386f * time)
        val m3 = sin(TWO_PI * 3750f * time)
        val metallic = (m1 + m2 + m3) / 3f
        val noise = random.nextFloat() * 2f - 1f
        return (metallic * 0.3f + noise * 0.7f) * decay
    }

    /** Tom 1: sine with pitch sweep 200->120 Hz, medium decay. */
    private fun tom1(time: Float): Float {
        val decay = exp(-time * 10f)
        val sweepFreq = 120f + 80f * exp(-time * 25f)
        return sin(TWO_PI * sweepFreq * time) * decay
    }

    /** Tom 2: sine with pitch sweep 150->80 Hz, slightly longer decay. */
    private fun tom2(time: Float): Float {
        val decay = exp(-time * 8f)
        val sweepFreq = 80f + 70f * exp(-time * 20f)
        return sin(TWO_PI * sweepFreq * time) * decay
    }
}
