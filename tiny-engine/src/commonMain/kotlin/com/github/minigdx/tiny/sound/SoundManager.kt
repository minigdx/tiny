package com.github.minigdx.tiny.sound

import com.github.minigdx.tiny.Seconds
import com.github.minigdx.tiny.input.InputHandler
import com.github.minigdx.tiny.sound.SoundManager.Companion.SAMPLE_RATE
import kotlin.math.min

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
        playBuffer(result, result.size.toLong())
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

    /**
     * @param buffer byte array representing the sound. Each sample is represented with a float from -1.0f to 1.0f
     */
    abstract fun playBuffer(buffer: FloatArray, numberOfSamples: Long)

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

    fun playSong(song: Song2) {
        val (mix, numberOfSamples) = createBufferFromSong(song)
        playBuffer(mix, numberOfSamples)
    }

    private val converter = SoundConverter()

    fun createBufferFromSong(song: Song): SoundBuffer {
        val numberOfSamplesPerBeat = (song.durationOfBeat * SAMPLE_RATE).toInt()

        val result = converter.createStrip(
            song.volume,
            numberOfSamplesPerBeat,
            song.music.flatMap { p -> p.notes }.toTypedArray(),
        )

        return result
    }

    fun createBufferFromSong(song: Song2): SoundBuffer {
        val numberOfSample = song.numberOfTotalSample

        val result = FloatArray(numberOfSample.toInt())

        val divider = song.tracks.size.toFloat()
        (0 until numberOfSample.toInt()).forEach { index ->
            song.tracks.forEach { track: Track ->
                val sample = track.getSample(index) * divider
                result[index] += sample
            }

            result[index] = result[index] / divider
        }
        return SoundBuffer(result, numberOfSample)
    }

    companion object {

        const val SAMPLE_RATE = 44100
        private const val FADE_OUT_DURATION: Seconds = 0.5f
    }
}

data class SoundBuffer(val samples: FloatArray, val numberOfSamples: Long)

class SoundConverter {

    internal fun createStrip(songVolume: Float, numberOfSamplesPerBeat: Int, waves: Array<WaveGenerator>): SoundBuffer {
        // 1/4 of a beat is used to fade
        val fader = Fader(0.25f * numberOfSamplesPerBeat / SAMPLE_RATE.toFloat())

        val result = FloatArray(waves.size * numberOfSamplesPerBeat)
        val cursor = Cursor()

        // Create the first wave.
        val firstBeat = waves.first()
        (0 until numberOfSamplesPerBeat).forEach { _ ->
            val volume = firstBeat.volume
            val value = firstBeat.generate(cursor.current)
            val sampled = value * volume
            result[cursor.absolute] = sampled * songVolume
            cursor.advance()
        }

        // crossfade the current wave with the previous one
        (1 until waves.size).forEach {
            val a = waves[it - 1]
            val b = waves[it]

            cursor.next()

            (0 until numberOfSamplesPerBeat).forEach { _ ->
                val sampled = fader.fadeWith(cursor.previous, a, cursor.current, b)
                result[cursor.absolute] = sampled * songVolume
                cursor.advance()
            }
        }

        return SoundBuffer(result, cursor.absolute.toLong())
    }
}

class Cursor(var previous: Int = 0, var current: Int = 0, var absolute: Int = 0) {
    fun next(): Cursor {
        previous = current
        current = 0
        return this
    }

    fun advance(): Int {
        val r = current
        current++
        absolute++
        return r
    }
}

class Fader(duration: Seconds) {

    private val cuteoff = (duration * SAMPLE_RATE).toInt()

    fun fadeWith(
        previousSample: Int,
        previous: WaveGenerator?,
        currentSample: Int,
        current: WaveGenerator?,
    ): Float {
        val prec = previous?.generate(previousSample) ?: 0f
        val precVol = previous?.volume ?: 1f
        val cur = current?.generate(currentSample) ?: 0f
        val curVol = current?.volume ?: 1f

        val alpha = if (previous?.note == current?.note) {
            1f
        } else {
            1f - min(cur / cuteoff, 1f)
        }

        return cur * curVol + prec * alpha * precVol
    }
}
