package com.github.minigdx.tiny.sound

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

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
            bass,
            lead,
            pluck,
            snare,
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
) {
    fun serialize(): String {
        return serialize(this)
    }

    companion object {
        private val json = Json { ignoreUnknownKeys = true }

        fun serialize(music: Music): String {
            val json = json.encodeToString(music)
            return json
        }

        fun deserialize(data: String): Music {
            val music: Music = json.decodeFromString(data)
            return music
        }
    }
}

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
        wave = Instrument.WaveType.DRUM,
        attack = 0.001f,
        decay = 0.01f,
        sustain = 1.0f,
        release = 0.05f,
        harmonics = floatArrayOf(1f),
    )

val bass =
    Instrument(
        index = 4,
        name = "bass",
        wave = Instrument.WaveType.PULSE,
        dutyCycle = 0.25f,
        attack = 0.005f,
        decay = 0.15f,
        sustain = 0.6f,
        release = 0.1f,
        harmonics = floatArrayOf(1.0f, 0.5f, 0.25f, 0.12f, 0.06f, 0.0f, 0.0f),
    )

val lead =
    Instrument(
        index = 5,
        name = "lead",
        wave = Instrument.WaveType.SQUARE,
        attack = 0.01f,
        decay = 0.08f,
        sustain = 0.85f,
        release = 0.15f,
        harmonics = floatArrayOf(1.0f, 0.0f, 0.45f, 0.0f, 0.25f, 0.0f, 0.15f),
    )

val pluck =
    Instrument(
        index = 6,
        name = "pluck",
        wave = Instrument.WaveType.TRIANGLE,
        attack = 0.002f,
        decay = 0.2f,
        sustain = 0.05f,
        release = 0.08f,
        harmonics = floatArrayOf(1.0f, 0.4f, 0.3f, 0.15f, 0.08f, 0.04f, 0.02f),
    )

val snare =
    Instrument(
        index = 7,
        name = "snare",
        wave = Instrument.WaveType.NOISE,
        attack = 0.001f,
        decay = 0.08f,
        sustain = 0.1f,
        release = 0.05f,
        harmonics = floatArrayOf(1.0f, 0.8f, 0.5f, 0.3f),
    )
