package com.github.minigdx.tiny.sound

import com.github.minigdx.tiny.Frequency
import com.github.minigdx.tiny.Percent
import com.github.minigdx.tiny.Seconds
import com.github.minigdx.tiny.sound.SoundManager.Companion.TWO_PI
import kotlin.math.sin

interface Modulation {
    /**
     * Is the Modulation Effect active?
     */
    var active: Boolean

    fun apply(
        time: Seconds,
        frequency: Frequency,
    ): Frequency
}

/**
 *
 * Change the frequency over time.
 */
class Sweep(
    var sweep: Frequency,
    var acceleration: Percent,
) : Modulation {
    override var active: Boolean = false

    private val way: Float
        get() {
            return acceleration * 2f - 1f
        }

    override fun apply(
        time: Seconds,
        frequency: Frequency,
    ): Frequency {
        return frequency + time * (sweep * way)
    }
}

class Vibrato(
    var vibratoFrequency: Frequency,
    var depth: Percent,
) : Modulation {
    override var active: Boolean = false

    override fun apply(
        time: Seconds,
        frequency: Frequency,
    ): Frequency {
        val vibrato = sin(TWO_PI * vibratoFrequency * time) * depth
        return frequency + vibrato
    }
}
