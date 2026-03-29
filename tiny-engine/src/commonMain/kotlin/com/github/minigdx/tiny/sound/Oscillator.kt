package com.github.minigdx.tiny.sound

import com.github.minigdx.tiny.Sample
import com.github.minigdx.tiny.sound.SoundManager.Companion.SAMPLE_RATE
import kotlin.math.PI
import kotlin.math.exp
import kotlin.math.max
import kotlin.math.sin
import kotlin.random.Random

/**
 * An Oscillator generates waveform values for different wave types at specific sample positions.
 * It converts a sample index (discrete time position) and frequency into an audio sample value.
 *
 * The oscillator supports various wave types including sine, triangle, square, sawtooth, pulse, and noise.
 * Each wave type produces a different harmonic content and timbre.
 *
 * @param waveType0 The type of waveform to generate (SINE, TRIANGLE, SQUARE, SAW_TOOTH, PULSE, NOISE)
 * @param dutyCycle0 The duty cycle for the PULSE wave type (0.0 to 1.0, default 0.5)
 */
class Oscillator(
    val waveType0: () -> Instrument.WaveType,
    val dutyCycle0: () -> Float = { 0.5f },
) {
    // State for NOISE wave type - implements a low-pass filter
    private var lastOutput: Float = 0.0f
    private var lastFrequencyUsed: Float = 0.0f
    private var cachedAlpha: Float = 0.0f
    private val random: Random = Random(42)

    // DC blocker state for NOISE
    private var dcBlockerPrev: Float = 0f
    private var dcBlockerOut: Float = 0f

    /**
     * Generates a single audio sample value for the given frequency at the specified sample position.
     *
     * The sample position (progress) is converted to time in seconds by dividing by the sample rate.
     * This time value is then used with the frequency to generate the appropriate waveform sample.
     *
     * @param frequency The frequency in Hz of the oscillation
     * @param progress The current sample index (discrete time position)
     * @return A floating-point sample value typically in the range [-1.0, 1.0]
     */
    fun emit(
        frequency: Float,
        progress: Sample,
    ): Float {
        // Convert sample index to time in seconds
        val time = progress.toFloat() / SAMPLE_RATE.toFloat()
        val waveType = waveType0.invoke()
        return when (waveType) {
            Instrument.WaveType.SINE -> {
                sin(TWO_PI * frequency * time)
            }

            Instrument.WaveType.TRIANGLE -> {
                val phase = (frequency * time) % 1.0f
                if (phase < 0.5f) {
                    (4.0f * phase) - 1.0f
                } else {
                    3.0f - (4.0f * phase)
                }
            }

            Instrument.WaveType.SQUARE -> {
                val value = sin(TWO_PI * frequency * time)
                if (value > 0f) 1f else -1f
            }

            Instrument.WaveType.SAW_TOOTH -> {
                val phase = (frequency * time) % 1.0f
                (2.0f * phase) - 1.0f
            }

            Instrument.WaveType.PULSE -> {
                val phase = (frequency * time) % 1.0f
                val dc = dutyCycle0()
                val raw = if (phase < dc) 1.0f else -1.0f
                // Remove DC offset for non-50% duty cycles
                val dcOffset = (2.0f * dc) - 1.0f
                raw - dcOffset
            }

            Instrument.WaveType.NOISE -> {
                val alpha = if (lastFrequencyUsed == frequency) {
                    cachedAlpha
                } else {
                    val safeCutoff = max(1f, frequency)
                    val wc = TWO_PI * safeCutoff / SAMPLE_RATE
                    val x = exp(-wc)
                    cachedAlpha = 1.0f - x
                    lastFrequencyUsed = frequency
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

            Instrument.WaveType.DRUM -> DrumSynthesizer.generate(frequency, time, random)
        }
    }

    companion object {
        private const val TWO_PI = PI.toFloat() * 2f
    }
}
