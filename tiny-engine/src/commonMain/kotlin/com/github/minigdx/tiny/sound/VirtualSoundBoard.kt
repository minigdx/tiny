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

    fun noteOn(
        note: Note,
        instrument: Instrument,
    )

    fun noteOff(note: Note)
}
