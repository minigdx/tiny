package com.github.minigdx.tiny.sound

import com.github.minigdx.tiny.input.InputHandler

interface MidiSound {
    fun play()

    fun loop(loop: Int)

    fun stop()
}

interface SoundManager {

    fun initSoundManager(inputHandler: InputHandler)

    suspend fun createSound(data: ByteArray): MidiSound
}
