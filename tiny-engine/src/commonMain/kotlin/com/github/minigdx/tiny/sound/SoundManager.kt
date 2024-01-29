package com.github.minigdx.tiny.sound

import com.github.minigdx.tiny.Seconds
import com.github.minigdx.tiny.input.InputHandler

interface Sound {
    fun play()

    fun loop()

    fun stop()
}

abstract class SoundManager {

    abstract fun initSoundManager(inputHandler: InputHandler)

    open fun destroy() = Unit

    abstract suspend fun createSfxSound(bytes: ByteArray): Sound

    abstract suspend fun createMidiSound(data: ByteArray): Sound

    fun playNotes(notes: List<WaveGenerator>, longestDuration: Seconds) {
        if (notes.isEmpty()) return

        val result = createNotesBuffer(longestDuration, notes)
        playBuffer(result)
    }

    fun playSfx(notes: List<WaveGenerator>) {
        if (notes.isEmpty()) return

        val result = createScoreBuffer(notes)

        playBuffer(result)
    }

    protected fun createNotesBuffer(
        longestDuration: Seconds,
        notes: List<WaveGenerator>,
    ): FloatArray {
        val numSamples: Int = (SAMPLE_RATE * longestDuration).toInt()
        val fadeOutIndex = getFadeOutIndex(longestDuration)
        val offsetSample = 0
        val result = FloatArray(numSamples)
        for (i in 0 until numSamples) {
            val sampleMixed = mix(i, notes, offsetSample)
            val sample = fadeOut(sampleMixed, i, fadeOutIndex, numSamples)
            result[i] = sample
        }
        return result
    }

    protected fun createScoreBuffer(notes: List<WaveGenerator>): FloatArray {
        val duration = notes.first().duration
        val noteSamples = (SAMPLE_RATE * duration).toInt()
        val noteFadeOutIndex = getFadeOutIndex(duration) // fade out index within the note.
        val numSamples: Int = (noteSamples * notes.size)
        var offsetSample = 0
        var currentIndex = 0
        val result = FloatArray(numSamples)

        notes.forEachIndexed { index, note ->
            val mixedNotes = listOf(note)
            val nextNoteIsDifferent = index == notes.size - 1 || notes[index + 1].isSame(note)

            for (i in 0 until noteSamples) {
                val sampleMixed = mix(i, mixedNotes, offsetSample)
                // Last note or different kind of note after
                val sample = if (nextNoteIsDifferent || notes[index + 1].isSilence) {
                    fadeOut(sampleMixed, i, noteFadeOutIndex, numSamples)
                } else {
                    sampleMixed
                }
                result[currentIndex++] = sample
                offsetSample++
            }
            if (nextNoteIsDifferent) {
                offsetSample = 0
            }
        }
        return result
    }

    /**
     * @param buffer byte array representing the sound. Each sample is represented with a float from -1.0f to 1.0f
     */
    abstract fun playBuffer(buffer: FloatArray)

    private fun mix(sample: Int, notes: List<WaveGenerator>, offsetSample: Int = 0): Float {
        var result = 0f
        notes.forEach {
            if (it.accept(sample)) {
                val sampleValue = it.generate(sample + offsetSample) * it.volume
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
