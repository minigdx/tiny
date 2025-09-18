package com.github.minigdx.tiny.sound

import com.github.minigdx.tiny.Sample
import com.github.minigdx.tiny.input.internal.ObjectPool
import com.github.minigdx.tiny.lua.Note
import com.github.minigdx.tiny.sound.SoundManager.Companion.MASTER_VOLUME
import com.github.minigdx.tiny.sound.SoundManager.Companion.SAMPLE_RATE
import kotlin.math.max
import kotlin.math.min

class InstrumentPlayer(private val instrument: Instrument) {
    class NoteProgress(
        // note managed
        var note: Note = Note.C0,
        // progress of the note playing
        var noteOnProgress: Sample = 0,
        var noteOffProgress: Sample = 0,
        // level at the moment noteOff was called
        var releaseStartLevel: Float = 0f,
    ) {
        override fun equals(other: Any?): Boolean {
            return (other as? NoteProgress)?.note == note
        }

        override fun hashCode(): Int {
            return note.hashCode()
        }

        fun isCompleted(release: Sample): Boolean {
            return noteOffProgress > release
        }

        override fun toString(): String {
            return "$note - $noteOnProgress - $noteOffProgress"
        }
    }

    private val envelop = Envelop(
        attack0 = { (instrument.attack * SAMPLE_RATE).toInt() },
        decay0 = { (instrument.decay * SAMPLE_RATE).toInt() },
        sustain0 = { instrument.sustain },
        release0 = { (instrument.release * SAMPLE_RATE).toInt() },
    )

    private val harmonizer = Harmonizer({ instrument.harmonics })

    private val oscillator = Oscillator({ instrument.wave })

    private val notesOn = mutableSetOf<NoteProgress>()
    private val notesOff = mutableSetOf<NoteProgress>()

    private val noteProgressPool = object : ObjectPool<NoteProgress>(100) {
        override fun newInstance(): NoteProgress {
            return NoteProgress()
        }

        override fun destroyInstance(obj: NoteProgress) {
            obj.noteOnProgress = 0
            obj.noteOffProgress = 0
            obj.releaseStartLevel = 0f
        }
    }

    fun noteOn(note: Note) {
        val progress = noteProgressPool.obtain()
        progress.note = note
        notesOn.add(progress)
    }

    fun noteOff(note: Note) {
        val currentProgress = notesOn.find { it.note == note } ?: return

        // Capture the current envelope level at the moment noteOff is called
        currentProgress.releaseStartLevel = envelop.noteOn(currentProgress.noteOnProgress)

        notesOn.remove(currentProgress)
        notesOff.add(currentProgress)
    }

    fun isNoteOn(note: Note): Boolean {
        val progress = noteProgressPool.obtain()
        progress.note = note
        return notesOn.contains(progress).also { noteProgressPool.free(progress) }
    }

    fun isNoteOff(note: Note): Boolean {
        val progress = noteProgressPool.obtain()
        progress.note = note
        return notesOff.contains(progress).also { noteProgressPool.free(progress) }
    }

    fun generate(): Float {
        var result = 0f
        notesOn.forEach { noteProgress ->
            var sample = harmonizer.generate(
                noteProgress.note,
                noteProgress.noteOnProgress,
                { frequency, progress -> oscillator.emit(frequency, progress) },
            )
            sample *= envelop.noteOn(noteProgress.noteOnProgress)
            result += sample
            noteProgress.noteOnProgress++
        }

        notesOff.forEach { noteProgress ->
            var sample = harmonizer.generate(
                noteProgress.note,
                noteProgress.noteOnProgress + noteProgress.noteOffProgress,
                { frequency, progress -> oscillator.emit(frequency, progress) },
            )
            val env = envelop.noteOff(noteProgress.noteOffProgress, noteProgress.releaseStartLevel)
            sample *= env
            result += sample
            noteProgress.noteOffProgress++
        }

        val release = envelop.release0()
        val toBeRemoved = notesOff.filter { noteProgress -> noteProgress.isCompleted(release) }

        notesOff.removeAll(toBeRemoved.toSet())
        noteProgressPool.free(toBeRemoved)

        result *= MASTER_VOLUME
        // Hard limiter
        result = max(-MASTER_VOLUME, min(MASTER_VOLUME, result))
        return result
    }

    fun close() {
        notesOn.forEach { it.noteOnProgress = 0 }
        notesOff.addAll(notesOn)
        notesOn.clear()
    }
}
