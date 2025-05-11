package com.github.minigdx.tiny.sound

/**
 * Sound handler that allow you to control a sound.
 */
interface SoundHandler {
    /**
     * Start to play a sound.
     */
    fun play()

    /**
     * Start to play a sound as a loop.
     */
    fun loop()

    /**
     * Stop the sound.
     */
    fun stop()
}
