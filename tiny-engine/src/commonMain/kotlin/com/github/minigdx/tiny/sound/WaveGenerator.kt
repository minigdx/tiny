package com.github.minigdx.tiny.sound

import com.github.minigdx.tiny.Percent
import com.github.minigdx.tiny.Seconds
import com.github.minigdx.tiny.lua.Note
import com.github.minigdx.tiny.sound.SoundManager.Companion.SAMPLE_RATE
import kotlin.math.abs
import kotlin.math.sin
import kotlin.random.Random

sealed class WaveGenerator(note: Note, val duration: Seconds, val volume: Percent) {

    val period = SAMPLE_RATE.toFloat() / note.frequency

    val numberOfSample = SAMPLE_RATE * duration

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

    companion object {
        internal const val PI = kotlin.math.PI.toFloat()
        internal const val TWO_PI = 2.0f * PI
    }
}

class SawToothWave(note: Note, duration: Seconds, volume: Percent = 1.0f) : WaveGenerator(note, duration, volume) {
    override fun generate(sample: Int): Float {
        return (2 * (angle(sample) / TWO_PI))
    }
}

class SineWave(note: Note, duration: Seconds, volume: Percent = 1.0f) : WaveGenerator(note, duration, volume) {
    override fun generate(sample: Int): Float {
        return sin(angle(sample))
    }
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
}

class TriangleWave(note: Note, duration: Seconds, volume: Percent = 1.0f) : WaveGenerator(note, duration, volume) {
    override fun generate(sample: Int): Float {
        val angle = angle(sample)
        return if (angle < PI) {
            2 * angle / PI
        } else {
            2 * (PI - angle) / PI + 1
        }
    }
}

class NoiseWave(private val note: Note, duration: Seconds, volume: Percent = 1.0f) : WaveGenerator(note, duration, volume) {

    private var lastNoise = 0.0f
    override fun generate(sample: Int): Float {
        val white = Random.nextFloat() * 2 - 1
        val brown = (lastNoise + (0.02f * white)) / 1.02f
        lastNoise = brown
        return brown * 3.5f * note.index / Note.B8.index
    }
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
}

class SilenceWave(duration: Seconds) : WaveGenerator(Note.C0, duration, 1.0f) {
    override fun generate(sample: Int): Float {
        return 0f
    }
}
