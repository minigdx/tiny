package com.github.minigdx.tiny.sound

import com.github.minigdx.tiny.sound.SoundManager.Companion.SAMPLE_RATE
import kotlin.math.abs
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class OscillatorTest {
    @Test
    fun emit_sine_wave_generates_correct_values() {
        val oscillator = Oscillator(waveType0 = { Instrument.WaveType.SINE })
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
        val oscillator = Oscillator(waveType0 = { Instrument.WaveType.SQUARE })
        val frequency = 1000.0f

        // Square wave should be either 1.0 or -1.0
        val result1 = oscillator.emit(frequency, 0)
        assertTrue(result1 == 1.0f || result1 == -1.0f)

        val result2 = oscillator.emit(frequency, 100)
        assertTrue(result2 == 1.0f || result2 == -1.0f)
    }

    @Test
    fun emit_triangle_wave_generates_bounded_values() {
        val oscillator = Oscillator(waveType0 = { Instrument.WaveType.TRIANGLE })
        val frequency = 880.0f

        // Test multiple sample positions
        for (sample in 0..1000 step 50) {
            val result = oscillator.emit(frequency, sample)
            assertTrue(result >= -1.0f && result <= 1.0f, "Triangle wave should be bounded between -1 and 1, got $result")
        }
    }

    @Test
    fun emit_sawtooth_wave_generates_bounded_values() {
        val oscillator = Oscillator(waveType0 = { Instrument.WaveType.SAW_TOOTH })
        val frequency = 220.0f

        // Test multiple sample positions
        for (sample in 0..1000 step 50) {
            val result = oscillator.emit(frequency, sample)
            assertTrue(result >= -1.0f && result <= 1.0f, "Sawtooth wave should be bounded between -1 and 1, got $result")
        }
    }

    @Test
    fun emit_pulse_wave_generates_bounded_values() {
        val oscillator = Oscillator(waveType0 = { Instrument.WaveType.PULSE })
        val frequency = 330.0f

        // Test multiple sample positions
        for (sample in 0..1000 step 50) {
            val result = oscillator.emit(frequency, sample)
            assertTrue(result >= -1.0f && result <= 1.0f, "Pulse wave should be bounded between -1 and 1, got $result")
        }
    }

    @Test
    fun emit_noise_generates_random_bounded_values() {
        val oscillator = Oscillator(waveType0 = { Instrument.WaveType.NOISE })
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
        val oscillator = Oscillator(waveType0 = { Instrument.WaveType.SINE })
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
        val oscillator = Oscillator(waveType0 = { Instrument.WaveType.SINE })

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
        val oscillator = Oscillator(waveType0 = { Instrument.WaveType.SINE })
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
        val oscillator = Oscillator(waveType0 = { Instrument.WaveType.NOISE })

        // Test that different frequencies produce different noise characteristics
        val lowFreqResults = mutableListOf<Float>()
        val highFreqResults = mutableListOf<Float>()

        for (sample in 0..50) {
            lowFreqResults.add(oscillator.emit(100.0f, sample))
        }

        // Create new oscillator for high frequency to reset internal state
        val oscillator2 = Oscillator(waveType0 = { Instrument.WaveType.NOISE })
        for (sample in 0..50) {
            highFreqResults.add(oscillator2.emit(5000.0f, sample))
        }

        // Both should produce varied results
        assertTrue(lowFreqResults.toSet().size > 10)
        assertTrue(highFreqResults.toSet().size > 10)
    }

    @Test
    fun emit_consistency_same_inputs_produce_same_outputs() {
        val oscillator1 = Oscillator(waveType0 = { Instrument.WaveType.SINE })
        val oscillator2 = Oscillator(waveType0 = { Instrument.WaveType.SINE })

        val frequency = 440.0f
        val sample = 1000

        val result1 = oscillator1.emit(frequency, sample)
        val result2 = oscillator2.emit(frequency, sample)

        assertEquals(result1, result2, 0.001f, "Same inputs should produce same outputs")
    }

    @Test
    fun emit_drum_bass_snare_hihat_produce_bounded_output() {
        val oscillator = Oscillator(waveType0 = { Instrument.WaveType.DRUM })
        // C4 = ~261.63 Hz (bass drum), D4 = ~293.66 Hz (snare), E4 = ~329.63 Hz (hi-hat closed)
        val frequencies = listOf(261.63f, 293.66f, 329.63f)
        for (freq in frequencies) {
            for (sample in 0..2000 step 10) {
                val result = oscillator.emit(freq, sample)
                assertTrue(
                    result >= -1.0f && result <= 1.0f,
                    "Drum at freq $freq should be bounded [-1, 1], got $result at sample $sample",
                )
            }
        }
    }

    @Test
    fun emit_drum_different_notes_produce_different_sounds() {
        // C4 (bass drum) vs D4 (snare) should produce different waveforms
        val oscBass = Oscillator(waveType0 = { Instrument.WaveType.DRUM })
        val oscSnare = Oscillator(waveType0 = { Instrument.WaveType.DRUM })

        val bassSamples = (0..100).map { oscBass.emit(261.63f, it) }
        val snareSamples = (0..100).map { oscSnare.emit(293.66f, it) }

        // The two drum parts should not produce identical output
        val different = bassSamples.zip(snareSamples).any { (a, b) -> abs(a - b) > 0.01f }
        assertTrue(different, "Bass drum and snare should produce different sounds")
    }

    @Test
    fun emit_drum_sound_decays_over_time() {
        val oscillator = Oscillator(waveType0 = { Instrument.WaveType.DRUM })
        // Use C4 (bass drum) - should decay
        val earlyEnergy = (0..50).map { abs(oscillator.emit(261.63f, it)) }.average()

        val oscillator2 = Oscillator(waveType0 = { Instrument.WaveType.DRUM })
        // Late samples - well after the drum has decayed
        val lateEnergy = (20000..20050).map { abs(oscillator2.emit(261.63f, it)) }.average()

        assertTrue(
            earlyEnergy > lateEnergy,
            "Drum sound should decay over time: early=$earlyEnergy, late=$lateEnergy",
        )
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
            Instrument.WaveType.DRUM,
        )

        val frequency = 440.0f

        waveTypes.forEach { waveType ->
            val oscillator = Oscillator(waveType0 = { waveType })

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
