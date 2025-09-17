package com.github.minigdx.tiny.sound

import com.github.minigdx.tiny.Percent
import com.github.minigdx.tiny.Sample

class Envelop(
    private val attack: Sample,
    private val decay: Sample,
    private val sustain: Percent,
    private val release: Sample,
) {

    /**
     * Return the multiplier to apply to a sample value regarding the progression of the sound.
     * The [noteOn] phase will apply the [attack] then the [decay] then the [sustain]
     * and keep the [sustain] until the [noteOff].
     */
    fun noteOn(progress: Sample): Percent {
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
        return when {
            progress <= attack + decay -> noteOn(progress)
            progress <= attack + decay + release -> {
                // Release phase: sustain to 0.0 over release samples
                if (release == 0) {
                    0.0f
                } else {
                    val releaseProgress = progress - attack - decay
                    sustain * (1.0f - releaseProgress.toFloat() / release.toFloat())
                }
            }
            else -> {
                // After release: silence
                0.0f
            }
        }
    }
}