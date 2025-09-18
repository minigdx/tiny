package com.github.minigdx.tiny.sound

import com.github.minigdx.tiny.lua.Note
import kotlin.test.Test
import kotlin.test.assertEquals

class HarmonizerTest {

    @Test
    fun generate_with_empty_harmonics_returns_zero() {
        val harmonizer = Harmonizer(floatArrayOf())
        val generator = { freq: Float, sample: Int -> 1.0f }

        val result = harmonizer.generate(Note.A4, 0, generator)
        assertEquals(0.0f, result, 0.001f)
    }

    @Test
    fun generate_with_single_harmonic_applies_amplitude() {
        val harmonizer = Harmonizer(floatArrayOf(0.5f))
        val generator = { freq: Float, sample: Int -> 2.0f }

        val result = harmonizer.generate(Note.A4, 0, generator)
        // Should be: 0.5 * 2.0 = 1.0
        assertEquals(1.0f, result, 0.001f)
    }

    @Test
    fun generate_with_multiple_harmonics_sums_contributions() {
        val harmonizer = Harmonizer(floatArrayOf(0.5f, 0.3f, 0.2f))
        val generator = { freq: Float, sample: Int -> 1.0f }

        val result = harmonizer.generate(Note.A4, 0, generator)
        // Should be: (0.5 * 1.0) + (0.3 * 1.0) + (0.2 * 1.0) = 1.0
        assertEquals(1.0f, result, 0.001f)
    }

    @Test
    fun generate_calls_generator_with_correct_frequencies() {
        val harmonizer = Harmonizer(floatArrayOf(1.0f, 1.0f))
        val fundamentalFreq = Note.A4.frequency // 440.0 Hz
        val capturedFrequencies = mutableListOf<Float>()
        val capturedSamples = mutableListOf<Int>()

        val generator = { freq: Float, sample: Int ->
            capturedFrequencies.add(freq)
            capturedSamples.add(sample)
            0.5f
        }

        harmonizer.generate(Note.A4, 42, generator)

        assertEquals(2, capturedFrequencies.size)
        assertEquals(fundamentalFreq * 1, capturedFrequencies[0], 0.001f) // 1st harmonic (2x fundamental)
        assertEquals(fundamentalFreq * 2, capturedFrequencies[1], 0.001f) // 2nd harmonic (3x fundamental)
        assertEquals(42, capturedSamples[0]) // sample passed to generator
        assertEquals(42, capturedSamples[1]) // sample passed to generator
    }

    @Test
    fun generate_harmonic_frequencies_are_multiples_of_fundamental() {
        val harmonizer = Harmonizer(floatArrayOf(1.0f, 1.0f, 1.0f))
        val capturedFrequencies = mutableListOf<Float>()
        val fundamentalFreq = Note.C0.frequency

        val generator = { freq: Float, sample: Int ->
            capturedFrequencies.add(freq)
            1.0f
        }

        harmonizer.generate(Note.C0, 0, generator)

        assertEquals(3, capturedFrequencies.size)
        assertEquals(fundamentalFreq * 1, capturedFrequencies[0], 0.001f) // 1st harmonic (1x fundamental)
        assertEquals(fundamentalFreq * 2, capturedFrequencies[1], 0.001f) // 2nd harmonic (2x fundamental)
        assertEquals(fundamentalFreq * 3, capturedFrequencies[2], 0.001f) // 3rd harmonic (3x fundamental)
    }

    @Test
    fun generate_with_zero_amplitude_harmonics() {
        val harmonizer = Harmonizer(floatArrayOf(0.0f, 1.0f, 0.0f))
        val generator = { freq: Float, sample: Int -> 2.0f }

        val result = harmonizer.generate(Note.C0, 0, generator)
        // Should be: (0.0 * 2.0) + (1.0 * 2.0) + (0.0 * 2.0) = 2.0
        assertEquals(2.0f, result, 0.001f)
    }

    @Test
    fun generate_with_negative_amplitude_harmonics() {
        val harmonizer = Harmonizer(floatArrayOf(0.5f, -0.3f))
        val generator = { freq: Float, sample: Int -> 1.0f }

        val result = harmonizer.generate(Note.C0, 0, generator)
        // Should be: (0.5 * 1.0) + (-0.3 * 1.0) = 0.2
        assertEquals(0.2f, result, 0.001f)
    }

    @Test
    fun generate_frequency_calculation_with_different_notes() {
        val harmonizer = Harmonizer(floatArrayOf(1.0f))
        val capturedFrequencies = mutableListOf<Float>()

        val generator = { freq: Float, sample: Int ->
            capturedFrequencies.add(freq)
            1.0f
        }

        // Test with C0 (16.35 Hz)
        harmonizer.generate(Note.C0, 0, generator)
        assertEquals(Note.C0.frequency * 1, capturedFrequencies[0], 0.001f)

        capturedFrequencies.clear()

        // Test with A4 (440.0 Hz)
        harmonizer.generate(Note.A4, 0, generator)
        assertEquals(Note.A4.frequency * 1, capturedFrequencies[0], 0.001f)
    }

    @Test
    fun generate_with_complex_generator_function() {
        val harmonizer = Harmonizer(floatArrayOf(0.8f, 0.6f, 0.4f))
        var callCount = 0

        // Generator that returns increasing values for each harmonic
        val generator = { freq: Float, sample: Int ->
            callCount++
            callCount.toFloat()
        }

        val result = harmonizer.generate(Note.C0, 0, generator)
        // Should be: (0.8 * 1) + (0.6 * 2) + (0.4 * 3) = 0.8 + 1.2 + 1.2 = 3.2
        assertEquals(3.2f, result, 0.001f)
    }

    @Test
    fun generate_with_large_harmonics_array() {
        val harmonics = FloatArray(10) { index -> (index + 1) * 0.1f } // [0.1, 0.2, 0.3, ..., 1.0]
        val harmonizer = Harmonizer(harmonics)
        val generator = { freq: Float, sample: Int -> 1.0f }

        val result = harmonizer.generate(Note.C0, 0, generator)
        // Should be: 0.1 + 0.2 + 0.3 + ... + 1.0 = sum of 0.1 to 1.0 = 5.5
        assertEquals(5.5f, result, 0.001f)
    }

    @Test
    fun generate_preserves_generator_return_values() {
        val harmonizer = Harmonizer(floatArrayOf(1.0f, 1.0f))
        val generatorValues = listOf(3.14f, 2.71f)
        var callIndex = 0

        val generator = { freq: Float, sample: Int ->
            generatorValues[callIndex++]
        }

        val result = harmonizer.generate(Note.C0, 0, generator)
        // Should be: (1.0 * 3.14) + (1.0 * 2.71) = 5.85
        assertEquals(5.85f, result, 0.001f)
    }
}