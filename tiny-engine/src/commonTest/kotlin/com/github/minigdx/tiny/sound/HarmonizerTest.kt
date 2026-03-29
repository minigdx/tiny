package com.github.minigdx.tiny.sound

import com.github.minigdx.tiny.lua.Note
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class HarmonizerTest {
    @Test
    fun generate_with_empty_harmonics_returns_zero() {
        val harmonizer = Harmonizer(harmonics0 = { floatArrayOf() })
        val generator = { freq: Float, sample: Int -> 1.0f }

        val result = harmonizer.generate(Note.A4, 0, generator)
        assertEquals(0.0f, result, 0.001f)
    }

    @Test
    fun generate_with_single_harmonic_below_one_no_normalization() {
        val harmonizer = Harmonizer(harmonics0 = { floatArrayOf(0.5f) })
        val generator = { freq: Float, sample: Int -> 2.0f }

        val result = harmonizer.generate(Note.A4, 0, generator)
        // totalAmplitude = 0.5 (< 1.0), so no normalization: 0.5 * 2.0 = 1.0
        assertEquals(1.0f, result, 0.001f)
    }

    @Test
    fun generate_with_multiple_harmonics_summing_to_one_no_normalization() {
        val harmonizer = Harmonizer(harmonics0 = { floatArrayOf(0.5f, 0.3f, 0.2f) })
        val generator = { freq: Float, sample: Int -> 1.0f }

        val result = harmonizer.generate(Note.A4, 0, generator)
        // totalAmplitude = 1.0 (not > 1.0), so no normalization: 0.5 + 0.3 + 0.2 = 1.0
        assertEquals(1.0f, result, 0.001f)
    }

    @Test
    fun generate_normalizes_when_total_amplitude_exceeds_one() {
        val harmonizer = Harmonizer(harmonics0 = { floatArrayOf(1.0f, 1.0f) })
        val generator = { freq: Float, sample: Int -> 1.0f }

        val result = harmonizer.generate(Note.A4, 0, generator)
        // totalAmplitude = 2.0, normFactor = 0.5
        // raw sum = 1.0 + 1.0 = 2.0, normalized = 2.0 * 0.5 = 1.0
        assertEquals(1.0f, result, 0.001f)
    }

    @Test
    fun generate_calls_generator_with_correct_frequencies() {
        val harmonizer = Harmonizer(harmonics0 = { floatArrayOf(1.0f, 1.0f) })
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
        assertEquals(fundamentalFreq * 1, capturedFrequencies[0], 0.001f) // fundamental (1x)
        assertEquals(fundamentalFreq * 2, capturedFrequencies[1], 0.001f) // 2nd harmonic (2x)
        assertEquals(42, capturedSamples[0])
        assertEquals(42, capturedSamples[1])
    }

    @Test
    fun generate_harmonic_frequencies_are_multiples_of_fundamental() {
        val harmonizer = Harmonizer(harmonics0 = { floatArrayOf(1.0f, 1.0f, 1.0f) })
        val capturedFrequencies = mutableListOf<Float>()
        val fundamentalFreq = Note.C0.frequency

        val generator = { freq: Float, sample: Int ->
            capturedFrequencies.add(freq)
            1.0f
        }

        harmonizer.generate(Note.C0, 0, generator)

        assertEquals(3, capturedFrequencies.size)
        assertEquals(fundamentalFreq * 1, capturedFrequencies[0], 0.001f) // fundamental (1x)
        assertEquals(fundamentalFreq * 2, capturedFrequencies[1], 0.001f) // 2nd harmonic (2x)
        assertEquals(fundamentalFreq * 3, capturedFrequencies[2], 0.001f) // 3rd harmonic (3x)
    }

    @Test
    fun generate_with_zero_amplitude_harmonics() {
        val harmonizer = Harmonizer(harmonics0 = { floatArrayOf(0.0f, 1.0f, 0.0f) })
        val generator = { freq: Float, sample: Int -> 2.0f }

        val result = harmonizer.generate(Note.C0, 0, generator)
        // totalAmplitude = 1.0 (not > 1.0), no normalization: (0.0 * 2.0) + (1.0 * 2.0) + (0.0 * 2.0) = 2.0
        assertEquals(2.0f, result, 0.001f)
    }

    @Test
    fun generate_with_negative_amplitude_harmonics() {
        val harmonizer = Harmonizer(harmonics0 = { floatArrayOf(0.5f, -0.3f) })
        val generator = { freq: Float, sample: Int -> 1.0f }

        val result = harmonizer.generate(Note.C0, 0, generator)
        // totalAmplitude = 0.5 + (-0.3) = 0.2 (< 1.0), no normalization: 0.5 - 0.3 = 0.2
        assertEquals(0.2f, result, 0.001f)
    }

    @Test
    fun generate_frequency_calculation_with_different_notes() {
        val harmonizer = Harmonizer(harmonics0 = { floatArrayOf(1.0f) })
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
        val harmonizer = Harmonizer(harmonics0 = { floatArrayOf(0.8f, 0.6f, 0.4f) })
        var callCount = 0

        // Generator that returns increasing values for each harmonic
        val generator = { freq: Float, sample: Int ->
            callCount++
            callCount.toFloat()
        }

        val result = harmonizer.generate(Note.C0, 0, generator)
        // totalAmplitude = 0.8 + 0.6 + 0.4 = 1.8 > 1.0, normFactor = 1/1.8
        // raw sum = (0.8 * 1) + (0.6 * 2) + (0.4 * 3) = 0.8 + 1.2 + 1.2 = 3.2
        // normalized = 3.2 / 1.8 ≈ 1.778
        assertEquals(3.2f / 1.8f, result, 0.01f)
    }

    @Test
    fun generate_with_large_harmonics_array() {
        val harmonics = FloatArray(10) { index -> (index + 1) * 0.1f } // [0.1, 0.2, 0.3, ..., 1.0]
        val harmonizer = Harmonizer(harmonics0 = { harmonics })
        val generator = { freq: Float, sample: Int -> 1.0f }

        val result = harmonizer.generate(Note.C0, 0, generator)
        // totalAmplitude = 0.1 + 0.2 + ... + 1.0 = 5.5 > 1.0, normFactor = 1/5.5
        // raw sum = 5.5, normalized = 5.5 / 5.5 = 1.0
        assertEquals(1.0f, result, 0.001f)
    }

    @Test
    fun generate_preserves_generator_return_values() {
        val harmonizer = Harmonizer(harmonics0 = { floatArrayOf(1.0f, 1.0f) })
        val generatorValues = listOf(3.14f, 2.71f)
        var callIndex = 0

        val generator = { freq: Float, sample: Int ->
            generatorValues[callIndex++]
        }

        val result = harmonizer.generate(Note.C0, 0, generator)
        // totalAmplitude = 2.0 > 1.0, normFactor = 0.5
        // raw sum = (1.0 * 3.14) + (1.0 * 2.71) = 5.85
        // normalized = 5.85 * 0.5 = 2.925
        assertEquals(2.925f, result, 0.001f)
    }

    @Test
    fun generate_output_bounded_with_unit_generator() {
        // When generator returns values in [-1,1] and harmonics sum > 1,
        // the normalizer should ensure output stays in [-1,1]
        val harmonizer = Harmonizer(harmonics0 = { floatArrayOf(1.0f, 0.8f, 0.6f, 0.4f) })
        val generator = { freq: Float, sample: Int -> 1.0f }

        val result = harmonizer.generate(Note.A4, 0, generator)
        assertTrue(result >= -1.0f && result <= 1.0f, "Normalized output should be bounded, got $result")
    }
}
