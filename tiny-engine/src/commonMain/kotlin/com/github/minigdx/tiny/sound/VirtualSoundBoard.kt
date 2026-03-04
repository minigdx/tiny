package com.github.minigdx.tiny.sound

import com.github.minigdx.tiny.lua.Note

interface VirtualSoundBoard {
    /**
     * Create a sound handler from a [MusicalBar]
     */
    fun prepare(bar: MusicalBar): SoundHandler

    /**
     * Create a sound handler from a [MusicalBar]
     */
    fun prepare(sequence: MusicalSequence): SoundHandler

    /**
     * Create a sound handler from a [MusicalSequence.Track]
     */
    fun prepare(track: MusicalSequence.Track): SoundHandler

    /**
     * Create a sound handler from a pre-computed audio buffer.
     */
    fun createHandler(buffer: FloatArray): SoundHandler

    fun convert(bar: MusicalBar): FloatArray

    fun noteOn(
        note: Note,
        instrument: Instrument,
    )

    fun noteOff(note: Note)
}
