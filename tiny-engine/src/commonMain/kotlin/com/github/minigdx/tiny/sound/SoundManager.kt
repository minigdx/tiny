package com.github.minigdx.tiny.sound

import com.github.minigdx.tiny.BPM
import com.github.minigdx.tiny.Percent
import com.github.minigdx.tiny.input.InputHandler
import com.github.minigdx.tiny.lua.Note
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

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

    override fun convert(sequence: MusicalSequence): FloatArray {
        return soundManager.convert(sequence)
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
        val result = FloatArray(resultSize)

        // Calculate RMS values for each track to properly scale them during mixing
        val trackRmsValues = FloatArray(tracks.size) { calculateRms(tracks[it]) }
        val totalRms = trackRmsValues.sum()

        // Step 6: Pre-compute scale factors and use indexed loops
        if (totalRms > 0.001f) {
            val scaleFactors = FloatArray(tracks.size) { t ->
                if (trackRmsValues[t] > 0.001f) {
                    1f / (tracks.size * (trackRmsValues[t] / totalRms))
                } else {
                    1f
                }
            }

            for (index in result.indices) {
                var mixedSample = 0f
                for (t in tracks.indices) {
                    val track = tracks[t]
                    if (index < track.size) {
                        mixedSample += track[index] * scaleFactors[t]
                    }
                }
                result[index] = softClip(mixedSample)
            }
        } else {
            val invSize = 1f / tracks.size.toFloat()
            for (index in result.indices) {
                var mixedSample = 0f
                for (t in tracks.indices) {
                    val track = tracks[t]
                    if (index < track.size) {
                        mixedSample += track[index]
                    }
                }
                result[index] = softClip(mixedSample * invSize)
            }
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

        // Step 1: Pre-allocate result array to avoid O(n^2) copyOf calls
        var resultSize = 0
        for (b in beats) {
            val instrument = b.instrument ?: defaultInstrument
            val effectiveRelease = max(instrument.release, MIN_RELEASE_SECONDS)
            val noteDurationInSeconds = (b.duration * secondsPerBeat) + effectiveRelease
            val numberOfSamples = (noteDurationInSeconds * SAMPLE_RATE).roundToInt()
            val minimumSize = (b.beat * secondsPerBeat * SAMPLE_RATE).roundToInt() + numberOfSamples
            if (minimumSize > resultSize) resultSize = minimumSize
        }
        val result = FloatArray(resultSize)

        for (b in beats) {
            // Step 7: Skip silent notes early
            if (b.note == null && b.volume == 0f) continue

            val instrument = b.instrument ?: defaultInstrument
            // Duration of the note added to the duration of the release.
            val effectiveRelease = max(instrument.release, MIN_RELEASE_SECONDS)
            val noteDurationInSeconds = (b.duration * secondsPerBeat) + effectiveRelease
            val numberOfSamples = (noteDurationInSeconds * SAMPLE_RATE).roundToInt()

            val maxPossibleAmplitude = instrument.harmonics.sum()
            val normalizationFactor = 1.0f / max(1.0f, maxPossibleAmplitude)

            val buffer = FloatArray(numberOfSamples)

            val fundamentalFreq = b.note?.frequency ?: 0f

            // Step 2: Pre-filter active modulations once per note
            val activeModulations = instrument.activeModulations()
            // Step 3: Precompute envelope parameters once per note
            val envelope = PrecomputedEnvelope(numberOfSamples, instrument)
            // Step 5: Get harmonics array for indexed loop
            val harmonics = instrument.harmonics

            for (i in 0..<numberOfSamples) {
                val time = (i.toFloat() / SAMPLE_RATE.toFloat())
                var sampleValue = 0.0f

                // Only generate sound if there is a note
                if (b.note != null) {
                    // Step 5: Indexed loop + skip zero harmonics
                    for (h in harmonics.indices) {
                        val amp = harmonics[h]
                        if (amp == 0f) continue
                        sampleValue += amp * instrument.generate(fundamentalFreq * (h + 1), time, activeModulations)
                    }

                    // Step 3: Use precomputed envelope
                    sampleValue *= envelope.evaluate(i)
                    sampleValue *= normalizationFactor * b.volume * volume
                    sampleValue *= masterVolume
                }

                // Step 4: No softClip per sample — only at final mix
                buffer[i] = sampleValue
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

            // Step 4: No softClip on additive mix — just accumulate
            var index = 0
            for (i in startIndex until endIndex) {
                result[i] += buffer[index++]
            }
        }

        return result
    }

    /**
     * Pre-computed envelope parameters to avoid redundant calculations per sample.
     * All phase boundaries are computed once per note.
     */
    private class PrecomputedEnvelope(totalSamples: Int, instrument: Instrument) {
        private val attackSamples: Float = max(1f, instrument.attack * SAMPLE_RATE)
        private val decaySamples: Float = max(1f, instrument.decay * SAMPLE_RATE)
        private val sustainLevel: Float = min(1f, max(instrument.sustain, 0f))
        private val releaseSamples: Float
        private val releaseStartSample: Float
        private val attackEndSample: Float
        private val decayEndSample: Float

        init {
            val effectiveRelease = max(MIN_RELEASE_SECONDS, instrument.release)
            releaseSamples = max(1f, effectiveRelease * SAMPLE_RATE)
            releaseStartSample = totalSamples - (effectiveRelease * SAMPLE_RATE)
            attackEndSample = min(attackSamples, releaseStartSample)
            decayEndSample = min(attackEndSample + decaySamples, releaseStartSample)
        }

        fun evaluate(currentSample: Int): Float {
            val multiplier = if (currentSample < attackEndSample) {
                val linear = currentSample.toFloat() / attackSamples
                linear * linear
            } else if (currentSample < decayEndSample) {
                val linear = (currentSample - attackEndSample) / decaySamples
                val remaining = 1.0f - linear
                sustainLevel + (1.0f - sustainLevel) * remaining * remaining
            } else if (currentSample < releaseStartSample) {
                sustainLevel
            } else {
                val linear = (currentSample - releaseStartSample) / releaseSamples
                val remaining = 1.0f - min(1.0f, linear)
                sustainLevel * remaining * remaining
            }

            return max(0.0f, min(1.0f, multiplier))
        }
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

        /**
         * Fast soft saturation using a Padé approximation of tanh.
         * Produces warm compression when signals exceed the dynamic range,
         * instead of harsh distortion from hard clipping.
         * Quiet signals pass through nearly unaffected.
         */
        fun softClip(sample: Float): Float {
            val x = sample * 1.5f
            return when {
                x >= 3f -> 1f
                x <= -3f -> -1f
                else -> {
                    val x2 = x * x
                    x * (27f + x2) / (27f + 9f * x2)
                }
            }
        }
    }
}
