package com.github.minigdx.tiny.sound

import com.github.minigdx.tiny.BPM
import com.github.minigdx.tiny.Beats
import com.github.minigdx.tiny.lua.Note
import kotlinx.serialization.Serializable
import kotlin.math.min

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

    val endBeat: Float
        get() = min(MAX_BEATS_PER_BAR, beats.maxBy { it.endBeat }.endBeat)

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
    ) {
        val volume = 1f // TODO: change

        // Remove notes that are during this new note
        val toRemoveBeats =
            beats.filter { n -> n.note == note }
                .filter { n -> n.beat in beat..beat + duration }
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
        val toBeRemoved =
            beats.filter { n -> n.note == note }
                .filter { n -> beat in n.beat..(n.beat + n.duration) }

        beats.removeAll(toBeRemoved)
    }

    companion object {
        const val MAX_BEATS_PER_BAR = 32f
    }
}
