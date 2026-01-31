package com.github.minigdx.tiny.lua.sfx

import com.github.minigdx.tiny.lua.Note
import com.github.minigdx.tiny.lua.WrapperLuaTable
import com.github.minigdx.tiny.resources.Sound
import com.github.minigdx.tiny.sound.MusicalSequence
import com.github.minigdx.tiny.sound.VirtualSoundBoard
import org.luaj.vm2.LuaTable

/**
 * Lua wrapper for a Section within a MusicalSequence.
 *
 * A section is a vertical slice of the sequence at a specific section index,
 * providing access to all notes across all tracks at that section.
 *
 * Provides:
 * - Section playback
 * - Note playback at specific positions
 * - Access to notes across all tracks
 * - Section manipulation (clear, copy)
 */
class SectionLuaWrapper(
    private val origin: Sound,
    private val sequence: MusicalSequence,
    private val sectionIndex: Int,
    private val soundBoard: VirtualSoundBoard,
) : WrapperLuaTable() {

    init {
        // Read-only section index
        wrap("index") { valueOf(sectionIndex) }

        // Play this section
        function0("play") {
            // Play the section using the sound board
            val handler = soundBoard.prepare(sequence).also { it.play() }
            val result = WrapperLuaTable()
            result.function0("stop") {
                handler.stop()
                NONE
            }
            result
        }

        // Play a specific note at a position: play_note({track, beat})
        function1("play_note") { arg ->
            val trackIndex = arg["track"].checkint()
            val beat = arg["beat"].checkdouble().toFloat()

            val track = sequence.tracks.getOrNull(trackIndex) ?: return@function1 NIL
            val musicalNote = track.getNote(sectionIndex, beat) ?: return@function1 NIL

            val note = musicalNote.note ?: return@function1 NIL
            val instrument = track.instrument
                ?: origin.data.music.instruments.getOrNull(track.instrumentIndex)
                ?: return@function1 NIL

            soundBoard.noteOn(note, instrument)

            // Return a handle to stop the note
            val result = WrapperLuaTable()
            result.function0("stop") {
                soundBoard.noteOff(note)
                NONE
            }
            result
        }

        // Get all notes in this section for a specific track
        function1("notes") { trackArg ->
            val trackIndex = trackArg.checkint()
            val track = sequence.tracks.getOrNull(trackIndex) ?: return@function1 NIL

            val notes = track.getNotesInSection(sectionIndex)
            val result = LuaTable()

            notes.sortedBy { it.beat }
                .forEachIndexed { index, musicalNote ->
                    val noteTable = LuaTable().apply {
                        set("note", musicalNote.note?.name?.let { valueOf(it) } ?: NIL)
                        set("notei", musicalNote.note?.index?.let { valueOf(it) } ?: NIL)
                        set("octave", musicalNote.note?.octave?.let { valueOf(it) } ?: NIL)
                        set("beat", valueOf(musicalNote.beat.toDouble()))
                        set("duration", valueOf(musicalNote.duration.toDouble()))
                        set("volume", valueOf(musicalNote.volume.toDouble()))
                    }
                    result.insert(index + 1, noteTable)
                }
            result
        }

        // Get all notes in this section across all tracks
        wrap("all_notes") {
            val allNotes = sequence.getNotesInSection(sectionIndex)
            val result = LuaTable()

            allNotes.forEach { (trackIndex, notes) ->
                val trackTable = LuaTable()
                notes.sortedBy { it.beat }
                    .forEachIndexed { index, musicalNote ->
                        val noteTable = LuaTable().apply {
                            set("note", musicalNote.note?.name?.let { valueOf(it) } ?: NIL)
                            set("notei", musicalNote.note?.index?.let { valueOf(it) } ?: NIL)
                            set("octave", musicalNote.note?.octave?.let { valueOf(it) } ?: NIL)
                            set("beat", valueOf(musicalNote.beat.toDouble()))
                            set("duration", valueOf(musicalNote.duration.toDouble()))
                            set("volume", valueOf(musicalNote.volume.toDouble()))
                        }
                        trackTable.insert(index + 1, noteTable)
                    }
                result.set(trackIndex + 1, trackTable)
            }
            result
        }

        // Clear this section (optionally for a specific track)
        function1("clear") { trackArg ->
            if (trackArg.isnil()) {
                // Clear all tracks in this section
                sequence.clearSection(sectionIndex)
            } else {
                // Clear only the specified track
                val trackIndex = trackArg.checkint()
                sequence.tracks.getOrNull(trackIndex)?.clearSection(sectionIndex)
            }
            NONE
        }

        // Clear all tracks in this section (no args version)
        function0("clear_all") {
            sequence.clearSection(sectionIndex)
            NONE
        }

        // Copy this section to another section index
        function1("copy_to") { targetArg ->
            val targetSection = targetArg.checkint()
            sequence.copySectionTo(sectionIndex, targetSection)
            NONE
        }
    }
}
