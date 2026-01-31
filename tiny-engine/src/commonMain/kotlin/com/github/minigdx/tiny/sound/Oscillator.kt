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
 * @param waveType The type of waveform to generate (SINE, TRIANGLE, SQUARE, SAW_TOOTH, PULSE, NOISE)
 */
class Oscillator(val waveType0: () -> Instrument.WaveType) {
    // State for NOISE wave type - implements a low-pass filter
    private var lastOutput: Float = 0.0f
    private var lastFrequencyUsed: Float = 0.0f
    private var cachedAlpha: Float = 0.0f

    // State for drum sounds that use noise
    private var drumNoiseOutput: Float = 0.0f

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
                // Generate proper triangle wave using phase
                val phase = (frequency * time) % 1.0f
                if (phase < 0.5f) {
                    // Rising edge: -1 to 1
                    (4.0f * phase) - 1.0f
                } else {
                    // Falling edge: 1 to -1
                    3.0f - (4.0f * phase)
                }
            }

            Instrument.WaveType.SQUARE -> {
                val value = sin(TWO_PI * frequency * time)
                if (value > 0f) 1f else -1f
            }

            Instrument.WaveType.SAW_TOOTH -> {
                // Generate proper sawtooth wave using phase instead of sine
                val phase = (frequency * time) % 1.0f
                (2.0f * phase) - 1.0f
            }

            Instrument.WaveType.PULSE -> {
                // Generate pulse wave with variable duty cycle
                val phase = (frequency * time) % 1.0f
                val dutyCycle = 0.25f // 25% duty cycle
                if (phase < dutyCycle) 1.0f else -1.0f
            }

            Instrument.WaveType.NOISE -> {
                // Filtered noise implementation using a low-pass filter
                val alpha = if (lastFrequencyUsed == frequency) {
                    cachedAlpha
                } else {
                    val safeCutoff = max(1f, frequency)
                    val wc = TWO_PI * safeCutoff / SAMPLE_RATE
                    val x = exp(-wc)
                    // Cache values for performance
                    cachedAlpha = 1.0f - x
                    lastFrequencyUsed = frequency
                    cachedAlpha
                }

                // Generate white noise using time-based seeding for better randomness
                val seed = (time * 12345.0f + progress * 67890.0f).toLong()
                val white = Random(seed).nextFloat() * 2f - 1f
                val result = alpha * white + (1.0f - alpha) * lastOutput
                lastOutput = result
                result
            }

            Instrument.WaveType.KICK -> {
                // Kick drum: sine wave with rapid pitch decay
                val pitchDecayRate = 40f
                val minPitchRatio = 0.3f
                val pitchEnvelope = minPitchRatio + (1f - minPitchRatio) * exp(-pitchDecayRate * time)
                val currentFreq = frequency * pitchEnvelope

                // Main body is a sine wave
                val body = sin(TWO_PI * currentFreq * time)

                // Add a subtle click at the start
                val clickAmount = exp(-200f * time) * 0.3f
                val click = sin(TWO_PI * frequency * 3f * time) * clickAmount

                body + click
            }

            Instrument.WaveType.SNARE -> {
                // Snare drum: pitched body + noise for snare wires
                val bodyFreq = frequency * 0.5f
                val bodyDecay = exp(-20f * time)
                val body = sin(TWO_PI * bodyFreq * time) * bodyDecay * 0.6f

                // Snare wires: high-pass filtered noise
                val noiseFreq = max(4000f, frequency * 10f)
                val wc = TWO_PI * noiseFreq / SAMPLE_RATE
                val alpha = 1.0f - exp(-wc)
                val seed = (time * 12345.0f + progress * 67890.0f).toLong()
                val white = Random(seed).nextFloat() * 2f - 1f
                drumNoiseOutput = alpha * white + (1.0f - alpha) * drumNoiseOutput
                val noise = drumNoiseOutput * 0.8f

                body + noise
            }

            Instrument.WaveType.HIHAT_CLOSED -> {
                // Closed hi-hat: high-frequency filtered noise
                val cutoff = max(6000f, frequency * 15f)
                val wc = TWO_PI * cutoff / SAMPLE_RATE
                val alpha = 1.0f - exp(-wc)

                val seed = (time * 12345.0f + progress * 67890.0f).toLong()
                val white = Random(seed).nextFloat() * 2f - 1f
                drumNoiseOutput = alpha * white + (1.0f - alpha) * drumNoiseOutput

                // Metallic resonance
                val metallic1 = sin(TWO_PI * 6500f * time) * 0.1f
                val metallic2 = sin(TWO_PI * 8200f * time) * 0.08f
                val metallic3 = sin(TWO_PI * 11000f * time) * 0.05f

                drumNoiseOutput * 0.7f + metallic1 + metallic2 + metallic3
            }

            Instrument.WaveType.HIHAT_OPEN -> {
                // Open hi-hat: similar to closed but with longer sustain
                val cutoff = max(5000f, frequency * 12f)
                val wc = TWO_PI * cutoff / SAMPLE_RATE
                val alpha = 1.0f - exp(-wc)

                val seed = (time * 12345.0f + progress * 67890.0f).toLong()
                val white = Random(seed).nextFloat() * 2f - 1f
                drumNoiseOutput = alpha * white + (1.0f - alpha) * drumNoiseOutput

                // More pronounced metallic resonance
                val metallic1 = sin(TWO_PI * 5500f * time) * 0.15f
                val metallic2 = sin(TWO_PI * 7800f * time) * 0.12f
                val metallic3 = sin(TWO_PI * 10500f * time) * 0.08f
                val metallic4 = sin(TWO_PI * 13000f * time) * 0.04f

                drumNoiseOutput * 0.6f + metallic1 + metallic2 + metallic3 + metallic4
            }
        }
    }

    companion object {
        private const val TWO_PI = PI.toFloat() * 2f
    }
}
