package com.github.minigdx.tiny.sound

import com.github.minigdx.tiny.BPM
import com.github.minigdx.tiny.Beats
import com.github.minigdx.tiny.Percent
import com.github.minigdx.tiny.lua.Note
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient

/**
 * A musical phrase is holding musical notes.
 * A musical phrase is holding up to 32 beats.
 * A musical phrase without an instrument will not be played.
 * A musical phrase will play only [duration] beats
 *
 * If the last note duration is longer the remaining time,
 * the note will be cut.
 */
@Serializable
class MusicalPhrase(
    var index: Int = 1,
    @Transient var instrument: Instrument? = null,
    /**
     * Index of the instrument. Used for the serialization and deserialization.
     */
    var instrumentIndex: Int = -1,
    /**
     * BPM (Beats Per Minute) of the bar.
     */
    var tempo: BPM = 120,
    /**
     * Name of the SFX
     */
    var name: String? = "SFX $index",
    var volume: Percent = 1.0f,
    var mute: Boolean = false,
    /**
     * Duration of the phase.
     *
     * Only the number of [duration] beats will be played before being looped again,
     *
     * For example: if a [duration] is 12, then beats after the 12 will be ignored, when played.
     */
    var duration: Int = 32,
) {
    /**
     * Backing storage for all beats. Serialized as `beats` for backward compatibility.
     * Prefer using [beats] for reading (filtered by [duration]) and the dedicated
     * mutation methods ([setBeatAt], [resetBeats], [addBeat]) instead of mutating
     * this list directly.
     */
    @SerialName("beats")
    val allBeats: MutableList<MusicalNote> = mutableListOf()

    /**
     * Beats that will be played, limited to the first [duration] beats.
     */
    val beats: List<MusicalNote>
        get() = allBeats.take(duration)

    /**
     * Append a beat to the backing storage. Intended for phrase initialization.
     */
    fun addBeat(note: MusicalNote) {
        allBeats.add(note)
    }

    /**
     * Replace the beat at [index] in the backing storage.
     * No-op if [index] is out of bounds.
     */
    fun setBeatAt(
        index: Int,
        note: MusicalNote,
    ) {
        if (index in allBeats.indices) {
            allBeats[index] = note
        }
    }

    /**
     * Reset every stored beat to a default silent beat.
     */
    fun resetBeats() {
        allBeats.indices.forEach { i ->
            allBeats[i] = MusicalNote(null, i.toFloat(), 1f, 1f)
        }
    }

    private fun notesOnTheBeat(
        beat: Beats,
        duration: Beats,
    ): List<MusicalNote> {
        return allBeats
            .filter { note1 ->
                note1.beat < beat + duration && note1.beat + note1.duration > beat
            }
    }

    /**
     * Set all notes of the musical bar.
     */
    fun setNotes(notes: List<MusicalNote>) {
        allBeats.clear()
        allBeats.addAll(notes)
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
        // Remove notes that are during this new note
        val toRemoveBeats = notesOnTheBeat(beat, duration)
            .filter { n -> uniqueOnBeat || n.note == note }

        // Keep the previous volume
        val volume = toRemoveBeats.firstOrNull()?.volume ?: 1f

        allBeats.removeAll(toRemoveBeats)

        // Save the new note
        allBeats.add(MusicalNote(note, beat, duration, volume))
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

        allBeats.removeAll(toBeRemoved)
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
}
