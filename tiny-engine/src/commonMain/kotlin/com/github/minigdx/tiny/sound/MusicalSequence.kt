package com.github.minigdx.tiny.sound

import com.github.minigdx.tiny.BPM
import com.github.minigdx.tiny.Beats
import com.github.minigdx.tiny.Percent
import com.github.minigdx.tiny.lua.Note
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient

/**
 * A MusicalSequence represents a complete music piece with multiple tracks and sections.
 *
 * - A sequence has 8 tracks (each track can have its own instrument)
 * - A sequence is divided into sections (like patterns in a tracker)
 * - Each section has a fixed number of beats (beatsPerSection)
 * - Notes are placed on specific beats within sections
 */
@Serializable
class MusicalSequence(
    val index: Int,
    /**
     * Number of tracks in this sequence (fixed at 8)
     */
    val tracks: Array<Track> = Array(8) { Track(it, 0) },
    /**
     * Global tempo in BPM (Beats Per Minute)
     */
    var tempo: BPM = 120,
    /**
     * Global volume (0.0 to 1.0)
     */
    var volume: Percent = 1f,
    /**
     * Number of beats per section
     */
    var beatsPerSection: Int = 32,
    /**
     * Number of sections in this sequence
     */
    var sectionCount: Int = 8,
    /**
     * Currently selected section for editing
     */
    @Transient
    var currentSectionIndex: Int = 0,
) {
    /**
     * A Track holds notes for a single instrument across all sections.
     */
    @Serializable
    class Track(
        val index: Int = 0,
        var instrumentIndex: Int,
        var mute: Boolean = false,
        @Transient var instrument: Instrument? = null,
        /**
         * Notes organized by section.
         * Key: section index, Value: list of notes in that section
         */
        val notesBySection: MutableMap<Int, MutableList<MusicalNote>> = mutableMapOf(),
        var volume: Percent = 1f,
        // Legacy field for backward compatibility
        val beats: MutableList<MusicalNote> = mutableListOf(),
    ) {
        /**
         * Get all notes in a specific section
         */
        fun getNotesInSection(sectionIndex: Int): List<MusicalNote> {
            return notesBySection[sectionIndex] ?: emptyList()
        }

        /**
         * Set a note at a specific section and beat
         */
        fun setNote(
            sectionIndex: Int,
            beat: Beats,
            note: Note,
            duration: Beats,
            volume: Percent = 1f,
        ) {
            val sectionNotes = notesBySection.getOrPut(sectionIndex) { mutableListOf() }

            // Remove existing notes that overlap with the new note
            val toRemove = sectionNotes.filter { existing ->
                existing.beat < beat + duration && existing.beat + existing.duration > beat
            }
            sectionNotes.removeAll(toRemove)

            // Add the new note
            sectionNotes.add(MusicalNote(note, beat, duration, volume))
        }

        /**
         * Remove a note at a specific section and beat
         */
        fun removeNote(
            sectionIndex: Int,
            beat: Beats,
            note: Note? = null,
        ) {
            val sectionNotes = notesBySection[sectionIndex] ?: return

            val toRemove = sectionNotes.filter { existing ->
                val beatMatches = beat in existing.beat..(existing.beat + existing.duration)
                val noteMatches = note == null || existing.note == note
                beatMatches && noteMatches
            }
            sectionNotes.removeAll(toRemove)
        }

        /**
         * Get a specific note at a section and beat
         */
        fun getNote(
            sectionIndex: Int,
            beat: Beats,
        ): MusicalNote? {
            val sectionNotes = notesBySection[sectionIndex] ?: return null
            return sectionNotes.find { existing ->
                beat in existing.beat..(existing.beat + existing.duration)
            }
        }

        /**
         * Set volume for notes at a specific beat in a section
         */
        fun setNoteVolume(
            sectionIndex: Int,
            beat: Beats,
            volume: Percent,
        ) {
            val sectionNotes = notesBySection[sectionIndex] ?: return
            sectionNotes
                .filter { beat in it.beat..(it.beat + it.duration) }
                .forEach { it.volume = volume }
        }

        /**
         * Clear all notes in a section
         */
        fun clearSection(sectionIndex: Int) {
            notesBySection[sectionIndex]?.clear()
        }

        /**
         * Copy notes from one section to another
         */
        fun copySectionTo(
            fromSection: Int,
            toSection: Int,
        ) {
            val sourceNotes = notesBySection[fromSection] ?: return
            val targetNotes = notesBySection.getOrPut(toSection) { mutableListOf() }
            targetNotes.clear()
            sourceNotes.forEach { note ->
                targetNotes.add(note.copy())
            }
        }
    }

    /**
     * Get a track by index
     */
    fun track(index: Int): Track? = tracks.getOrNull(index)

    /**
     * Get all notes for a specific section across all tracks
     */
    fun getNotesInSection(sectionIndex: Int): Map<Int, List<MusicalNote>> {
        return tracks.associate { track ->
            track.index to track.getNotesInSection(sectionIndex)
        }
    }

    /**
     * Clear a section across all tracks
     */
    fun clearSection(sectionIndex: Int) {
        tracks.forEach { it.clearSection(sectionIndex) }
    }

    /**
     * Copy a section to another section across all tracks
     */
    fun copySectionTo(
        fromSection: Int,
        toSection: Int,
    ) {
        tracks.forEach { it.copySectionTo(fromSection, toSection) }
    }
}
