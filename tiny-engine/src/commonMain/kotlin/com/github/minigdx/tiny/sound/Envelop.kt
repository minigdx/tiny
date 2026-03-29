package com.github.minigdx.tiny.sound

import com.github.minigdx.tiny.Percent
import com.github.minigdx.tiny.Sample
import kotlin.math.max

class Envelop(
    internal val attack0: () -> Sample,
    internal val decay0: () -> Sample,
    internal val sustain0: () -> Percent,
    internal val release0: () -> Sample,
) {
    /**
     * Return the multiplier to apply to a sample value regarding the progression of the sound.
     * The [noteOn] phase will apply the [attack] then the [decay] then the [sustain]
     * and keep the [sustain] until the [noteOff].
     *
     * Uses quadratic curves for more natural-sounding envelopes:
     * - Attack uses x^2 (slow start, fast finish)
     * - Decay uses inverse-square (fast drop, slow tail toward sustain)
     */
    fun noteOn(progress: Sample): Percent {
        val attack = attack0.invoke()
        val decay = decay0.invoke()
        val sustain = sustain0.invoke()

        return when {
            progress < 0 -> 0.0f
            progress <= attack -> {
                // Attack phase: 0.0 to 1.0 with quadratic curve (slow start, fast finish)
                if (attack == 0) {
                    1.0f
                } else {
                    val linear = progress.toFloat() / attack.toFloat()
                    linear * linear
                }
            }
            progress <= attack + decay -> {
                // Decay phase: 1.0 to sustain with inverse-square curve (fast drop, slow tail)
                if (decay == 0) {
                    sustain
                } else {
                    val decayProgress = progress - attack
                    val linear = decayProgress.toFloat() / decay.toFloat()
                    val remaining = 1.0f - linear
                    sustain + (1.0f - sustain) * remaining * remaining
                }
            }
            else -> {
                // Sustain phase: stay at sustain level
                sustain
            }
        }
    }

    /**
     * Return the multiplier to apply to a sample value regarding the progression of the sound.
     * The [noteOff] will apply the [attack] then the [decay] then right away the [release].
     *
     * Enforces a minimum release duration of ~2ms (88 samples at 44100 Hz)
     * to prevent audible clicks from abrupt note endings.
     *
     * Uses quadratic curve (fast drop, slow tail) for natural release.
     */
    fun noteOff(progress: Sample): Percent {
        val sustain = sustain0.invoke()
        val release = max(MIN_RELEASE_SAMPLES, release0.invoke())

        return when {
            progress < 0 -> 0.0f
            progress <= release -> {
                // Release phase: sustain to 0.0 with quadratic curve (fast drop, slow tail)
                val linear = progress.toFloat() / release.toFloat()
                val remaining = 1.0f - linear
                sustain * remaining * remaining
            }
            else -> {
                // After release: silence
                0.0f
            }
        }
    }

    companion object {
        // ~2ms at 44100 Hz - minimum release to prevent clicks
        const val MIN_RELEASE_SAMPLES = 88
    }
}
