package com.github.minigdx.tiny.sound

import com.github.minigdx.tiny.Percent
import com.github.minigdx.tiny.Seconds
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
        Sweep(440f, 1f),
        Vibrato(0f, 0f),
    ),
    /**
     * Duty cycle for the PULSE wave type.
     * 0.5 = square wave, 0.25 = nasal, 0.125 = thin/reedy.
     */
    var dutyCycle: Percent = 0.5f,
    /**
     * Tremolo effect (amplitude modulation).
     */
    val tremolo: Tremolo = Tremolo(),
) {
    enum class WaveType {
        SAW_TOOTH,
        PULSE,
        TRIANGLE,
        SINE,
        NOISE,
        SQUARE,
        DRUM,
    }

    // State for NOISE wave type - low-pass filter
    @Transient
    private var lastOutput: Float = 0.0f

    @Transient
    private var lastFrequencyUsed: Float = 0.0f

    @Transient
    private var cachedAlpha: Float = 0.0f

    @Transient
    private val random: Random = Random(42)

    // DC blocker state for NOISE
    @Transient
    private var dcBlockerPrev: Float = 0f

    @Transient
    private var dcBlockerOut: Float = 0f

    fun generate(
        freq: Float,
        time: Float,
    ): Float {
        // Apply modulation to the base frequency
        val harmonicFreq = modulations.filter { it.active }
            .fold(freq) { acc, modulation -> modulation.apply(time, acc) }

        val sample = when (this.wave) {
            TRIANGLE -> {
                val phase = (harmonicFreq * time) % 1.0f
                if (phase < 0.5f) {
                    (4.0f * phase) - 1.0f
                } else {
                    3.0f - (4.0f * phase)
                }
            }

            SINE -> sin(TWO_PI * harmonicFreq * time)

            SQUARE -> {
                val value = sin(TWO_PI * harmonicFreq * time)
                if (value > 0f) 1f else -1f
            }

            PULSE -> {
                val phase = (harmonicFreq * time) % 1.0f
                val dc = dutyCycle
                val raw = if (phase < dc) 1.0f else -1.0f
                // Remove DC offset for non-50% duty cycles
                val dcOffset = (2.0f * dc) - 1.0f
                raw - dcOffset
            }

            SAW_TOOTH -> {
                val phase = (harmonicFreq * time) % 1.0f
                (2.0f * phase) - 1.0f
            }

            NOISE -> {
                val alpha =
                    if (lastFrequencyUsed == harmonicFreq) {
                        cachedAlpha
                    } else {
                        val safeCutoff = max(1f, harmonicFreq)
                        val wc = TWO_PI * safeCutoff / SAMPLE_RATE
                        val x = exp(-wc)
                        cachedAlpha = 1.0f - x
                        lastFrequencyUsed = harmonicFreq
                        cachedAlpha
                    }
                val white = random.nextFloat() * 2f - 1f
                val filtered = alpha * white + (1.0f - alpha) * lastOutput
                lastOutput = filtered

                // DC blocker (single-pole high-pass, ~20 Hz cutoff)
                val dcAlpha = 0.997f
                dcBlockerOut = dcAlpha * (dcBlockerOut + filtered - dcBlockerPrev)
                dcBlockerPrev = filtered
                dcBlockerOut
            }

            DRUM -> DrumSynthesizer.generate(harmonicFreq, time, random)
        }

        return tremolo.apply(time, sample)
    }

    /**
     * Return only the active modulations, to avoid filtering per sample.
     */
    fun activeModulations(): List<Modulation> = modulations.filter { it.active }

    /**
     * Generate a sample with pre-filtered active modulations to avoid
     * per-sample list allocation.
     */
    fun generate(
        freq: Float,
        time: Float,
        activeModulations: List<Modulation>,
    ): Float {
        val harmonicFreq = activeModulations.fold(freq) { acc, modulation -> modulation.apply(time, acc) }

        val sample = when (this.wave) {
            TRIANGLE -> {
                val phase = (harmonicFreq * time) % 1.0f
                if (phase < 0.5f) {
                    (4.0f * phase) - 1.0f
                } else {
                    3.0f - (4.0f * phase)
                }
            }

            SINE -> sin(TWO_PI * harmonicFreq * time)

            SQUARE -> {
                val value = sin(TWO_PI * harmonicFreq * time)
                if (value > 0f) 1f else -1f
            }

            PULSE -> {
                val phase = (harmonicFreq * time) % 1.0f
                val dc = dutyCycle
                val raw = if (phase < dc) 1.0f else -1.0f
                val dcOffset = (2.0f * dc) - 1.0f
                raw - dcOffset
            }

            SAW_TOOTH -> {
                val phase = (harmonicFreq * time) % 1.0f
                (2.0f * phase) - 1.0f
            }

            NOISE -> {
                val alpha =
                    if (lastFrequencyUsed == harmonicFreq) {
                        cachedAlpha
                    } else {
                        val safeCutoff = max(1f, harmonicFreq)
                        val wc = TWO_PI * safeCutoff / SAMPLE_RATE
                        val x = exp(-wc)
                        cachedAlpha = 1.0f - x
                        lastFrequencyUsed = harmonicFreq
                        cachedAlpha
                    }
                val white = random.nextFloat() * 2f - 1f
                val filtered = alpha * white + (1.0f - alpha) * lastOutput
                lastOutput = filtered

                val dcAlpha = 0.997f
                dcBlockerOut = dcAlpha * (dcBlockerOut + filtered - dcBlockerPrev)
                dcBlockerPrev = filtered
                dcBlockerOut
            }

            DRUM -> DrumSynthesizer.generate(harmonicFreq, time, random)
        }

        return tremolo.apply(time, sample)
    }

    companion object {
        private const val NUMBER_OF_HARMONICS = 7
        private const val TWO_PI = PI.toFloat() * 2f
    }
}
