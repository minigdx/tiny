package com.github.minigdx.tiny.sound

import com.github.minigdx.tiny.Frequency
import com.github.minigdx.tiny.Sample
import com.github.minigdx.tiny.lua.Note

/**
 * A Harmonizer generates complex waveforms by combining multiple harmonic frequencies
 * with the fundamental frequency of a note. This creates richer, more complex sounds
 * than simple sine waves.
 *
 * Each harmonic is a multiple of the fundamental frequency (1x, 2x, 3x, etc.) and
 * has its own amplitude weight defined in the harmonics array.
 *
 * The output is normalized so that the sum of harmonics never exceeds [-1, 1],
 * preventing clipping in downstream stages.
 *
 * @param harmonics0 Array of relative amplitudes for each harmonic. Index 0 represents
 *                   the fundamental (1x frequency), index 1 represents the 2nd harmonic
 *                   (2x frequency), index 2 represents the 3rd harmonic (3x frequency), etc.
 *                   Values typically range from 0.0 to 1.0.
 */
class Harmonizer(
    val harmonics0: () -> FloatArray,
) {
    /**
     * Generates a sample value by combining the fundamental frequency with its harmonics.
     * Each harmonic frequency is calculated as a multiple of the fundamental frequency,
     * and the generator function is called to produce the actual waveform value for each frequency.
     *
     * The result is normalized by the total harmonic amplitude to prevent exceeding [-1, 1].
     *
     * @param note The musical note that provides the fundamental frequency
     * @param sample The current sample number (used for time-based calculations)
     * @param generator A function that generates waveform values given a frequency and sample index.
     *                  Takes (frequency, sampleIndex) and returns the waveform sample value.
     * @return The combined and normalized sample value
     */
    fun generate(
        note: Note,
        sample: Sample,
        generator: (Float, Sample) -> Float,
    ): Frequency {
        val fundamentalFreq = note.frequency

        val harmonics = harmonics0.invoke()
        var sampleValue = 0f
        var totalAmplitude = 0f
        harmonics.forEachIndexed { index, relativeAmplitude ->
            val harmonicNumber = index + 1
            val harmonicFreq = fundamentalFreq * harmonicNumber
            val value = generator.invoke(harmonicFreq, sample)

            sampleValue += relativeAmplitude * value
            totalAmplitude += relativeAmplitude
        }

        // Normalize to prevent exceeding [-1, 1]
        val normFactor = if (totalAmplitude > 1.0f) 1.0f / totalAmplitude else 1.0f
        return sampleValue * normFactor
    }
}
