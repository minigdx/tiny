package com.github.minigdx.tiny.sound

import com.github.minigdx.tiny.Seconds
import com.github.minigdx.tiny.lua.Note
import com.github.minigdx.tiny.sound.SoundManager.Companion.SAMPLE_RATE
import kotlin.math.sin

sealed class WaveGenerator(val note: Note, val duration: Seconds) {

    val period = SAMPLE_RATE.toFloat() / note.frequency

    /**
     * Return a value between -1.0 and 1.0
     */
    abstract fun generate(sample: Int): Float

    internal fun angle(sample: Int): Float {
        return (TWO_PI * sample) / period
    }

    companion object {
        internal const val PI = kotlin.math.PI.toFloat()
        internal const val TWO_PI = 2.0f * PI
    }
}

class SawTooth(note: Note, duration: Seconds) : WaveGenerator(note, duration) {
    override fun generate(sample: Int): Float {
        return (2 * (angle(sample) / TWO_PI))
    }
}
class SineWave(note: Note, duration: Seconds) : WaveGenerator(note, duration) {
    override fun generate(sample: Int): Float {
        return sin(angle(sample))
    }
}

class SquareWave(note: Note, duration: Seconds) : WaveGenerator(note, duration) {
    override fun generate(sample: Int): Float {
        val value = sin(angle(sample))
        return if (value > 0f) {
            1f
        } else {
            -1f
        }
    }
}

class TriangleWave(note: Note, duration: Seconds) : WaveGenerator(note, duration) {
    override fun generate(sample: Int): Float {
        val angle = angle(sample)
        return if (angle < PI) {
            2 * angle / PI
        } else {
            2 * (PI - angle) / PI + 1
        }
    }
}
