package com.github.minigdx.tiny.sound

import com.github.minigdx.tiny.Frequency
import com.github.minigdx.tiny.Percent
import com.github.minigdx.tiny.Seconds
import com.github.minigdx.tiny.sound.SoundManager.Companion.TWO_PI
import kotlinx.serialization.Serializable
import kotlin.math.sin

@Serializable
sealed interface Modulation {
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
@Serializable
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

@Serializable
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

/**
 * Tremolo effect — amplitude modulation using a low-frequency oscillator (LFO).
 *
 * Unlike [Modulation] which modulates frequency, tremolo modulates the amplitude
 * of the signal. Typical LFO rates are 2–10 Hz.
 *
 * @param frequency LFO rate in Hz
 * @param depth Modulation depth: 0.0 = no effect, 1.0 = full tremolo
 */
@Serializable
class Tremolo(
    var frequency: Frequency = 0f,
    var depth: Percent = 0f,
) {
    var active: Boolean = false

    fun apply(
        time: Seconds,
        sample: Float,
    ): Float {
        if (!active || depth == 0f) return sample
        // LFO oscillates between (1-depth) and 1.0
        val lfo = (1.0f - depth) + depth * ((sin(TWO_PI * frequency * time) + 1.0f) * 0.5f)
        return sample * lfo
    }
}
