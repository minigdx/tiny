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

    fun convert(bar: MusicalBar): FloatArray

    fun convert(sequence: MusicalSequence): FloatArray

    /**
     * Create a sound handler from a pre-computed audio buffer.
     * This is used for playing pre-rendered sounds without recomputation.
     */
    fun prepareFromBuffer(buffer: FloatArray): SoundHandler

    fun noteOn(
        note: Note,
        instrument: Instrument,
    )

    fun noteOff(note: Note)
}
