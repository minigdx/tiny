package com.github.minigdx.tiny.sound

import com.github.minigdx.tiny.input.InputHandler
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

abstract class SoundManager {
    abstract fun initSoundManager(inputHandler: InputHandler)

    open fun destroy() = Unit

    /**
     * @param buffer byte array representing the sound. Each sample is represented with a float from -1.0f to 1.0f
     */
    abstract fun createSoundHandler(
        buffer: FloatArray,
        numberOfSamples: Long,
    ): SoundHandler

    fun createSoundHandler(bar: MusicalBar): SoundHandler {
        val buffer = convert(bar)
        return createSoundHandler(buffer, buffer.size.toLong())
    }

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

    companion object {
        const val SAMPLE_RATE = 44100
        const val MASTER_VOLUME = 0.8f
        const val PI = (kotlin.math.PI).toFloat()
        const val TWO_PI = 2.0f * PI
    }
}
