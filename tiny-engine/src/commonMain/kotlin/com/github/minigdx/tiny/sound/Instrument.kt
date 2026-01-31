package com.github.minigdx.tiny.sound

import com.github.minigdx.tiny.Percent
import com.github.minigdx.tiny.Seconds
import com.github.minigdx.tiny.lua.Note
import com.github.minigdx.tiny.sound.Instrument.WaveType.HIHAT_CLOSED
import com.github.minigdx.tiny.sound.Instrument.WaveType.HIHAT_OPEN
import com.github.minigdx.tiny.sound.Instrument.WaveType.KICK
import com.github.minigdx.tiny.sound.Instrument.WaveType.NOISE
import com.github.minigdx.tiny.sound.Instrument.WaveType.PULSE
import com.github.minigdx.tiny.sound.Instrument.WaveType.SAW_TOOTH
import com.github.minigdx.tiny.sound.Instrument.WaveType.SINE
import com.github.minigdx.tiny.sound.Instrument.WaveType.SNARE
import com.github.minigdx.tiny.sound.Instrument.WaveType.SQUARE
import com.github.minigdx.tiny.sound.Instrument.WaveType.TRIANGLE
import com.github.minigdx.tiny.sound.SoundManager.Companion.SAMPLE_RATE
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.exp
import kotlin.math.max
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
         * Kick drum (bass drum) synthesis.
         * Uses a sine wave with rapid pitch decay from ~150Hz to ~50Hz.
         * The frequency parameter controls the initial pitch.
         */
        KICK,
        /**
         * Snare drum synthesis.
         * Combines a pitched body (low sine) with filtered noise for the snare wires.
         * The frequency parameter controls the body pitch.
         */
        SNARE,
        /**
         * Closed hi-hat synthesis.
         * High-frequency filtered noise with very short decay.
         * The frequency parameter controls the filter cutoff.
         */
        HIHAT_CLOSED,
        /**
         * Open hi-hat synthesis.
         * High-frequency filtered noise with longer decay than closed hi-hat.
         * The frequency parameter controls the filter cutoff.
         */
        HIHAT_OPEN,
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

            KICK -> {
                // Kick drum: sine wave with rapid pitch decay
                // Start at the given frequency and decay to ~30% of it
                val pitchDecayRate = 40f // How fast the pitch drops
                val minPitchRatio = 0.3f // Final pitch is 30% of initial
                val pitchEnvelope = minPitchRatio + (1f - minPitchRatio) * exp(-pitchDecayRate * time)
                val currentFreq = harmonicFreq * pitchEnvelope

                // Main body is a sine wave
                val body = sin(TWO_PI * currentFreq * time)

                // Add a subtle click at the start (short burst of higher frequency)
                val clickAmount = exp(-200f * time) * 0.3f
                val click = sin(TWO_PI * harmonicFreq * 3f * time) * clickAmount

                return body + click
            }

            SNARE -> {
                // Snare drum: pitched body + noise for snare wires
                // Body: low frequency sine with fast decay
                val bodyFreq = harmonicFreq * 0.5f // Lower the body frequency
                val bodyDecay = exp(-20f * time)
                val body = sin(TWO_PI * bodyFreq * time) * bodyDecay * 0.6f

                // Snare wires: high-pass filtered noise
                val noiseFreq = max(4000f, harmonicFreq * 10f) // High cutoff for snare character
                val wc = TWO_PI * noiseFreq / SAMPLE_RATE
                val alpha = 1.0f - exp(-wc)
                val white = Random.nextFloat() * 2f - 1f
                drumNoiseOutput = alpha * white + (1.0f - alpha) * drumNoiseOutput
                val noise = drumNoiseOutput * 0.8f

                return body + noise
            }

            HIHAT_CLOSED -> {
                // Closed hi-hat: high-frequency filtered noise with very short decay
                // Use high cutoff frequency for metallic character
                val cutoff = max(6000f, harmonicFreq * 15f)
                val wc = TWO_PI * cutoff / SAMPLE_RATE
                val alpha = 1.0f - exp(-wc)

                val white = Random.nextFloat() * 2f - 1f
                drumNoiseOutput = alpha * white + (1.0f - alpha) * drumNoiseOutput

                // Add some metallic resonance using multiple high frequency components
                val metallic1 = sin(TWO_PI * 6500f * time) * 0.1f
                val metallic2 = sin(TWO_PI * 8200f * time) * 0.08f
                val metallic3 = sin(TWO_PI * 11000f * time) * 0.05f

                return drumNoiseOutput * 0.7f + metallic1 + metallic2 + metallic3
            }

            HIHAT_OPEN -> {
                // Open hi-hat: similar to closed but with longer sustain (handled by envelope)
                // Use slightly lower cutoff for fuller sound
                val cutoff = max(5000f, harmonicFreq * 12f)
                val wc = TWO_PI * cutoff / SAMPLE_RATE
                val alpha = 1.0f - exp(-wc)

                val white = Random.nextFloat() * 2f - 1f
                drumNoiseOutput = alpha * white + (1.0f - alpha) * drumNoiseOutput

                // More pronounced metallic resonance for open hi-hat
                val metallic1 = sin(TWO_PI * 5500f * time) * 0.15f
                val metallic2 = sin(TWO_PI * 7800f * time) * 0.12f
                val metallic3 = sin(TWO_PI * 10500f * time) * 0.08f
                val metallic4 = sin(TWO_PI * 13000f * time) * 0.04f

                return drumNoiseOutput * 0.6f + metallic1 + metallic2 + metallic3 + metallic4
            }
        }
    }

    companion object {
        private const val NUMBER_OF_HARMONICS = 7
        private const val TWO_PI = PI.toFloat() * 2f
    }
}
