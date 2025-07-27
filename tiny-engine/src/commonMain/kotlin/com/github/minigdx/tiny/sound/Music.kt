package com.github.minigdx.tiny.sound

import kotlinx.serialization.Serializable

/**
 *
 */
@Serializable
class Music(
    var instruments: Array<Instrument?> =
        arrayOf(
            clarinet,
            violon,
            obos,
            drum,
            custom1,
            custom2,
            custom3,
            custom4,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
        ),
    val musicalBars: Array<MusicalBar> = Array(32) {
        MusicalBar(it, clarinet, clarinet.index)
    },
    val sequences: Array<MusicalSequence> = Array(8) {
        MusicalSequence(it)
    },
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
        harmonics = floatArrayOf(1f),
    )

val custom1 =
    Instrument(
        index = 4,
        name = "custom1",
        wave = Instrument.WaveType.PULSE,
        attack = 0.1f,
        decay = 0.1f,
        sustain = 0.9f,
        release = 0.05f,
        harmonics = floatArrayOf(1f, 0.05f, 0.01f),
    )

val custom2 =
    Instrument(
        index = 5,
        name = "custom2",
        wave = Instrument.WaveType.SAW_TOOTH,
        attack = 0.1f,
        decay = 0.1f,
        sustain = 0.9f,
        release = 0.05f,
        harmonics = floatArrayOf(1f, 0.05f, 0.01f),
    )

val custom3 =
    Instrument(
        index = 6,
        name = "custom3",
        wave = Instrument.WaveType.TRIANGLE,
        attack = 0.1f,
        decay = 0.1f,
        sustain = 0.9f,
        release = 0.05f,
        harmonics = floatArrayOf(1f, 0.05f, 0.01f),
    )

val custom4 =
    Instrument(
        index = 7,
        name = "custom4",
        wave = Instrument.WaveType.SQUARE,
        attack = 0.1f,
        decay = 0.1f,
        sustain = 0.9f,
        release = 0.05f,
        harmonics = floatArrayOf(1f, 0.05f, 0.01f),
    )
