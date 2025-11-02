package com.github.minigdx.tiny.sound

import com.github.minigdx.tiny.Frequency
import com.github.minigdx.tiny.Sample
import com.github.minigdx.tiny.lua.Note

/**
 * A Harmonizer generates complex waveforms by combining multiple harmonic frequencies
 * with the fundamental frequency of a note. This creates richer, more complex sounds
 * than simple sine waves.
 *
 * Each harmonic is a multiple of the fundamental frequency (2x, 3x, 4x, etc.) and
 * has its own amplitude weight defined in the harmonics array.
 *
 * @param harmonics Array of relative amplitudes for each harmonic. Index 0 represents
 *                  the first harmonic (2x fundamental), index 1 represents the second
 *                  harmonic (3x fundamental), etc. Values typically range from 0.0 to 1.0.
 */
class Harmonizer(
    val harmonics0: () -> FloatArray,
) {
    /**
     * Generates a sample value by combining the fundamental frequency with its harmonics.
     * Each harmonic frequency is calculated as a multiple of the fundamental frequency,
     * and the generator function is called to produce the actual waveform value for each frequency.
     *
     * @param note The musical note that provides the fundamental frequency
     * @param sample The current sample number (used for time-based calculations)
     * @param generator A function that generates waveform values given a frequency and harmonic number.
     *                  Takes (frequency, harmonicNumber) and returns the waveform sample value.
     * @return The combined sample value of the fundamental frequency and all its harmonics
     */
    fun generate(
        note: Note,
        sample: Sample,
        generator: (Float, Sample) -> Float,
    ): Frequency {
        val fundamentalFreq = note.frequency

        val harmonics = harmonics0.invoke()
        var sampleValue = 0f
        harmonics.forEachIndexed { index, relativeAmplitude ->
            // Harmonic numbers start at 1 (fundamental is implied to be 1x)
            // So index 0 = 2nd harmonic (2x), index 1 = 3rd harmonic (3x), etc.
            val harmonicNumber = index + 1
            val harmonicFreq = fundamentalFreq * harmonicNumber
            val value = generator.invoke(harmonicFreq, sample)

            // Weight the harmonic contribution by its relative amplitude
            sampleValue += relativeAmplitude * value
        }
        return sampleValue
    }
}
