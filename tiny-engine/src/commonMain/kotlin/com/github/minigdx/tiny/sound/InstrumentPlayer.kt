package com.github.minigdx.tiny.sound

import com.github.minigdx.tiny.input.internal.ObjectPool
import com.github.minigdx.tiny.lua.Note

class InstrumentPlayer(private val instrument: Instrument) {

    class NoteProgress(
        var note: Note = Note.C0, // note managed
        var progress: Long = 0, // progress of the note playing
    ) {
        override fun equals(other: Any?): Boolean {
            return (other as? NoteProgress)?.note == note
        }

        override fun hashCode(): Int {
            return note.hashCode()
        }
    }

    private val notesOn = mutableSetOf<NoteProgress>()
    private val notesOff = mutableSetOf<NoteProgress>()

    private val noteProgressPool = object : ObjectPool<NoteProgress>(100) {
        override fun newInstance(): NoteProgress {
            return NoteProgress()
        }

        override fun destroyInstance(obj: NoteProgress) {
            obj.progress = 0
        }
    }

    fun noteOn(note: Note) {
        val progress = noteProgressPool.obtain()
        progress.note = note
        notesOn.add(progress)
    }

    fun noteOff(note: Note) {
        val progress = noteProgressPool.obtain()
        progress.note = note
        notesOn.remove(progress)
        notesOff.add(progress)
    }

    fun isNoteOn(note: Note): Boolean {
        val progress = noteProgressPool.obtain()
        progress.note = note
        return notesOn.contains(progress)
    }

    fun isNoteOff(note: Note): Boolean {
        val progress = noteProgressPool.obtain()
        progress.note = note
        return notesOff.contains(progress)
    }

    fun close() {
        notesOn.forEach { it.progress = 0 }
        notesOff.addAll(notesOn)
        notesOn.clear()
    }
}
