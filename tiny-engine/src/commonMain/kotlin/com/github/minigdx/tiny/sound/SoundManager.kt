package com.github.minigdx.tiny.sound

import com.github.minigdx.tiny.Seconds
import com.github.minigdx.tiny.input.InputHandler

interface MidiSound {
    fun play()

    fun loop()

    fun stop()
}

interface SoundManager {

    fun initSoundManager(inputHandler: InputHandler)

    suspend fun createSound(data: ByteArray): MidiSound

    fun playNotes(notes: List<WaveGenerator>, longestDuration: Seconds)

    fun mix(sample: Int, notes: List<WaveGenerator>): Float {
        var result = 0f
        notes.forEach {
            if (it.accept(sample)) {
                val sampleValue = it.generate(sample) * it.volume
                result += sampleValue
            }
        }
        return result
    }

    companion object {

        const val SAMPLE_RATE = 44100
    }
}
