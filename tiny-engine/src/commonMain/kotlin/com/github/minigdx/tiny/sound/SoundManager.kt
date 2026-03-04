package com.github.minigdx.tiny.sound

import com.github.minigdx.tiny.BPM
import com.github.minigdx.tiny.Percent
import com.github.minigdx.tiny.input.InputHandler
import com.github.minigdx.tiny.lua.Note
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt
import kotlin.math.tanh

class DefaultSoundBoard(private val soundManager: SoundManager) : VirtualSoundBoard {
    override fun prepare(bar: MusicalBar): SoundHandler {
        val buffer = soundManager.convert(bar)
        return soundManager.createSoundHandler(buffer)
    }

    override fun prepare(sequence: MusicalSequence): SoundHandler {
        val buffer = soundManager.convert(sequence)
        return soundManager.createSoundHandler(buffer)
    }

    override fun prepare(track: MusicalSequence.Track): SoundHandler {
        val sequence = MusicalSequence(tracks = arrayOf(track), index = 0)
        val buffer = soundManager.convert(sequence)
        return soundManager.createSoundHandler(buffer)
    }

    override fun createHandler(buffer: FloatArray): SoundHandler {
        return soundManager.createSoundHandler(buffer)
    }

    override fun convert(bar: MusicalBar): FloatArray {
        val buffer = soundManager.convert(bar)
        return buffer
    }

    override fun noteOn(
        note: Note,
        instrument: Instrument,
    ) {
        soundManager.noteOn(note, instrument)
    }

    override fun noteOff(note: Note) {
        soundManager.noteOff(note)
    }
}

abstract class SoundManager {
    abstract fun initSoundManager(inputHandler: InputHandler)

    open fun destroy() = Unit

    /**
     * Configurable master volume. Defaults to [DEFAULT_MASTER_VOLUME].
     */
    var masterVolume: Float = DEFAULT_MASTER_VOLUME

    /**
     * @param buffer byte array representing the sound. Each sample is represented with a float from -1.0f to 1.0f
     */
    abstract fun createSoundHandler(buffer: FloatArray): SoundHandler

    /**
     * Convert the MusicBar into a playable sound.
     */
    fun convert(bar: MusicalBar): FloatArray {
        return convert(
            defaultInstrument = bar.instrument,
            beats = bar.beats,
            tempo = bar.tempo,
            volume = bar.volume,
        )
    }

    fun convert(sequence: MusicalSequence): FloatArray {
        val tracks = sequence.tracks.filter { !it.mute && it.instrument != null }.map { track ->
            // Convert notes from track to note for sounds.
            var current = track.beats.first().copy()
            val beats = mutableListOf(current)
            track.beats.drop(1).forEach { beat ->
                // Set the Note Off
                if (beat.isOffNote) {
                    current = beat.copy(volume = 0f, duration = 1f)
                    beats.add(current)
                } else if (beat.note == null && current.isRepeating) {
                    // Repeating the previous note
                    current = current.copy(beat = beat.beat, duration = 1f, isRepeating = true)
                    beats.add(current)
                } else if (beat.note == null && !current.isRepeating) {
                    // Extending the previous note
                    current.duration += 1f
                } else {
                    // New note!
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

        if (tracks.isEmpty()) return floatArrayOf()

        val resultSize = tracks.maxOf { it.size }
        val result = FloatArray(resultSize) { 0f }

        // Calculate RMS values for each track to properly scale them during mixing
        val trackRmsValues = tracks.map { calculateRms(it) }
        val totalRms = trackRmsValues.sum()

        // Mix tracks with RMS-based scaling to prevent saturation
        result.indices.forEach { index ->
            var mixedSample = 0f

            // If total RMS is significant, use it for scaling
            if (totalRms > 0.001f) {
                tracks.forEachIndexed { trackIndex, track ->
                    val sample = track.getOrNull(index) ?: 0f
                    // Scale each sample based on its track's contribution to the total RMS
                    val scaleFactor = if (trackRmsValues[trackIndex] > 0.001f) {
                        // Normalize by the track's RMS value relative to the total RMS
                        1f / (trackRmsValues.size * (trackRmsValues[trackIndex] / totalRms))
                    } else {
                        1f
                    }
                    mixedSample += sample * scaleFactor
                }
            } else {
                // Fallback to simple averaging if RMS values are too small
                mixedSample = tracks.mapNotNull { it.getOrNull(index) }.sum() / tracks.size.toFloat()
            }

            result[index] = softClip(mixedSample)
        }
        return result
    }

    /**
     * Calculates the Root Mean Square (RMS) value of an audio buffer.
     * RMS is a measure of the average power of the signal and provides a better
     * representation of perceived loudness than simple averaging.
     */
    private fun calculateRms(buffer: FloatArray): Float {
        if (buffer.isEmpty()) return 0f

        var sumOfSquares = 0f
        for (sample in buffer) {
            sumOfSquares += sample * sample
        }

        return kotlin.math.sqrt(sumOfSquares / buffer.size)
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
            val effectiveRelease = max(instrument.release, MIN_RELEASE_SECONDS)
            val noteDurationInSeconds = (b.duration * secondsPerBeat) + effectiveRelease
            val numberOfSamples = (noteDurationInSeconds * SAMPLE_RATE).roundToInt()

            val maxPossibleAmplitude = instrument.harmonics.sum()
            val normalizationFactor = 1.0f / max(1.0f, maxPossibleAmplitude)

            val buffer = FloatArray(numberOfSamples)

            val fundamentalFreq = b.note?.frequency ?: 0f

            for (i in 0..<numberOfSamples) {
                val time = (i.toFloat() / SAMPLE_RATE.toFloat())
                var sampleValue = 0.0f

                // Only generate sound if there is a note
                if (b.note != null) {
                    instrument.harmonics.forEachIndexed { index, relativeAmplitude ->
                        val harmonicNumber = index + 1
                        val harmonicFreq = fundamentalFreq * harmonicNumber

                        sampleValue += relativeAmplitude * instrument.generate(harmonicFreq, time)
                    }

                    sampleValue *= envelopeFilter(i, numberOfSamples, instrument)
                    sampleValue *= normalizationFactor * b.volume * volume
                    sampleValue *= masterVolume
                }

                buffer[i] = softClip(sampleValue)
            }

            // Apply safety fade-out to prevent clicks at buffer boundaries
            val fadeOutSamples = min(FADE_OUT_SAMPLES, numberOfSamples)
            for (i in 0 until fadeOutSamples) {
                val fadeIndex = numberOfSamples - fadeOutSamples + i
                if (fadeIndex >= 0 && fadeIndex < buffer.size) {
                    val fadeFactor = 1.0f - (i.toFloat() / fadeOutSamples.toFloat())
                    buffer[fadeIndex] *= fadeFactor
                }
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
                result[i] = softClip(result[i] + buffer[index++])
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
        val effectiveRelease = max(MIN_RELEASE_SECONDS, instrument.release)
        val releaseSamples = max(1f, effectiveRelease * SAMPLE_RATE)
        val sustainLevel = min(1f, max(instrument.sustain, 0f))

        val releaseStartSample = totalSamples - (effectiveRelease * SAMPLE_RATE)
        // Ensure that phases can't finish AFTER the release.
        val attackEndSample = min(attackSamples, releaseStartSample)
        val decayEndSample = min(attackEndSample + decaySamples, releaseStartSample)

        val multiplier = if (currentSample < attackEndSample) {
            // Attack: quadratic curve (slow start, fast finish)
            val linear = currentSample.toFloat() / attackSamples
            linear * linear
        } else if (currentSample < decayEndSample) {
            // Decay: inverse-square curve (fast drop, slow tail toward sustain)
            val linear = (currentSample - attackEndSample) / decaySamples
            val remaining = 1.0f - linear
            sustainLevel + (1.0f - sustainLevel) * remaining * remaining
        } else if (currentSample < releaseStartSample) {
            // Sustain level
            sustainLevel
        } else {
            // Release: quadratic curve (fast drop, slow tail to 0)
            val linear = (currentSample - releaseStartSample) / releaseSamples
            val remaining = 1.0f - min(1.0f, linear)
            sustainLevel * remaining * remaining
        }

        return max(0.0f, min(1.0f, multiplier))
    }

    abstract fun noteOn(
        note: Note,
        instrument: Instrument,
    )

    abstract fun noteOff(note: Note)

    private val currentHandlers = mutableListOf<SoundHandler>()

    fun stopAll() {
        val toBeRemoved = currentHandlers.toList()
        toBeRemoved.forEach { it.stop() }
        currentHandlers.clear()
    }

    internal fun addSoundHandler(handler: SoundHandler) {
        currentHandlers.add(handler)
    }

    internal fun removeSoundHandler(handler: SoundHandler) {
        currentHandlers.remove(handler)
    }

    companion object {
        const val SAMPLE_RATE = 44100

        const val DEFAULT_MASTER_VOLUME = 0.5f

        @Deprecated("Use instance masterVolume instead", replaceWith = ReplaceWith("masterVolume"))
        const val MASTER_VOLUME = DEFAULT_MASTER_VOLUME

        const val PI = (kotlin.math.PI).toFloat()
        const val TWO_PI = 2.0f * PI

        // ~2ms at 44100 Hz - safety fade-out to prevent clicks at buffer boundaries
        private const val FADE_OUT_SAMPLES = 88

        // Minimum release time in seconds (~2ms) to prevent clicks
        private const val MIN_RELEASE_SECONDS = 0.002f

        // Pre-computed normalization constant for tanh-based soft clipping
        private val TANH_NORM = 1.0f / tanh(1.5f)

        /**
         * Soft saturation using tanh.
         * Produces warm compression when signals exceed the dynamic range,
         * instead of harsh distortion from hard clipping.
         * Quiet signals pass through nearly unaffected.
         */
        fun softClip(sample: Float): Float {
            val clipped = tanh(sample * 1.5f) * TANH_NORM
            return max(-1.0f, min(1.0f, clipped))
        }
    }
}
