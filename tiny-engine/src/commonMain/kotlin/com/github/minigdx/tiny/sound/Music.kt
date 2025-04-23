package com.github.minigdx.tiny.sound

import kotlinx.serialization.Serializable

/**
 *
 */
@Serializable
class Music(
    val instruments: Array<Instrument> =
        arrayOf(
            clarinet,
            violon,
            obos,
            drum,
        ),
    val sequences: MutableList<MusicalSequence> = mutableListOf(),
)

val clarinet =
    Instrument(
        index = 0,
        name = "clarinet",
        wave = Instrument.WaveType.TRIANGLE,
        attack = 0.01f,
        decay = 0.1f,
        sustain = 0.8f,
        release = 0.5f,
        harmonics = floatArrayOf(1.1f, 0.3f, 0.031f, 0.039f, 0.345f, 0.290f, 0.0119f),
    )

val violon =
    Instrument(
        index = 1,
        name = "violon",
        wave = Instrument.WaveType.SINE,
        attack = 0.1f,
        decay = 0.1f,
        sustain = 0.9f,
        release = 0.05f,
        harmonics = floatArrayOf(1f, 0.65f, 0.7f, 0.55f, 0.45f, 0.35f, 0.30f),
    )

val obos =
    Instrument(
        index = 2,
        name = "obos",
        wave = Instrument.WaveType.SAW_TOOTH,
        attack = 0.1f,
        decay = 0.1f,
        sustain = 0.9f,
        release = 0.05f,
        harmonics = floatArrayOf(1f, 0.05f, 0.01f),
    )

val drum =
    Instrument(
        index = 3,
        name = "drum",
        wave = Instrument.WaveType.NOISE,
        attack = 0.1f,
        decay = 0.1f,
        sustain = 0.9f,
        release = 0.05f,
        harmonics = floatArrayOf(),
    )
