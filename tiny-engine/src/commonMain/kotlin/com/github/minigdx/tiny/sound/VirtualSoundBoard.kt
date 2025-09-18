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

    fun prepare(chunkGenerator: ChunkGenerator): SoundHandler

    fun convert(bar: MusicalBar): FloatArray
}
