package com.github.minigdx.tiny.sound

import com.github.minigdx.tiny.Percent
import com.github.minigdx.tiny.Seconds
import com.github.minigdx.tiny.lua.Note
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
    }

    // Last output generated. Used by the [NOISE] wave type
    @Transient
    private var lastOutput: Float = 0.0f

    @Transient
    private var lastFrequencyUsed: Float = 0.0f

    @Transient
    private var cachedAlpha: Float = 0.0f

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
        }
    }

    companion object {
        private const val NUMBER_OF_HARMONICS = 7
        private const val TWO_PI = PI.toFloat() * 2f
    }
}
