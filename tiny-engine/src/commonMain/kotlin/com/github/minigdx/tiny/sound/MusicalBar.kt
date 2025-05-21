package com.github.minigdx.tiny.sound

import com.github.minigdx.tiny.BPM
import com.github.minigdx.tiny.Beats
import com.github.minigdx.tiny.Percent
import com.github.minigdx.tiny.lua.Note
import kotlinx.serialization.Serializable

/**
 * A musical bar is holding musical notes.
 * A musical bar is holding 32 beats.
 * A musical bar without an instrument will not be played.
 *
 * If the last note duration is longer the remaining time,
 * the note will be cut.
 */
@Serializable
class MusicalBar(
    var index: Int = 1,
    var instrument: Instrument? = null,
    /**
     * If part of a Sequence, this BPM will be controlled by the Sequence.
     */
    var tempo: BPM = 120,
) {
    val beats: MutableList<MusicalNote> = mutableListOf()

    private fun notesOnTheBeat(
        beat: Beats,
        duration: Beats,
    ): List<MusicalNote> {
        return beats
            .filter { note1 ->
                note1.beat < beat + duration && note1.beat + note1.duration > beat
            }
    }

    /**
     * Set all notes of the musical bar.
     */
    fun setNotes(notes: List<MusicalNote>) {
        beats.clear()
        beats.addAll(notes)
    }

    /**
     * Set the note on a specific beat in the bar
     * Remove the same note that happen during this new note
     */
    fun setNote(
        note: Note,
        beat: Beats,
        duration: Beats,
        uniqueOnBeat: Boolean = false,
    ) {
        val volume = 1f

        // Remove notes that are during this new note
        val toRemoveBeats = notesOnTheBeat(beat, duration)
            .filter { n -> uniqueOnBeat || n.note == note }
        beats.removeAll(toRemoveBeats)

        // Save the new note
        beats.add(MusicalNote(note, beat, duration, volume))
    }

    /**
     * Remove a note at a beat.
     * Doing nothing if there is no note.
     */
    fun removeNote(
        note: Note,
        beat: Beats,
    ) {
        val toBeRemoved = notesOnTheBeat(beat, 0.5f)
            .filter { n -> n.note == note }
            .filter { n -> beat in n.beat..(n.beat + n.duration) }

        beats.removeAll(toBeRemoved)
    }

    fun setVolume(
        beat: Beats,
        volume: Percent,
    ) {
        val notes = notesOnTheBeat(beat, 0.5f)
        notes.forEach { n ->
            n.volume = volume
        }
    }

    companion object {
        const val MAX_BEATS_PER_BAR = 32f
    }
}
