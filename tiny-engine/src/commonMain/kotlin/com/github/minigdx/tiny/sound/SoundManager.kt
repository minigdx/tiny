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
                    sampleMixed = (
                        sampleMixed + fadeOut(
                            prec!!.generate(lastSample + i),
                            lastSample + i,
                            lastSample,
                            lastSample + crossover,
                        )
                        )
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

    fun playSong(song: Song) {
        val mix = createBufferFromSong(song)

        playBuffer(mix)
    }

    private val converter = SoundConverter()

    fun createBufferFromSong(song: Song): FloatArray {
        val numberOfSamplesPerBeat = (song.durationOfBeat * SAMPLE_RATE).toInt()
        val strips = converter.prepateStrip(song)
        val buffers = strips.map { (kind, strip) ->
            kind to converter.createStrip(numberOfSamplesPerBeat, strip)
        }.toMap()

        val numberOfTotalSamples = numberOfSamplesPerBeat * (song.numberOfBeats + 1)

        val mix = FloatArray(numberOfTotalSamples)
        (0 until numberOfTotalSamples).forEach { sample ->
            var result = 0f
            buffers.forEach { (_, line) ->
                result += line[sample]
            }
            mix[sample] = result / buffers.size.toFloat()
        }
        return mix
    }

    companion object {

        const val SAMPLE_RATE = 44100
        private const val FADE_OUT_DURATION: Seconds = 0.5f
    }
}

class SoundConverter {

    internal fun prepateStrip(song: Song): Map<String, Array<WaveGenerator>> {
        // Create a line per WaveGenerator kind.
        val musicPerType: MutableMap<String, Array<WaveGenerator>> = mutableMapOf()
        val beats = song.music.flatMap { pattern -> pattern.beats }
        val silence = SilenceWave(song.durationOfBeat)
        beats.forEachIndexed { index, beat ->
            beat.notes.forEach {
                val waves = musicPerType.getOrPut(it.name) { Array(song.numberOfBeats + 1) { silence } }
                waves[index] = it
            }
        }
        return musicPerType
    }

    internal fun createStrip(numberOfSamplesPerBeat: Int, waves: Array<WaveGenerator>): FloatArray {
        // 1/4 of a beat is used to fade
        val fader = Fader(0.25f * numberOfSamplesPerBeat / SAMPLE_RATE.toFloat())

        val result = FloatArray(waves.size * numberOfSamplesPerBeat)
        val cursor = Cursor()

        // Create the first wave.
        val firstBeat = waves.first()
        (0 until numberOfSamplesPerBeat).forEach { sample ->
            val volume = firstBeat.volume
            val value = firstBeat.generate(cursor.current)
            val sampled = value * volume
            result[sample] = sampled
            cursor.advance()
        }

        // crossfade the current wave with the previous one
        (1 until waves.size).forEach {
            val a = waves[it - 1]
            val b = waves[it]

            cursor.next()

            (0 until numberOfSamplesPerBeat).forEach { sample ->
                val sampled = fader.fadeWith(cursor.previous, a, cursor.current, b)
                result[sample] = sampled
                cursor.advance()
            }
        }

        return result
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
