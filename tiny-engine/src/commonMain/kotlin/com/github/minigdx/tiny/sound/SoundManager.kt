package com.github.minigdx.tiny.sound

import com.github.minigdx.tiny.Seconds
import com.github.minigdx.tiny.input.InputHandler
import com.github.minigdx.tiny.sound.SoundManager.Companion.SAMPLE_RATE
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

interface Sound {
    fun play()

    fun loop()

    fun stop()
}

abstract class SoundManager {
    abstract fun initSoundManager(inputHandler: InputHandler)

    open fun destroy() = Unit

    abstract suspend fun createSfxSound(bytes: ByteArray): Sound

    fun playNotes(
        notes: List<WaveGenerator>,
        longestDuration: Seconds,
    ) {
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
    abstract fun playBuffer(
        buffer: FloatArray,
        numberOfSamples: Long,
    )

    private fun mix(
        sample: Int,
        notes: List<WaveGenerator>,
    ): Float {
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

    fun fadeOut(
        sample: Float,
        index: Int,
        fadeOutIndex: Int,
        endIndex: Int,
    ): Float {
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

    fun play(bar: MusicalBar) {
        val buffer = convert(bar)
        playBuffer(buffer, buffer.size.toLong())
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

            result[index] = (result[index] / divider) * song.volume
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
    internal fun createStrip(
        songVolume: Float,
        numberOfSamplesPerBeat: Int,
        waves: Array<WaveGenerator>,
    ): SoundBuffer {
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

        val alpha =
            if (previous?.note == current?.note) {
                1f
            } else {
                1f - min(cur / cuteoff, 1f)
            }

        return cur * curVol + prec * alpha * precVol
    }
}

const val MASTER_VOLUME = 0.8f

/**
 * Convert the MusicBar into a playable sound.
 */
fun convert(bar: MusicalBar): FloatArray {
    val instrument = bar.instrument
    if (instrument == null) return floatArrayOf()
    if (bar.beats.isEmpty()) return floatArrayOf()

    val secondsPerBeat = 60f / bar.tempo

    var result = floatArrayOf()

    for (b in bar.beats) {
        // Duration of the note added to the duration of the release.
        val noteDurationInSeconds = (b.duration * secondsPerBeat) + instrument.release
        val numberOfSamples = (noteDurationInSeconds * SAMPLE_RATE).roundToInt()

        val maxPossibleAmplitude = instrument.harmonics.sum()
        val normalizationFactor = 1.0f / max(1.0f, maxPossibleAmplitude)

        val buffer = FloatArray(numberOfSamples)

        val fundamentalFreq = b.note?.frequency ?: 0f

        for (i in 0..<numberOfSamples) {
            val time = (i.toFloat() / SAMPLE_RATE.toFloat())
            var sampleValue = 0.0f

            instrument.harmonics.forEachIndexed { index, relativeAmplitude ->
                val harmonicNumber = index + 1
                val harmonicFreq = fundamentalFreq * harmonicNumber

                sampleValue += relativeAmplitude * instrument.generate(harmonicFreq, time)
            }

            sampleValue *= envelopeFilter(i, numberOfSamples, instrument)
            sampleValue *= normalizationFactor * b.volume
            sampleValue *= MASTER_VOLUME

            buffer[i] = max(-1.0f, min(1.0f, sampleValue))
        }

        // Put the buffer at the time of the beat in the result (without the release).
        val startIndex = (b.beat * secondsPerBeat * SAMPLE_RATE).roundToInt()
        val endIndex = startIndex + numberOfSamples - 1
        val minimumSize = startIndex + numberOfSamples

        // Adjust the size of result if the buffer finish after the actual result.
        if (minimumSize > result.size) {
            result = result.copyOf(minimumSize)
        }

        // Additive synthesis of the buffer into the result.
        var index = 0
        for (i in startIndex until endIndex) {
            result[i] = min(max(-1f, result[i] + buffer[index++]), 1f)
        }
    }

    return result
}

private fun envelopeFilter(
    currentSample: Int,
    totalSamples: Int,
    instrument: Instrument,
): Float {
    // Get the number of samples for each phase. Avoid empty phase
    val attackSamples = max(1f, instrument.attack * SAMPLE_RATE)
    val decaySamples = max(1f, instrument.decay * SAMPLE_RATE)
    val releaseSamples = max(1f, instrument.release * SAMPLE_RATE)
    val sustainLevel = min(1f, max(instrument.sustain, 0f))

    val releaseStartSample = totalSamples - (instrument.release * SAMPLE_RATE)
    // Ensure that phases can't finish AFTER the release.
    val attackEndSample = min(attackSamples, releaseStartSample)
    val decayEndSample = min(attackEndSample + decaySamples, releaseSamples)

    // FIXME: le changement de niveau sonore pourrait ne pas être linéaire. (cf juice)
    val multiplier =
        if (currentSample < attackEndSample) {
            // Attack: going from 0 to 1.0
            currentSample.toFloat() / attackSamples
        } else if (currentSample < decayEndSample) {
            // Decay: going from 1.0 to sustain level
            val decayProgress = (currentSample - attackEndSample) / decaySamples
            1.0f - decayProgress * (1.0f - sustainLevel)
        } else if (currentSample < releaseStartSample) {
            // Sustain level
            sustainLevel
        } else {
            // Release: going from sustain to 0f
            val releaseProgress = (currentSample - releaseStartSample) / releaseSamples
            sustainLevel * (1.0f - min(1.0f, releaseProgress))
        }

    return max(0.0f, min(1.0f, multiplier))
}
