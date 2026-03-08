package com.github.minigdx.tiny.sound

import com.github.minigdx.tiny.lua.Note
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class MusicGeneratorTest {
    private fun createSequence(): MusicalSequence = MusicalSequence(0)

    @Test
    fun determinism_same_config_produces_identical_beats() {
        val config = MusicConfiguration(seed = 123L)
        val seq1 = createSequence()
        val seq2 = createSequence()

        MusicGenerator.generate(seq1, config)
        MusicGenerator.generate(seq2, config)

        for (trackIdx in 0..3) {
            val beats1 = seq1.tracks[trackIdx].beats
            val beats2 = seq2.tracks[trackIdx].beats
            assertEquals(beats1.size, beats2.size, "Track $trackIdx beat count mismatch")
            for (i in beats1.indices) {
                assertEquals(
                    beats1[i].note,
                    beats2[i].note,
                    "Track $trackIdx beat $i note mismatch",
                )
                assertEquals(
                    beats1[i].volume,
                    beats2[i].volume,
                    "Track $trackIdx beat $i volume mismatch",
                )
            }
        }
    }

    @Test
    fun different_seeds_produce_different_lead_track() {
        val config1 = MusicConfiguration(seed = 1L)
        val config2 = MusicConfiguration(seed = 999L)
        val seq1 = createSequence()
        val seq2 = createSequence()

        MusicGenerator.generate(seq1, config1)
        MusicGenerator.generate(seq2, config2)

        // Lead track (index 2) should differ with different seeds
        val lead1 = seq1.tracks[2].beats.mapNotNull { it.note }
        val lead2 = seq2.tracks[2].beats.mapNotNull { it.note }
        assertTrue(lead1 != lead2, "Lead tracks should differ with different seeds")

        // Chord track (index 0) should be identical regardless of seed
        val chords1 = seq1.tracks[0].beats.map { it.note }
        val chords2 = seq2.tracks[0].beats.map { it.note }
        assertEquals(chords1, chords2, "Chord tracks should be identical with different seeds")

        // Bass track (index 1) should be identical regardless of seed
        val bass1 = seq1.tracks[1].beats.map { it.note }
        val bass2 = seq2.tracks[1].beats.map { it.note }
        assertEquals(bass1, bass2, "Bass tracks should be identical with different seeds")

        // Drum track (index 3) should be identical regardless of seed
        val drums1 = seq3drums(seq1)
        val drums2 = seq3drums(seq2)
        assertEquals(drums1, drums2, "Drum tracks should be identical with different seeds")
    }

    private fun seq3drums(seq: MusicalSequence) = seq.tracks[3].beats.map { it.note }

    @Test
    fun each_track_has_33_beats() {
        val config = MusicConfiguration()
        val seq = createSequence()
        MusicGenerator.generate(seq, config)

        for (trackIdx in 0..3) {
            assertEquals(33, seq.tracks[trackIdx].beats.size, "Track $trackIdx should have 33 beats")
        }
    }

    @Test
    fun generated_notes_within_valid_range() {
        val config = MusicConfiguration(seed = 42L)
        val seq = createSequence()
        MusicGenerator.generate(seq, config)

        for (trackIdx in 0..3) {
            seq.tracks[trackIdx].beats.forEach { beat ->
                if (beat.note != null) {
                    val note = beat.note!!
                    assertTrue(note.index in 1..108, "Note index ${note.index} out of range")
                    assertTrue(beat.volume in 0f..1f, "Volume ${beat.volume} out of range")
                }
            }
        }
    }

    @Test
    fun chord_track_uses_configured_instrument_and_volume() {
        val config = MusicConfiguration(chordInstrument = 5, chordVolume = 0.7f)
        val seq = createSequence()
        MusicGenerator.generate(seq, config)

        assertEquals(5, seq.tracks[0].instrumentIndex)
        assertEquals(0.7f, seq.tracks[0].volume)
    }

    @Test
    fun bass_track_uses_configured_instrument_and_volume() {
        val config = MusicConfiguration(bassInstrument = 4, bassVolume = 0.6f)
        val seq = createSequence()
        MusicGenerator.generate(seq, config)

        assertEquals(4, seq.tracks[1].instrumentIndex)
        assertEquals(0.6f, seq.tracks[1].volume)
    }

    @Test
    fun lead_track_uses_configured_instrument_and_volume() {
        val config = MusicConfiguration(leadInstrument = 2, leadVolume = 0.5f)
        val seq = createSequence()
        MusicGenerator.generate(seq, config)

        assertEquals(2, seq.tracks[2].instrumentIndex)
        assertEquals(0.5f, seq.tracks[2].volume)
    }

    @Test
    fun drum_track_uses_configured_instrument_and_volume() {
        val config = MusicConfiguration(drumInstrument = 3, drumVolume = 0.8f)
        val seq = createSequence()
        MusicGenerator.generate(seq, config)

        assertEquals(3, seq.tracks[3].instrumentIndex)
        assertEquals(0.8f, seq.tracks[3].volume)
    }

    @Test
    fun tempo_is_set_from_config() {
        val config = MusicConfiguration(bpm = 140)
        val seq = createSequence()
        MusicGenerator.generate(seq, config)

        assertEquals(140, seq.tempo)
    }

    @Test
    fun drum_pattern_places_correct_notes() {
        val config = MusicConfiguration(drumPattern = "Rock")
        val seq = createSequence()
        MusicGenerator.generate(seq, config)

        val drums = seq.tracks[3]
        // Rock pattern: kick on 0,4,8,12,...  snare on 2,6,10,14,...  hihat on all others
        val kickNote = Note.fromName("C2")
        val snareNote = Note.fromName("C4")
        val hihatNote = Note.fromName("C6")

        assertEquals(kickNote, drums.beats[0].note, "Beat 0 should be kick")
        assertEquals(hihatNote, drums.beats[1].note, "Beat 1 should be hihat")
        assertEquals(snareNote, drums.beats[2].note, "Beat 2 should be snare")
        assertEquals(hihatNote, drums.beats[3].note, "Beat 3 should be hihat")
        assertEquals(kickNote, drums.beats[4].note, "Beat 4 should be kick")
    }

    @Test
    fun all_scales_produce_valid_output() {
        for (scaleName in MusicGenerator.SCALE_NAMES) {
            val config = MusicConfiguration(scaleName = scaleName, seed = 42L)
            val seq = createSequence()
            MusicGenerator.generate(seq, config)

            // All tracks should have notes
            for (trackIdx in 0..3) {
                val hasNotes = seq.tracks[trackIdx].beats.any { it.note != null }
                assertTrue(hasNotes, "Scale '$scaleName' track $trackIdx should have notes")
            }
        }
    }

    @Test
    fun all_progressions_produce_valid_output() {
        for (progName in MusicGenerator.PROGRESSION_NAMES) {
            val config = MusicConfiguration(progressionName = progName, seed = 42L)
            val seq = createSequence()
            MusicGenerator.generate(seq, config)

            val hasChords = seq.tracks[0].beats.any { it.note != null }
            assertTrue(hasChords, "Progression '$progName' should produce chord notes")
        }
    }

    @Test
    fun serialization_round_trip_with_configuration() {
        val config = MusicConfiguration(
            root = "D",
            scaleName = "Minor",
            progressionName = "Melancholy",
            leadStyle = "Bouncy",
            drumPattern = "Dance",
            chordInstrument = 1,
            bassInstrument = 4,
            leadInstrument = 5,
            drumInstrument = 3,
            chordVolume = 0.4f,
            bassVolume = 0.5f,
            leadVolume = 0.3f,
            drumVolume = 0.6f,
            bpm = 100,
            seed = 777L,
        )

        val music = Music()
        val seq = music.sequences[0]
        MusicGenerator.generate(seq, config)
        seq.configuration = config

        // Capture expected notes before serialization
        val expectedNotes = seq.tracks.map { track ->
            track.beats.map { it.note to it.volume }
        }

        // Serialize (clears beats for config-based sequences)
        val json = music.serialize()

        // Deserialize
        val restored = Music.deserialize(json)
        val restoredSeq = restored.sequences[0]

        // Config should be preserved
        assertNotNull(restoredSeq.configuration)
        assertEquals(config, restoredSeq.configuration)

        // Regenerate from config
        MusicGenerator.generate(restoredSeq, restoredSeq.configuration!!)

        // Notes should match
        for (trackIdx in 0..3) {
            val restoredBeats = restoredSeq.tracks[trackIdx].beats
            for (i in expectedNotes[trackIdx].indices) {
                if (i < restoredBeats.size) {
                    assertEquals(
                        expectedNotes[trackIdx][i].first,
                        restoredBeats[i].note,
                        "Track $trackIdx beat $i note mismatch after round-trip",
                    )
                }
            }
        }
    }

    @Test
    fun serialization_without_configuration_preserves_beats() {
        val music = Music()
        val seq = music.sequences[0]
        // Manually set a note without configuration
        seq.tracks[0].beats[0] = MusicalNote(Note.C4, 0f, 1f, 0.8f)

        val json = music.serialize()
        val restored = Music.deserialize(json)

        assertNull(restored.sequences[0].configuration)
    }

    @Test
    fun config_based_serialization_reduces_size() {
        val config = MusicConfiguration(seed = 42L)

        // Music with config-based sequence
        val musicWithConfig = Music()
        MusicGenerator.generate(musicWithConfig.sequences[0], config)
        musicWithConfig.sequences[0].configuration = config
        val jsonWithConfig = musicWithConfig.serialize()

        // Music without config (all beats serialized)
        val musicWithoutConfig = Music()
        MusicGenerator.generate(musicWithoutConfig.sequences[0], config)
        val jsonWithoutConfig = musicWithoutConfig.serialize()

        assertTrue(
            jsonWithConfig.length < jsonWithoutConfig.length,
            "Config-based serialization should be smaller: " +
                "${jsonWithConfig.length} vs ${jsonWithoutConfig.length}",
        )
    }

    @Test
    fun serialized_json_contains_configuration_fields() {
        val config = MusicConfiguration(
            root = "D",
            scaleName = "Minor",
            bpm = 100,
            seed = 777L,
        )

        val music = Music()
        MusicGenerator.generate(music.sequences[0], config)
        music.sequences[0].configuration = config

        val json = music.serialize()

        // Configuration key must be present
        assertTrue(json.contains("\"configuration\""), "JSON must contain 'configuration' key")
        // Key config fields must be serialized
        assertTrue(json.contains("\"root\""), "JSON must contain 'root' field. JSON: $json")
        assertTrue(json.contains("\"scaleName\""), "JSON must contain 'scaleName' field")
        assertTrue(json.contains("\"seed\""), "JSON must contain 'seed' field")
        assertTrue(json.contains("\"bpm\""), "JSON must contain 'bpm' field")
    }

    @Test
    fun unused_sequences_have_no_beats_in_json() {
        val music = Music()
        // Only generate for sequence 0
        val config = MusicConfiguration(seed = 42L)
        MusicGenerator.generate(music.sequences[0], config)
        music.sequences[0].configuration = config

        val json = music.serialize()

        // Count occurrences of "note":null - should be minimal
        val nullNoteCount = "\"note\":null".toRegex().findAll(json).count()
        assertTrue(
            nullNoteCount == 0,
            "Unused sequences should not serialize null-note beats, found $nullNoteCount",
        )
    }
}
