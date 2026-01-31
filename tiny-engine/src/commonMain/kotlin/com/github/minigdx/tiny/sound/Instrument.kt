package com.github.minigdx.tiny.sound

import com.github.minigdx.tiny.Percent
import com.github.minigdx.tiny.Seconds
import com.github.minigdx.tiny.lua.Note
import com.github.minigdx.tiny.sound.Instrument.WaveType.DRUM
import com.github.minigdx.tiny.sound.Instrument.WaveType.NOISE
import com.github.minigdx.tiny.sound.Instrument.WaveType.PULSE
import com.github.minigdx.tiny.sound.Instrument.WaveType.SAW_TOOTH
import com.github.minigdx.tiny.sound.Instrument.WaveType.SINE
import com.github.minigdx.tiny.sound.Instrument.WaveType.SQUARE
import com.github.minigdx.tiny.sound.Instrument.WaveType.TRIANGLE
import com.github.minigdx.tiny.sound.SoundManager.Companion.SAMPLE_RATE
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.exp
import kotlin.math.ln
import kotlin.math.max
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.random.Random

/**
 * An instrument holds the specific of the sound generation
 */
@Serializable
class Instrument(
    /**
     * Index of the instrument
     */
    val index: Int,
    /**
     * Name of the instrument
     */
    var name: String? = "Instrument $index",
    /**
     * Kind of wave assigned to this instrument
     */
    var wave: WaveType = WaveType.SINE,
    /**
     * Attack of the sound envelope
     */
    var attack: Seconds = 0f,
    /**
     * Decay of the sound envelope
     */
    var decay: Seconds = 0f,
    /**
     * Sustain of the sound envelope
     */
    var sustain: Percent = 0f,
    /**
     * Release of the sound envelope
     */
    var release: Seconds = 0f,
    /**
     * Harmonics of the instruments (up to 7)
     */
    val harmonics: FloatArray = FloatArray(NUMBER_OF_HARMONICS),
    /**
     * Modulation effects.
     * Will be applied in the order configured.
     */
    val modulations: List<Modulation> = listOf(
        Sweep(Note.A5.frequency, 1f),
        Vibrato(0f, 0f),
    ),
) {
    enum class WaveType {
        SAW_TOOTH,
        PULSE,
        TRIANGLE,
        SINE,
        NOISE,
        SQUARE,
        /**
         * Drum machine synthesis.
         * The sound changes based on the note played (repeating every octave):
         * - C (Do) = Kick drum (bass drum)
         * - D (Ré) = Snare drum
         * - E (Mi) = Closed hi-hat
         * - F (Fa) = Open hi-hat
         * Other notes will produce silence.
         */
        DRUM,
    }

    /**
     * Drum sound type based on semitone (0-11).
     */
    private enum class DrumSound {
        KICK,      // C = 0
        SNARE,     // D = 2
        HIHAT_CLOSED, // E = 4
        HIHAT_OPEN,   // F = 5
        NONE
    }

    /**
     * Convert frequency to semitone (0-11) within an octave.
     * C=0, C#=1, D=2, D#=3, E=4, F=5, F#=6, G=7, G#=8, A=9, A#=10, B=11
     */
    private fun frequencyToSemitone(freq: Float): Int {
        if (freq <= 0f) return 0
        // MIDI note number formula: 12 * log2(freq / 440) + 69
        val noteNumber = (12f * ln(freq / 440f) / LN_2 + 69f).roundToInt()
        // Get semitone within octave (0-11)
        return ((noteNumber % 12) + 12) % 12
    }

    /**
     * Get drum sound type from frequency.
     */
    private fun getDrumSound(freq: Float): DrumSound {
        return when (frequencyToSemitone(freq)) {
            0 -> DrumSound.KICK        // C
            2 -> DrumSound.SNARE       // D
            4 -> DrumSound.HIHAT_CLOSED // E
            5 -> DrumSound.HIHAT_OPEN   // F
            else -> DrumSound.NONE
        }
    }

    // Last output generated. Used by the [NOISE] wave type
    @Transient
    private var lastOutput: Float = 0.0f

    @Transient
    private var lastFrequencyUsed: Float = 0.0f

    @Transient
    private var cachedAlpha: Float = 0.0f

    // State for drum sounds that use noise
    @Transient
    private var drumNoiseOutput: Float = 0.0f

    @Transient
    private var drumNoiseAlpha: Float = 0.0f

    fun generate(
        freq: Float,
        time: Float,
    ): Float {
        // Apply modulation to the base frequency
        val harmonicFreq = modulations.filter { it.active }
            .fold(freq) { acc, modulation -> modulation.apply(time, acc) }

        return when (this.wave) {
            TRIANGLE -> {
                val angle: Float = sin(TWO_PI * harmonicFreq * time)
                val phase = (angle + 1.0) % 1.0 // Normalize sinValue to the range [0, 1]
                return (if (phase < 0.5) 4.0 * phase - 1.0 else 3.0 - 4.0 * phase).toFloat()
            }

            SINE -> sin(TWO_PI * harmonicFreq * time)
            SQUARE -> {
                val value = sin(TWO_PI * harmonicFreq * time)
                return if (value > 0f) {
                    1f
                } else {
                    -1f
                }
            }

            PULSE -> {
                val angle = sin(TWO_PI * harmonicFreq * time)

                val t = angle % 1
                val k = abs(2.0 * ((angle / 128.0) % 1.0) - 1.0)
                val u = (t + 0.5 * k) % 1.0
                val ret = abs(4.0 * u - 2.0) - abs(8.0 * t - 4.0)
                return (ret / 6.0).toFloat()
            }

            SAW_TOOTH -> {
                val angle: Float = sin(TWO_PI * harmonicFreq * time)
                return (angle * 2f) - 1f
            }

            NOISE -> {
                val alpha =
                    if (lastFrequencyUsed == harmonicFreq) {
                        cachedAlpha
                    } else {
                        val safeCutoff = max(1f, harmonicFreq)
                        val wc = TWO_PI * safeCutoff / SAMPLE_RATE
                        val x = exp(-wc)
                        // Cache values
                        cachedAlpha = 1.0f - x
                        lastFrequencyUsed = harmonicFreq

                        cachedAlpha
                    }
                val white = Random.nextFloat() * 2f - 1f
                val result = alpha * white + (1.0f - alpha) * lastOutput
                lastOutput = result
                return result
            }

            DRUM -> {
                // Determine which drum sound to play based on the note
                when (getDrumSound(freq)) {
                    DrumSound.KICK -> {
                        // Kick drum: sine wave with rapid pitch decay
                        val pitchDecayRate = 40f
                        val minPitchRatio = 0.3f
                        val kickFreq = 80f // Fixed kick frequency for consistent sound
                        val pitchEnvelope = minPitchRatio + (1f - minPitchRatio) * exp(-pitchDecayRate * time)
                        val currentFreq = kickFreq * pitchEnvelope

                        val body = sin(TWO_PI * currentFreq * time)
                        val clickAmount = exp(-200f * time) * 0.3f
                        val click = sin(TWO_PI * kickFreq * 3f * time) * clickAmount

                        return body + click
                    }
                    DrumSound.SNARE -> {
                        // Snare drum: pitched body + noise for snare wires
                        val snareFreq = 180f // Fixed snare body frequency
                        val bodyDecay = exp(-20f * time)
                        val body = sin(TWO_PI * snareFreq * time) * bodyDecay * 0.6f

                        val noiseFreq = 4000f
                        val wc = TWO_PI * noiseFreq / SAMPLE_RATE
                        val alpha = 1.0f - exp(-wc)
                        val white = Random.nextFloat() * 2f - 1f
                        drumNoiseOutput = alpha * white + (1.0f - alpha) * drumNoiseOutput
                        val noise = drumNoiseOutput * 0.8f

                        return body + noise
                    }
                    DrumSound.HIHAT_CLOSED -> {
                        // Closed hi-hat: high-frequency filtered noise
                        val cutoff = 8000f
                        val wc = TWO_PI * cutoff / SAMPLE_RATE
                        val alpha = 1.0f - exp(-wc)

                        val white = Random.nextFloat() * 2f - 1f
                        drumNoiseOutput = alpha * white + (1.0f - alpha) * drumNoiseOutput

                        val metallic1 = sin(TWO_PI * 6500f * time) * 0.1f
                        val metallic2 = sin(TWO_PI * 8200f * time) * 0.08f
                        val metallic3 = sin(TWO_PI * 11000f * time) * 0.05f

                        return drumNoiseOutput * 0.7f + metallic1 + metallic2 + metallic3
                    }
                    DrumSound.HIHAT_OPEN -> {
                        // Open hi-hat: similar to closed but fuller
                        val cutoff = 6000f
                        val wc = TWO_PI * cutoff / SAMPLE_RATE
                        val alpha = 1.0f - exp(-wc)

                        val white = Random.nextFloat() * 2f - 1f
                        drumNoiseOutput = alpha * white + (1.0f - alpha) * drumNoiseOutput

                        val metallic1 = sin(TWO_PI * 5500f * time) * 0.15f
                        val metallic2 = sin(TWO_PI * 7800f * time) * 0.12f
                        val metallic3 = sin(TWO_PI * 10500f * time) * 0.08f
                        val metallic4 = sin(TWO_PI * 13000f * time) * 0.04f

                        return drumNoiseOutput * 0.6f + metallic1 + metallic2 + metallic3 + metallic4
                    }
                    DrumSound.NONE -> {
                        return 0f
                    }
                }
            }
        }
    }

    companion object {
        private const val NUMBER_OF_HARMONICS = 7
        private const val TWO_PI = PI.toFloat() * 2f
        private const val LN_2 = 0.693147180559945f // ln(2)
    }
}
