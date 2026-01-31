package com.github.minigdx.tiny.lua.sfx

import com.github.minigdx.tiny.lua.Note
import com.github.minigdx.tiny.lua.WrapperLuaTable
import com.github.minigdx.tiny.resources.Sound
import com.github.minigdx.tiny.sound.MusicalSequence
import com.github.minigdx.tiny.sound.VirtualSoundBoard
import org.luaj.vm2.LuaTable

/**
 * Lua wrapper for a Track within a MusicalSequence.
 *
 * Provides access to:
 * - Track settings (instrument, mute, volume)
 * - Note editing (set_note, remove_note, note)
 */
class TrackLuaWrapper(
    private val origin: Sound,
    private val sequence: MusicalSequence,
    private val track: MusicalSequence.Track,
    private val soundBoard: VirtualSoundBoard,
) : WrapperLuaTable() {

    init {
        // Read-only index
        wrap("index") { valueOf(track.index) }

        // Instrument index
        wrap(
            "instrument",
            { valueOf(track.instrumentIndex) },
            {
                val index = it.checkint()
                track.instrumentIndex = index
                track.instrument = origin.data.music.instruments.getOrNull(index)
            },
        )

        // Mute state
        wrap(
            "mute",
            { valueOf(track.mute) },
            { track.mute = it.checkboolean() },
        )

        // Track volume
        wrap(
            "volume",
            { valueOf(track.volume.toDouble()) },
            { track.volume = it.checkdouble().toFloat().coerceIn(0f, 1f) },
        )

        // Set a note: set_note({section, beat, note, duration, volume})
        function1("set_note") { arg ->
            val sectionIndex = arg["section"].optint(sequence.currentSectionIndex)
            val beat = arg["beat"].checkdouble().toFloat()
            val noteName = arg["note"].checkjstring()
            val duration = arg["duration"].optdouble(1.0).toFloat()
            val volume = arg["volume"].optdouble(1.0).toFloat().coerceIn(0f, 1f)

            val note = Note.fromName(noteName)
            track.setNote(sectionIndex, beat, note, duration, volume)

            NONE
        }

        // Remove a note: remove_note({section, beat, note?})
        function1("remove_note") { arg ->
            val sectionIndex = arg["section"].optint(sequence.currentSectionIndex)
            val beat = arg["beat"].checkdouble().toFloat()
            val noteName = arg["note"].optjstring(null)

            val note = noteName?.let { Note.fromName(it) }
            track.removeNote(sectionIndex, beat, note)

            NONE
        }

        // Get note data at a specific position: note(section, beat)
        function2("note") { sectionArg, beatArg ->
            val sectionIndex = sectionArg.optint(sequence.currentSectionIndex)
            val beat = beatArg.checkdouble().toFloat()

            val musicalNote = track.getNote(sectionIndex, beat) ?: return@function2 NIL

            LuaTable().apply {
                set("note", musicalNote.note?.name?.let { valueOf(it) } ?: NIL)
                set("notei", musicalNote.note?.index?.let { valueOf(it) } ?: NIL)
                set("octave", musicalNote.note?.octave?.let { valueOf(it) } ?: NIL)
                set("beat", valueOf(musicalNote.beat.toDouble()))
                set("duration", valueOf(musicalNote.duration.toDouble()))
                set("volume", valueOf(musicalNote.volume.toDouble()))
            }
        }

        // Get all notes in a section for this track
        function1("notes") { sectionArg ->
            val sectionIndex = sectionArg.optint(sequence.currentSectionIndex)
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

        // Set volume for a note: set_volume({section, beat, volume})
        function1("set_volume") { arg ->
            val sectionIndex = arg["section"].optint(sequence.currentSectionIndex)
            val beat = arg["beat"].checkdouble().toFloat()
            val volume = arg["volume"].checkdouble().toFloat().coerceIn(0f, 1f)

            track.setNoteVolume(sectionIndex, beat, volume)

            NONE
        }

        // Clear all notes in a section for this track
        function1("clear") { sectionArg ->
            val sectionIndex = sectionArg.optint(sequence.currentSectionIndex)
            track.clearSection(sectionIndex)
            NONE
        }

        // Copy notes from one section to another
        function2("copy_to") { fromArg, toArg ->
            val fromSection = fromArg.checkint()
            val toSection = toArg.checkint()
            track.copySectionTo(fromSection, toSection)
            NONE
        }
    }
}
