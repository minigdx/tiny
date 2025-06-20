package com.github.minigdx.tiny.sound

import com.github.minigdx.tiny.lua.Note
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
            numberOfSamples: Long,
        ): SoundHandler {
            // Return a dummy sound handler for testing
            return object : SoundHandler {
                override fun play() {}

                override fun loop() {}

                override fun stop() {}
            }
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

        // Create a musical sequence with two tracks that would normally cause clipping
        val sequence = MusicalSequence(0)

        // Create two tracks with loud sounds
        val track1 = MusicalSequence.Track(0, 0)
        val track2 = MusicalSequence.Track(1, 1)

        // Create properly configured instruments
        track1.instrument = createTestInstrument(0)
        track2.instrument = createTestInstrument(1)

        // Clear default beats and add our test beats
        track1.beats.clear()
        track2.beats.clear()

        // Add a loud note to each track at the same position
        val note = Note.A4
        track1.beats.add(MusicalNote(note, 0f, 1f, 0.9f))
        track2.beats.add(MusicalNote(note, 0f, 1f, 0.9f))

        sequence.tracks[0] = track1
        sequence.tracks[1] = track2

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

        // Create a sequence with multiple tracks playing the same note
        val sequence = MusicalSequence(0)

        // Create tracks with the same loud note
        val numTracks = 4
        for (i in 0 until numTracks) {
            val track = MusicalSequence.Track(i, i)
            track.instrument = createTestInstrument(i)
            track.beats.clear()
            // Add the same loud note to each track
            track.beats.add(MusicalNote(Note.A4, 0f, 1f, 0.9f))
            sequence.tracks[i] = track
        }

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
        val singleTrackSequence = MusicalSequence(0)
        val singleTrack = MusicalSequence.Track(0, 0)
        singleTrack.instrument = createTestInstrument(0)
        singleTrack.beats.clear()
        singleTrack.beats.add(MusicalNote(Note.A4, 0f, 1f, 0.9f))
        singleTrackSequence.tracks[0] = singleTrack

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
