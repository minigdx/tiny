package com.github.minigdx.tiny.sound

import com.github.minigdx.tiny.lua.Note
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class InstrumentPlayerTest {
    private val instrument = Instrument(index = 0, name = "Test Instrument")

    @Test
    fun noteOn_it_turns_note_on() {
        val player = InstrumentPlayer(instrument)
        player.noteOn(Note.C0)
        assertTrue(player.isNoteOn(Note.C0))
        assertFalse(player.isNoteOff(Note.C0))
    }

    @Test
    fun noteOn_multiple_notes_can_be_on() {
        val player = InstrumentPlayer(instrument)
        player.noteOn(Note.C0)
        player.noteOn(Note.D0)
        assertTrue(player.isNoteOn(Note.C0))
        assertTrue(player.isNoteOn(Note.D0))
        assertFalse(player.isNoteOff(Note.C0))
        assertFalse(player.isNoteOff(Note.D0))
    }

    @Test
    fun noteOff_it_turns_note_off() {
        val player = InstrumentPlayer(instrument)
        player.noteOn(Note.C0)
        player.noteOff(Note.C0)
        assertFalse(player.isNoteOn(Note.C0))
        assertTrue(player.isNoteOff(Note.C0))
    }

    @Test
    fun noteOff_without_noteOn_still_marks_as_off() {
        val player = InstrumentPlayer(instrument)
        player.noteOff(Note.C0)
        assertFalse(player.isNoteOn(Note.C0))
        assertTrue(player.isNoteOff(Note.C0))
    }

    @Test
    fun noteOff_multiple_notes_can_be_off() {
        val player = InstrumentPlayer(instrument)
        player.noteOn(Note.C0)
        player.noteOn(Note.D0)
        player.noteOff(Note.C0)
        player.noteOff(Note.D0)
        assertFalse(player.isNoteOn(Note.C0))
        assertFalse(player.isNoteOn(Note.D0))
        assertTrue(player.isNoteOff(Note.C0))
        assertTrue(player.isNoteOff(Note.D0))
    }

    @Test
    fun close_it_turns_all_notes_off() {
        val player = InstrumentPlayer(instrument)
        player.noteOn(Note.C0)
        player.noteOn(Note.D0)
        player.noteOn(Note.E0)
        player.close()
        assertFalse(player.isNoteOn(Note.C0))
        assertFalse(player.isNoteOn(Note.D0))
        assertFalse(player.isNoteOn(Note.E0))
        assertTrue(player.isNoteOff(Note.C0))
        assertTrue(player.isNoteOff(Note.D0))
        assertTrue(player.isNoteOff(Note.E0))
    }

    @Test
    fun close_empty_player_does_not_fail() {
        val player = InstrumentPlayer(instrument)
        player.close()
        assertFalse(player.isNoteOn(Note.C0))
        assertFalse(player.isNoteOff(Note.C0))
    }
}
