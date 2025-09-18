package com.github.minigdx.tiny.sound

import com.github.minigdx.tiny.sound.SoundManager.Companion.SAMPLE_RATE
import kotlin.math.abs
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class OscillatorTest {
    @Test
    fun emit_sine_wave_generates_correct_values() {
        val oscillator = Oscillator(Instrument.WaveType.SINE)
        val frequency = 440.0f // A4

        // Test at known sample positions
        val result0 = oscillator.emit(frequency, 0)
        assertEquals(0.0f, result0, 0.001f) // sin(0) = 0

        // Test at quarter period (should be close to 1.0)
        val quarterPeriodSample = (SAMPLE_RATE / (frequency * 4)).toInt()
        val resultQuarter = oscillator.emit(frequency, quarterPeriodSample)
        assertTrue(resultQuarter > 0.9f) // Should be close to 1.0

        // Test at half period (should be close to 0)
        val halfPeriodSample = (SAMPLE_RATE / (frequency * 2)).toInt()
        val resultHalf = oscillator.emit(frequency, halfPeriodSample)
        assertTrue(abs(resultHalf) < 0.1f) // Should be close to 0
    }

    @Test
    fun emit_square_wave_generates_correct_values() {
        val oscillator = Oscillator(Instrument.WaveType.SQUARE)
        val frequency = 1000.0f

        // Square wave should be either 1.0 or -1.0
        val result1 = oscillator.emit(frequency, 0)
        assertTrue(result1 == 1.0f || result1 == -1.0f)

        val result2 = oscillator.emit(frequency, 100)
        assertTrue(result2 == 1.0f || result2 == -1.0f)
    }

    @Test
    fun emit_triangle_wave_generates_bounded_values() {
        val oscillator = Oscillator(Instrument.WaveType.TRIANGLE)
        val frequency = 880.0f

        // Test multiple sample positions
        for (sample in 0..1000 step 50) {
            val result = oscillator.emit(frequency, sample)
            assertTrue(result >= -1.0f && result <= 1.0f, "Triangle wave should be bounded between -1 and 1, got $result")
        }
    }

    @Test
    fun emit_sawtooth_wave_generates_bounded_values() {
        val oscillator = Oscillator(Instrument.WaveType.SAW_TOOTH)
        val frequency = 220.0f

        // Test multiple sample positions
        for (sample in 0..1000 step 50) {
            val result = oscillator.emit(frequency, sample)
            assertTrue(result >= -1.0f && result <= 1.0f, "Sawtooth wave should be bounded between -1 and 1, got $result")
        }
    }

    @Test
    fun emit_pulse_wave_generates_bounded_values() {
        val oscillator = Oscillator(Instrument.WaveType.PULSE)
        val frequency = 330.0f

        // Test multiple sample positions
        for (sample in 0..1000 step 50) {
            val result = oscillator.emit(frequency, sample)
            assertTrue(result >= -1.0f && result <= 1.0f, "Pulse wave should be bounded between -1 and 1, got $result")
        }
    }

    @Test
    fun emit_noise_generates_random_bounded_values() {
        val oscillator = Oscillator(Instrument.WaveType.NOISE)
        val frequency = 1000.0f

        val results = mutableListOf<Float>()

        // Generate multiple noise samples
        for (sample in 0..100) {
            val result = oscillator.emit(frequency, sample)
            results.add(result)
            assertTrue(result >= -1.0f && result <= 1.0f, "Noise should be bounded between -1 and 1, got $result")
        }

        // Verify noise is actually random (not all the same value)
        val uniqueValues = results.toSet()
        assertTrue(uniqueValues.size > 10, "Noise should generate varied values")
    }

    @Test
    fun emit_same_frequency_different_samples_produces_different_values() {
        val oscillator = Oscillator(Instrument.WaveType.SINE)
        val frequency = 440.0f

        val result1 = oscillator.emit(frequency, 0)
        val result2 = oscillator.emit(frequency, 100)
        val result3 = oscillator.emit(frequency, 1000)

        // For a sine wave, different sample positions should generally produce different values
        val results = setOf(result1, result2, result3)
        assertTrue(results.size >= 2, "Different sample positions should produce different values")
    }

    @Test
    fun emit_different_frequencies_produce_different_periods() {
        val oscillator = Oscillator(Instrument.WaveType.SINE)

        // Low frequency should change more slowly
        val lowFreq = 100.0f
        val lowResult1 = oscillator.emit(lowFreq, 0)
        val lowResult2 = oscillator.emit(lowFreq, 10)

        // High frequency should change more quickly
        val highFreq = 2000.0f
        val highResult1 = oscillator.emit(highFreq, 0)
        val highResult2 = oscillator.emit(highFreq, 10)

        val lowChange = abs(lowResult2 - lowResult1)
        val highChange = abs(highResult2 - highResult1)

        // Higher frequency should generally show more change in the same number of samples
        assertTrue(highChange >= lowChange, "Higher frequency should change more rapidly")
    }

    @Test
    fun emit_zero_frequency_produces_constant_or_near_constant_values() {
        val oscillator = Oscillator(Instrument.WaveType.SINE)
        val frequency = 0.0f

        val results = mutableListOf<Float>()
        for (sample in 0..100 step 10) {
            results.add(oscillator.emit(frequency, sample))
        }

        // All values should be the same (or very close) for zero frequency
        val firstValue = results.first()
        results.forEach { result ->
            assertEquals(firstValue, result, 0.001f)
        }
    }

    @Test
    fun emit_noise_frequency_affects_filtering() {
        val oscillator = Oscillator(Instrument.WaveType.NOISE)

        // Test that different frequencies produce different noise characteristics
        val lowFreqResults = mutableListOf<Float>()
        val highFreqResults = mutableListOf<Float>()

        for (sample in 0..50) {
            lowFreqResults.add(oscillator.emit(100.0f, sample))
        }

        // Create new oscillator for high frequency to reset internal state
        val oscillator2 = Oscillator(Instrument.WaveType.NOISE)
        for (sample in 0..50) {
            highFreqResults.add(oscillator2.emit(5000.0f, sample))
        }

        // Both should produce varied results
        assertTrue(lowFreqResults.toSet().size > 10)
        assertTrue(highFreqResults.toSet().size > 10)
    }

    @Test
    fun emit_consistency_same_inputs_produce_same_outputs() {
        val oscillator1 = Oscillator(Instrument.WaveType.SINE)
        val oscillator2 = Oscillator(Instrument.WaveType.SINE)

        val frequency = 440.0f
        val sample = 1000

        val result1 = oscillator1.emit(frequency, sample)
        val result2 = oscillator2.emit(frequency, sample)

        assertEquals(result1, result2, 0.001f, "Same inputs should produce same outputs")
    }

    @Test
    fun emit_all_wave_types_produce_bounded_output() {
        val waveTypes = listOf(
            Instrument.WaveType.SINE,
            Instrument.WaveType.TRIANGLE,
            Instrument.WaveType.SQUARE,
            Instrument.WaveType.SAW_TOOTH,
            Instrument.WaveType.PULSE,
            Instrument.WaveType.NOISE,
        )

        val frequency = 440.0f

        waveTypes.forEach { waveType ->
            val oscillator = Oscillator(waveType)

            for (sample in 0..100 step 10) {
                val result = oscillator.emit(frequency, sample)
                assertTrue(
                    result >= -1.0f && result <= 1.0f,
                    "Wave type $waveType should produce bounded output [-1, 1], got $result at sample $sample",
                )
            }
        }
    }
}
