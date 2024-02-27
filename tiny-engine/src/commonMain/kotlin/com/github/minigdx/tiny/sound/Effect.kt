package com.github.minigdx.tiny.sound

import com.github.minigdx.tiny.Frequency
import com.github.minigdx.tiny.Percent
import com.github.minigdx.tiny.Seconds
import com.github.minigdx.tiny.sound.SoundManager.Companion.SAMPLE_RATE
import com.github.minigdx.tiny.sound.WaveGenerator.Companion.TWO_PI
import kotlin.math.abs
import kotlin.math.sin
import kotlin.random.Random

interface Modulation {

    fun apply(index: Int, frequency: Float): Float
}

/**
 *
 * Change the frequency over time.
 */
class Sweep(
    val sweep: Frequency,
) : Modulation {
    override fun apply(index: Int, frequency: Float): Float {
        return frequency + index * sweep / SAMPLE_RATE.toFloat()
    }
}

class Vibrato(
    val vibratoFrequency: Float,
    val depth: Percent,
) : Modulation {
    override fun apply(index: Int, frequency: Float): Float {
        val t = index / SAMPLE_RATE.toFloat()
        val vibrato = sin(TWO_PI * vibratoFrequency * t) * depth
        return frequency + vibrato
    }
}

sealed interface SoundGenerator {

    val modulation: Modulation?

    val envelope: Envelope?

    val frequency: Float

    fun angle(index: Int): Float {
        val t = index / SAMPLE_RATE.toFloat()

        val apply = modulation?.apply(index, frequency) ?: frequency
        val angle = TWO_PI * apply * t
        return angle
    }

    fun apply(index: Int): Float

    fun generate(index: Int, beatDuration: Int): Float {
        val sample = apply(index)
        return envelope?.apply(sample, index, beatDuration) ?: sample
    }
}

class Sine2(
    override var frequency: Float,
    override val modulation: Modulation? = null,
    override val envelope: Envelope? = null,
) : SoundGenerator {
    override fun apply(index: Int): Float {
        return sin(angle(index)) * 0.7f
    }
}

class Square2(
    override val frequency: Float,
    override val modulation: Modulation?,
    override val envelope: Envelope?,
) : SoundGenerator {
    override fun apply(index: Int): Float {
        val value = sin(angle(index))
        return if (value > 0f) {
            0.7f
        } else {
            -0.7f
        }
    }
}

class Triangle2(
    override val frequency: Float,
    override val modulation: Modulation?,
    override val envelope: Envelope?,
) : SoundGenerator {
    override fun apply(index: Int): Float {
        val angle: Float = sin(angle(index))
        val phase = (angle + 1.0) % 1.0 // Normalize sinValue to the range [0, 1]
        return (if (phase < 0.5) 4.0 * phase - 1.0 else 3.0 - 4.0 * phase).toFloat()
    }
}

class Pulse2(
    override val frequency: Float,
    override val modulation: Modulation?,
    override val envelope: Envelope?,
) : SoundGenerator {
    override fun apply(index: Int): Float {
        val angle = angle(index)

        val t = angle % 1
        val k = abs(2.0 * ((angle / 128.0) % 1.0) - 1.0)
        val u = (t + 0.5 * k) % 1.0
        val ret = abs(4.0 * u - 2.0) - abs(8.0 * t - 4.0)
        return (ret / 6.0).toFloat()
    }
}

class SawTooth2(
    override val frequency: Float,
    override val modulation: Modulation?,
    override val envelope: Envelope?,
) : SoundGenerator {
    override fun apply(index: Int): Float {
        val angle: Float = sin(angle(index))
        val phase = (angle * 2f) - 1f
        return phase
    }
}

class Silence2(
    override val frequency: Float,
    override val modulation: Modulation?,
    override val envelope: Envelope?,
) : SoundGenerator {
    override fun apply(index: Int): Float = 0f
}

class Noise2(
    override val frequency: Float,
    override val modulation: Modulation?,
    override val envelope: Envelope?,
) : SoundGenerator {
    override fun apply(index: Int): Float {
        val white = Random.nextFloat() * 2 - 1
        val brown = (lastNoise + (0.02f * white)) / 1.02f
        lastNoise = brown
        return brown * 3.5f
    }

    private var lastNoise = 0.0f
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
) {

    private val endOfAttackIndex = (attack * SAMPLE_RATE).toInt()

    private val decayDuration = (decay * SAMPLE_RATE).toInt()

    private val endOfDecay = endOfAttackIndex + decayDuration

    private val releaseDuration = (release * SAMPLE_RATE).toInt()

    fun apply(sample: Float, index: Int, nbSample: Int): Float {
        // attack phase
        if (index <= endOfAttackIndex) {
            val percentAttack = index / endOfAttackIndex.toFloat()
            return sample * percentAttack
        } else if (index > endOfAttackIndex && index <= endOfDecay) { // decay phase
            val percentDecay = (index - endOfAttackIndex) / decayDuration.toFloat()
            return sample * (1f - (1f - sustain) * percentDecay)
        } else if (index > endOfDecay && index <= nbSample - releaseDuration) { // sustain phase
            return sample * sustain
        } else { // release phase
            val percentRelease = (index - (nbSample - releaseDuration)) / releaseDuration.toFloat()
            return sample * (sustain * (1f - percentRelease))
        }
    }
}
