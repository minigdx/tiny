package com.github.minigdx.tiny.sound

import com.github.minigdx.tiny.Percent
import com.github.minigdx.tiny.Seconds
import com.github.minigdx.tiny.lua.Note
import com.github.minigdx.tiny.sound.SoundManager.Companion.SAMPLE_RATE
import kotlin.math.abs
import kotlin.math.sin
import kotlin.random.Random

sealed class WaveGenerator(
    val note: Note,
    val duration: Seconds,
    val volume: Percent,
) {

    var frequency: Float = note.frequency

    val period: Float
        get() = SAMPLE_RATE.toFloat() / frequency

    val numberOfSample = SAMPLE_RATE * duration

    open val isSilence: Boolean = false

    /**
     * Return a value between -1.0 and 1.0
     */
    abstract fun generate(sample: Int): Float

    /**
     * Is a wave can be generated for this sample?
     */
    internal fun accept(sample: Int): Boolean {
        return sample < numberOfSample
    }

    internal fun angle(sample: Int): Float {
        return (TWO_PI * sample) / period
    }

    fun isSame(other: WaveGenerator): Boolean {
        return (other.note == this.note && this::class == other::class)
    }

    abstract fun copy(duration: Seconds, volume: Percent): WaveGenerator

    abstract val name: String

    abstract val index: Int

    companion object {
        internal const val PI = kotlin.math.PI.toFloat()
        internal const val TWO_PI = 2.0f * PI
    }
}

class SineWave(note: Note, duration: Seconds, volume: Percent = 1.0f) : WaveGenerator(note, duration, volume) {
    override fun generate(sample: Int): Float {
        return sin(angle(sample))
    }

    override fun copy(duration: Seconds, volume: Percent): WaveGenerator = SineWave(note, duration, volume)

    override val name: String = "Sine"

    override val index: Int = 1
}

class SquareWave(note: Note, duration: Seconds, volume: Percent = 1.0f) : WaveGenerator(note, duration, volume) {
    override fun generate(sample: Int): Float {
        val value = sin(angle(sample))
        return if (value > 0f) {
            1f
        } else {
            -1f
        }
    }

    override fun copy(duration: Seconds, volume: Percent): WaveGenerator = SquareWave(note, duration, volume)

    override val name: String = "Square"

    override val index: Int = 2
}

class TriangleWave(note: Note, duration: Seconds, volume: Percent = 1.0f) : WaveGenerator(note, duration, volume) {
    override fun generate(sample: Int): Float {
        val angle: Float = sin(angle(sample))
        val phase = (angle + 1.0) % 1.0 // Normalize sinValue to the range [0, 1]
        return (if (phase < 0.5) 4.0 * phase - 1.0 else 3.0 - 4.0 * phase).toFloat()
    }

    override fun copy(duration: Seconds, volume: Percent): WaveGenerator = TriangleWave(note, duration, volume)

    override val name: String = "Triangle"

    override val index: Int = 3
}

class NoiseWave(note: Note, duration: Seconds, volume: Percent = 1.0f) : WaveGenerator(note, duration, volume) {

    private var lastNoise = 0.0f
    override fun generate(sample: Int): Float {
        val white = Random.nextFloat() * 2 - 1
        val brown = (lastNoise + (0.02f * white)) / 1.02f
        lastNoise = brown
        return brown * 3.5f * note.index / Note.B8.index
    }

    override fun copy(duration: Seconds, volume: Percent): WaveGenerator = NoiseWave(note, duration, volume)

    override val name: String = "Noise"

    override val index: Int = 4
}

class PulseWave(note: Note, duration: Seconds, volume: Percent = 1.0f) : WaveGenerator(note, duration, volume) {
    override fun generate(sample: Int): Float {
        val angle = angle(sample)

        val t = angle % 1
        val k = abs(2.0 * ((angle / 128.0) % 1.0) - 1.0)
        val u = (t + 0.5 * k) % 1.0
        val ret = abs(4.0 * u - 2.0) - abs(8.0 * t - 4.0)
        return (ret / 6.0).toFloat()
    }

    override fun copy(duration: Seconds, volume: Percent): WaveGenerator = PulseWave(note, duration, volume)

    override val name: String = "Pulse"

    override val index: Int = 5
}

class SawToothWave(note: Note, duration: Seconds, volume: Percent = 1.0f) : WaveGenerator(note, duration, volume) {
    override fun generate(sample: Int): Float {
        val angle: Float = sin(angle(sample))
        val phase = (angle * 2f) - 1f
        return phase
    }

    override fun copy(duration: Seconds, volume: Percent): WaveGenerator = SawToothWave(note, duration, volume)

    override val name: String = "Sawtooth"

    override val index: Int = 6
}

class SilenceWave(duration: Seconds) : WaveGenerator(Note.C0, duration, 0.0f) {

    override val isSilence: Boolean = true
    override fun generate(sample: Int): Float {
        return 0f
    }

    override fun copy(duration: Seconds, volume: Percent): WaveGenerator = SilenceWave(duration)

    override val name: String = "Silence"

    override val index: Int = 0
}
