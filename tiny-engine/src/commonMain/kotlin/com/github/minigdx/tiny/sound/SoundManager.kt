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

    fun getFadeOutIndex(longestDuration: Seconds): Int {
        return ((longestDuration - FADE_OUT_DURATION) * SAMPLE_RATE).toInt()
    }

    fun fadeOut(sample: Float, index: Int, fadeOutIndex: Int, endIndex: Int): Float {
        return if (index < fadeOutIndex) {
            sample
        } else {
            sample * (endIndex - index) / (endIndex - fadeOutIndex).toFloat()
        }
    }

    companion object {

        const val SAMPLE_RATE = 44100
        private const val FADE_OUT_DURATION: Seconds = 0.5f
    }
}
