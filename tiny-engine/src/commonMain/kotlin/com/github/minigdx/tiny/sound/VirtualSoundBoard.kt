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

    fun convert(sequence: MusicalSequence): FloatArray

    fun noteOn(
        note: Note,
        instrument: Instrument,
    )

    fun noteOff(note: Note)

    /**
     * Start streaming music playback using real-time beat-by-beat scheduling.
     * Changes apply without interrupting playback.
     */
    fun playMusic(
        config: MusicConfiguration,
        instruments: Array<Instrument?>,
    )

    /**
     * Stop streaming music playback.
     */
    fun stopMusic()

    /**
     * Update the configuration of currently streaming music.
     * The change is applied at the next bar boundary.
     */
    fun updateMusic(
        config: MusicConfiguration,
        instruments: Array<Instrument?>,
    )

    /**
     * Check if streaming music is currently playing.
     */
    fun isMusicPlaying(): Boolean
}
