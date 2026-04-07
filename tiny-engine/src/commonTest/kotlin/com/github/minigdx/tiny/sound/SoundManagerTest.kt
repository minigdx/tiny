package com.github.minigdx.tiny.sound

import com.github.minigdx.tiny.lua.Note
import com.github.minigdx.tiny.util.FloatData
import kotlin.math.sqrt
import kotlin.test.Test
import kotlin.test.assertTrue

class SoundManagerTest {
    /**
     * Test class that extends SoundManager for testing purposes
     */
    private class TestSoundManager : SoundManager() {
        override fun initSoundManager(inputHandler: com.github.minigdx.tiny.input.InputHandler) {
            // No-op for testing
        }

        override fun createSoundHandler(
            buffer: FloatArray,
            loopStartSample: Int,
        ): SoundHandler {
            // Return a dummy sound handler for testing
            return object : SoundHandler {
                override fun play() {}

                override fun loop() {}

                override fun stop() {}

                override fun isPlaying(): Boolean = false

                override fun nextChunk(samples: Int): FloatData {
                    TODO("Not yet implemented")
                }
            }
        }

        override fun noteOn(
            note: Note,
            instrument: Instrument,
        ) {
            TODO("Not yet implemented")
        }

        override fun noteOff(note: Note) {
            TODO("Not yet implemented")
        }
    }

    /**
     * Calculate RMS manually for testing purposes
     */
    private fun calculateRms(buffer: FloatArray): Float {
        if (buffer.isEmpty()) return 0f

        var sumOfSquares = 0f
        for (sample in buffer) {
            sumOfSquares += sample * sample
        }

        return sqrt(sumOfSquares / buffer.size)
    }

    /**
     * Create a properly configured instrument for testing
     */
    private fun createTestInstrument(index: Int): Instrument {
        val instrument = Instrument(index, "Test $index", Instrument.WaveType.SINE)

        // Set up harmonics to generate sound
        instrument.harmonics[0] = 1.0f // Set fundamental harmonic to full amplitude

        // Set up ADSR envelope
        instrument.attack = 0.01f
        instrument.decay = 0.1f
        instrument.sustain = 0.8f
        instrument.release = 0.1f

        return instrument
    }

    /**
     * Create a MusicalPhrase configured as a track channel (with a single beat).
     */
    private fun createTrackPhrase(
        index: Int,
        instrumentIndex: Int,
        instrument: Instrument,
        note: Note,
        volume: Float = 0.9f,
    ): MusicalPhrase {
        return MusicalPhrase(index = index, instrumentIndex = instrumentIndex).also {
            it.instrument = instrument
            it.beats.add(MusicalNote(note, 0f, 1f, volume))
        }
    }

    /**
     * Build a MusicalSequence from an array of track phrases.
     */
    private fun buildSequence(vararg phrases: MusicalPhrase): MusicalSequence {
        val pattern = MusicalPattern(
            index = 0,
            tracks = Array(phrases.size) { phrases[it] },
        )
        return MusicalSequence(
            index = 0,
            patterns = arrayOf(pattern),
        )
    }

    @Test
    fun testInstrumentProducesSound() {
        // First, let's verify that our instrument setup actually produces sound
        val instrument = createTestInstrument(0)

        // Generate a simple sound sample
        val sampleRate = SoundManager.SAMPLE_RATE
        val frequency = Note.A4.frequency
        val duration = 0.1f // 100ms
        val numSamples = (duration * sampleRate).toInt()

        var hasNonZeroSample = false
        for (i in 0 until numSamples) {
            val time = i.toFloat() / sampleRate
            val sample = instrument.generate(frequency, time)
            if (sample != 0f) {
                hasNonZeroSample = true
                break
            }
        }

        assertTrue(hasNonZeroSample, "Instrument should generate non-zero samples")
    }

    @Test
    fun testMixingPreventsClipping() {
        val soundManager = TestSoundManager()

        val note = Note.A4
        val phrase1 = createTrackPhrase(0, 0, createTestInstrument(0), note)
        val phrase2 = createTrackPhrase(1, 1, createTestInstrument(1), note)
        val sequence = buildSequence(phrase1, phrase2)

        // Convert the sequence to audio samples
        val result = soundManager.convert(sequence)

        // Check if we have any non-zero samples
        var hasNonZeroSample = false
        for (sample in result) {
            if (sample != 0f) {
                hasNonZeroSample = true
                break
            }
        }

        assertTrue(hasNonZeroSample, "Sound conversion should produce non-zero samples")

        // Verify that no sample exceeds the valid range (-1.0 to 1.0)
        for (sample in result) {
            assertTrue(sample >= -1.0f && sample <= 1.0f, "Sample value $sample exceeds valid range")
        }

        // Verify that the RMS-based mixing produces a reasonable result
        // The mixed output should have a reasonable amplitude (not too quiet)
        val mixedRms = calculateRms(result)
        assertTrue(mixedRms > 0.01f, "Mixed output is too quiet: RMS = $mixedRms")
    }

    @Test
    fun testMultipleTracksWithSameNote() {
        val soundManager = TestSoundManager()

        // Create a sequence with 4 tracks playing the same note
        val note = Note.A4
        val phrases = Array(4) { i ->
            createTrackPhrase(i, i, createTestInstrument(i), note)
        }
        val sequence = buildSequence(*phrases)

        // Convert the sequence to audio samples
        val result = soundManager.convert(sequence)

        // Check if we have any non-zero samples
        var hasNonZeroSample = false
        for (sample in result) {
            if (sample != 0f) {
                hasNonZeroSample = true
                break
            }
        }

        assertTrue(hasNonZeroSample, "Sound conversion should produce non-zero samples")

        // Verify that no sample exceeds the valid range (-1.0 to 1.0)
        for (sample in result) {
            assertTrue(sample >= -1.0f && sample <= 1.0f, "Sample value $sample exceeds valid range")
        }

        // Calculate RMS of the mixed result
        val mixedRms = calculateRms(result)

        // The RMS should be reasonable (not too quiet) despite having multiple tracks
        assertTrue(mixedRms > 0.01f, "Mixed output is too quiet: RMS = $mixedRms")

        // Create a sequence with just one track for comparison
        val singlePhrase = createTrackPhrase(0, 0, createTestInstrument(0), note)
        val singleTrackSequence = buildSequence(singlePhrase)

        // Convert the single track sequence
        val singleTrackResult = soundManager.convert(singleTrackSequence)

        // Calculate RMS of the single track result
        val singleTrackRms = calculateRms(singleTrackResult)

        // The multi-track RMS should be comparable to the single track RMS
        // This verifies that our RMS-based mixing is working correctly
        if (singleTrackRms > 0.001f) {
            assertTrue(
                mixedRms >= singleTrackRms * 0.5f,
                "Multi-track RMS ($mixedRms) is too low compared to single track RMS ($singleTrackRms)",
            )
        }
    }
}
