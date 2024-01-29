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
        val result = FloatArray(numSamples)
        for (i in 0 until numSamples) {
            val sampleMixed = mix(i, notes)
            val sample = fadeOut(sampleMixed, i, fadeOutIndex, numSamples)
            result[i] = sample
        }
        return result
    }

    protected fun createScoreBuffer(notes: List<WaveGenerator>): FloatArray {
        var currentIndex = 0

        fun merge(head: WaveGenerator, tail: List<WaveGenerator>): List<WaveGenerator> {
            if (tail.isEmpty()) {
                return listOf(head)
            }

            val next = tail.first()
            return if (next.isSame(head)) {
                merge(head.copy(head.duration + next.duration, head.volume), tail.drop(1))
            } else {
                listOf(head) + merge(next, tail.drop(1))
            }
        }

        val mergedNotes = merge(notes.first(), notes.drop(1)) + SilenceWave(0.1f)

        var prec: WaveGenerator? = null
        var lastSample = 0
        val result = FloatArray((mergedNotes.sumOf { it.duration.toDouble() } * SAMPLE_RATE).toInt())
        mergedNotes.forEach { note ->
            val crossover = (0.05f * SAMPLE_RATE).toInt()
            val mixedNotes = listOf(note)
            val noteSamples = (SAMPLE_RATE * note.duration).toInt()
            for (i in 0 until noteSamples) {
                var sampleMixed = mix(i, mixedNotes)

                // crossover
                if (prec != null && i <= crossover) {
                    sampleMixed = (sampleMixed + fadeOut(prec!!.generate(lastSample + i), lastSample + i, lastSample, lastSample + crossover))
                }
                result[currentIndex++] = sampleMixed
            }

            prec = note
            lastSample = noteSamples
        }
        return result
    }

    /**
     * @param buffer byte array representing the sound. Each sample is represented with a float from -1.0f to 1.0f
     */
    abstract fun playBuffer(buffer: FloatArray)

    private fun mix(sample: Int, notes: List<WaveGenerator>): Float {
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
