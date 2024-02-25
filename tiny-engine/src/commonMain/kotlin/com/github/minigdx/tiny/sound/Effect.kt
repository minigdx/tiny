package com.github.minigdx.tiny.sound

import com.github.minigdx.tiny.Frequency
import com.github.minigdx.tiny.Percent
import com.github.minigdx.tiny.Seconds
import com.github.minigdx.tiny.sound.SoundManager.Companion.SAMPLE_RATE
import com.github.minigdx.tiny.sound.WaveGenerator.Companion.TWO_PI
import kotlin.math.sin

interface Effect {
    /**
     * Set the sample at the [index] in the [buffer].
     *
     */
    fun apply(index: Int, buffer: FloatArray)
}

// update frequencies
interface WaveEffect {

    fun apply(index: Int, frequency: Float): Float
}

/**
 *
 * Change the frequency over time.
 */
class Sweep(
    val sweep: Frequency,
) : WaveEffect {
    override fun apply(index: Int, frequency: Float): Float {
        return frequency + index * sweep / SAMPLE_RATE.toFloat()
    }
}

class Vibrato(
    val vibratoFrequency: Float,
    val depth: Percent
) : WaveEffect {
    override fun apply(index: Int, frequency: Float): Float {
        val t = index / SAMPLE_RATE.toFloat()
        val vibrato = sin(TWO_PI * vibratoFrequency * t) * depth
        return frequency + vibrato
    }

}

sealed interface Wave : Effect {

    val waveEffets: Array<WaveEffect>

    val frequency: Float

    fun angle(index: Int): Float {
        val t = index / SAMPLE_RATE.toFloat()

        var f = frequency
        waveEffets.forEach {
            f = it.apply(index, f)
        }

        val angle = TWO_PI * f * t
        return angle
    }
}

class SineWave2(
    override var frequency: Float,
    override val waveEffets: Array<WaveEffect> = emptyArray(),
) : Wave {
    override fun apply(index: Int, buffer: FloatArray) {
        buffer[index] = sin(angle(index)) * 0.7f
    }
}


/**
 * Volume envelope. It changes the volume level over time.
 */
class Envelope(
    /**
     * Time to reach the maximum level volume.
     */
    val attack: Seconds,
    /**
     * Time from the maximum level volume to the sustain volume.
     */
    val decay: Seconds,
    /**
     * Sustain volume.
     */
    val sustain: Percent,
    /**
     * Time to reach the volume 0
     */
    val release: Seconds,
) : Effect {

    private val endOfAttackIndex = (attack * SAMPLE_RATE).toInt()

    private val decayDuration = (decay * SAMPLE_RATE).toInt()

    private val endOfDecay = endOfAttackIndex + decayDuration

    private val releaseDuration = (release * SAMPLE_RATE).toInt()

    override fun apply(index: Int, buffer: FloatArray) {
        // attack phase
        if (index <= endOfAttackIndex) {
            val percentAttack = index / endOfAttackIndex.toFloat()
            buffer[index] *= percentAttack
        } else if (index > endOfAttackIndex && index <= endOfDecay) { // decay phase
            val percentDecay = (index - endOfAttackIndex) / decayDuration.toFloat()
            buffer[index] *= 1f - (1f - sustain) * percentDecay
        } else if (index > endOfDecay && index <= buffer.size - releaseDuration) { // sustain phase
            buffer[index] *= sustain
        } else { // release phase
            val percentRelease = (index - (buffer.size - releaseDuration)) / releaseDuration.toFloat()
            buffer[index] *= sustain * (1f - percentRelease)
        }
    }
}