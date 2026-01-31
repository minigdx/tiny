package com.github.minigdx.tiny.sound

import com.github.minigdx.tiny.lua.Note
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

class MusicalSequenceTest {

    @Test
    fun it_creates_sequence_with_8_tracks() {
        val sequence = MusicalSequence(0)

        assertEquals(8, sequence.tracks.size)
        assertEquals(120, sequence.tempo)
        assertEquals(1f, sequence.volume)
        assertEquals(32, sequence.beatsPerSection)
        assertEquals(8, sequence.sectionCount)
    }

    @Test
    fun it_sets_note_on_track() {
        val sequence = MusicalSequence(0)
        val track = sequence.track(0)!!

        track.setNote(0, 0f, Note.C4, 1f, 0.8f)

        val note = track.getNote(0, 0f)
        assertNotNull(note)
        assertEquals(Note.C4, note.note)
        assertEquals(0f, note.beat)
        assertEquals(1f, note.duration)
        assertEquals(0.8f, note.volume)
    }

    @Test
    fun it_removes_note_from_track() {
        val sequence = MusicalSequence(0)
        val track = sequence.track(0)!!

        track.setNote(0, 0f, Note.C4, 1f)
        assertNotNull(track.getNote(0, 0f))

        track.removeNote(0, 0f, Note.C4)
        assertNull(track.getNote(0, 0f))
    }

    @Test
    fun it_gets_notes_in_section() {
        val sequence = MusicalSequence(0)
        val track = sequence.track(0)!!

        track.setNote(0, 0f, Note.C4, 1f)
        track.setNote(0, 4f, Note.E4, 1f)
        track.setNote(1, 0f, Note.G4, 1f) // Different section

        val sectionNotes = track.getNotesInSection(0)
        assertEquals(2, sectionNotes.size)
    }

    @Test
    fun it_clears_section() {
        val sequence = MusicalSequence(0)
        val track0 = sequence.track(0)!!
        val track1 = sequence.track(1)!!

        track0.setNote(0, 0f, Note.C4, 1f)
        track1.setNote(0, 0f, Note.E4, 1f)

        sequence.clearSection(0)

        assertEquals(0, track0.getNotesInSection(0).size)
        assertEquals(0, track1.getNotesInSection(0).size)
    }

    @Test
    fun it_copies_section() {
        val sequence = MusicalSequence(0)
        val track = sequence.track(0)!!

        track.setNote(0, 0f, Note.C4, 1f)
        track.setNote(0, 4f, Note.E4, 1f)

        sequence.copySectionTo(0, 1)

        val copiedNotes = track.getNotesInSection(1)
        assertEquals(2, copiedNotes.size)
    }

    @Test
    fun it_overwrites_existing_notes() {
        val sequence = MusicalSequence(0)
        val track = sequence.track(0)!!

        track.setNote(0, 0f, Note.C4, 2f)
        track.setNote(0, 0f, Note.E4, 1f) // Should replace C4

        val notes = track.getNotesInSection(0)
        assertEquals(1, notes.size)
        assertEquals(Note.E4, notes[0].note)
    }

    @Test
    fun it_sets_note_volume() {
        val sequence = MusicalSequence(0)
        val track = sequence.track(0)!!

        track.setNote(0, 0f, Note.C4, 1f, 1f)
        track.setNoteVolume(0, 0f, 0.5f)

        val note = track.getNote(0, 0f)
        assertEquals(0.5f, note?.volume)
    }

    @Test
    fun it_manages_current_section() {
        val sequence = MusicalSequence(0)

        assertEquals(0, sequence.currentSectionIndex)

        sequence.currentSectionIndex = 3
        assertEquals(3, sequence.currentSectionIndex)
    }

    @Test
    fun it_manages_track_instrument() {
        val sequence = MusicalSequence(0)
        val track = sequence.track(0)!!

        assertEquals(0, track.instrumentIndex)

        track.instrumentIndex = 2
        assertEquals(2, track.instrumentIndex)
    }

    @Test
    fun it_manages_track_mute() {
        val sequence = MusicalSequence(0)
        val track = sequence.track(0)!!

        assertEquals(false, track.mute)

        track.mute = true
        assertEquals(true, track.mute)
    }
}
