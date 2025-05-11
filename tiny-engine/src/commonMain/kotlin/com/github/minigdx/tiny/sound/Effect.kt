package com.github.minigdx.tiny.sound

import com.github.minigdx.tiny.Frequency
import com.github.minigdx.tiny.Percent
import com.github.minigdx.tiny.sound.SoundManager.Companion.SAMPLE_RATE
import com.github.minigdx.tiny.sound.SoundManager.Companion.TWO_PI
import kotlin.math.sin

interface Modulation {
    fun apply(
        index: Int,
        frequency: Float,
    ): Float
}

/**
 *
 * Change the frequency over time.
 */
class Sweep(
    val sweep: Frequency,
    val acceleration: Boolean,
) : Modulation {
    private val way =
        if (acceleration) {
            1
        } else {
            -1
        }

    override fun apply(
        index: Int,
        frequency: Float,
    ): Float {
        return frequency + index * (sweep * way) / SAMPLE_RATE.toFloat()
    }
}

class Vibrato(
    val vibratoFrequency: Float,
    val depth: Percent,
) : Modulation {
    override fun apply(
        index: Int,
        frequency: Float,
    ): Float {
        val t = index / SAMPLE_RATE.toFloat()
        val vibrato = sin(TWO_PI * vibratoFrequency * t) * depth
        return frequency + vibrato
    }
}
