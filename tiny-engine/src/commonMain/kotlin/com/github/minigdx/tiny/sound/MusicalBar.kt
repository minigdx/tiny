package com.github.minigdx.tiny.sound

import com.github.minigdx.tiny.BPM
import com.github.minigdx.tiny.Percent
import com.github.minigdx.tiny.Seconds
import com.github.minigdx.tiny.lua.Note
import kotlinx.serialization.Serializable


/**
 * A musical bar is holding musical notes.
 * A musical bar is holding 32 beats.
 * A musical bar without an instrument will not be played.
 *
 * If the last note duration is longuer the remaining time,
 * the note will be cut.
 */
@Serializable
class MusicalBar(
    var instrument: Instrument? = null,
    /**
     * If part of a Sequence, this BPM will be controlled by the Sequence.
     */
    var tempo: BPM = 120,
) {

    val beats: MutableList<MusicalNote> = mutableListOf()

    val endBeat: Float
        get() = beats.maxBy { it.endBeat }.endBeat

    fun setNotes(notes: List<MusicalNote>) {
        beats.clear()
        beats.addAll(notes)
    }
}