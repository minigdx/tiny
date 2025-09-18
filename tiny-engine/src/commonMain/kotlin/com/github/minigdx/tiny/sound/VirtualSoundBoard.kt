package com.github.minigdx.tiny.sound

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
     * Create a sound handler from a chunkGenerator.
     *
     * The lambda will be called each time a new chunk needs to be generated/played.
     */
    fun prepare(chunkGenerator: Sequence<FloatArray>): SoundHandler

    fun convert(bar: MusicalBar): FloatArray
}
