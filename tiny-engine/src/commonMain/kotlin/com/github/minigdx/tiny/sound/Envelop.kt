package com.github.minigdx.tiny.sound

import com.github.minigdx.tiny.Percent
import com.github.minigdx.tiny.Sample

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
     */
    fun noteOn(progress: Sample): Percent {
        val attack = attack0.invoke()
        val decay = decay0.invoke()
        val sustain = sustain0.invoke()

        return when {
            progress < 0 -> 0.0f
            progress <= attack -> {
                // Attack phase: 0.0 to 1.0 over attack samples
                if (attack == 0) {
                    1.0f
                } else {
                    progress.toFloat() / attack.toFloat()
                }
            }
            progress <= attack + decay -> {
                // Decay phase: 1.0 to sustain over decay samples
                if (decay == 0) {
                    sustain
                } else {
                    val decayProgress = progress - attack
                    val decayAmount = 1.0f - sustain
                    1.0f - (decayAmount * decayProgress.toFloat() / decay.toFloat())
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
     */
    fun noteOff(progress: Sample): Percent {
        val sustain = sustain0.invoke()
        val release = release0.invoke()

        return when {
            progress <= release -> {
                // Release phase: sustain to 0.0 over release samples
                if (release == 0) {
                    0.0f
                } else {
                    sustain * (1.0f - progress.toFloat() / release.toFloat())
                }
            }
            else -> {
                // After release: silence
                0.0f
            }
        }
    }

    /**
     * Return the multiplier for release phase starting from a specific amplitude level.
     * Used when noteOff is called during attack or decay phases.
     */
    fun noteOff(progress: Sample, startLevel: Percent): Percent {
        val release = release0.invoke()

        return when {
            progress <= release -> {
                // Release phase: startLevel to 0.0 over release samples
                if (release == 0) {
                    0.0f
                } else {
                    startLevel * (1.0f - progress.toFloat() / release.toFloat())
                }
            }
            else -> {
                // After release: silence
                0.0f
            }
        }
    }
}
