package com.github.minigdx.tiny.sound

import com.github.minigdx.tiny.BPM
import com.github.minigdx.tiny.Percent
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

    fun createSoundHandler(bar: MusicalSequence): SoundHandler {
        val buffer = convert(bar)
        return createSoundHandler(buffer, buffer.size.toLong())
    }

    fun createSoundHandler(bar: MusicalSequence.Track): SoundHandler {
        // TODO: pass tempo
        val sequence = MusicalSequence(tracks = arrayOf(bar), index = 0)
        val buffer = convert(sequence)
        return createSoundHandler(buffer, buffer.size.toLong())
    }

    /**
     * Convert the MusicBar into a playable sound.
     */
    fun convert(bar: MusicalBar): FloatArray {
        return convert(
            defaultInstrument = bar.instrument,
            beats = bar.beats,
            tempo = bar.tempo,
        )
    }

    fun convert(sequence: MusicalSequence): FloatArray {
        val tracks = sequence.tracks.map { track ->
            // Convert notes from track to note for sounds.
            var current = track.beats.first().copy()
            val beats = mutableListOf(current)
            track.beats.drop(1).forEach { beat ->
                if (beat.note == null && current.isRepeating) {
                    current = current.copy(duration = 1f)
                    beats.add(current)
                } else if (beat.note == null && !current.isRepeating) {
                    current.duration += 0.5f
                } else if (beat.isOffNote) {
                    current = beat.copy(volume = 0f, duration = 1f)
                    beats.add(current)
                } else {
                    current = beat.copy()
                    beats.add(current)
                }
            }
            convert(
                defaultInstrument = track.instrument,
                beats = beats,
                tempo = sequence.tempo,
                volume = track.volume,
            )
        }

        val resultSize = tracks.map { it.size }.max()
        val result = FloatArray(resultSize) { 0f }

        result.indices.forEach { index ->
            result[index] = tracks.mapNotNull { it.getOrNull(index) }.sum()
        }
        return result
    }

    private fun convert(
        defaultInstrument: Instrument?,
        beats: MutableList<MusicalNote>,
        tempo: BPM,
        volume: Percent = 1f,
    ): FloatArray {
        if (defaultInstrument == null) return floatArrayOf()
        if (beats.isEmpty()) return floatArrayOf()

        val secondsPerBeat = 60f / tempo

        var result = floatArrayOf()

        for (b in beats) {
            val instrument = b.instrument ?: defaultInstrument
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
