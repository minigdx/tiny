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
            custom1,
            custom2,
            custom3,
            custom4,
            kick,
            snare,
            hihatClosed,
            hihatOpen,
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

/**
 * Kick drum (bass drum) instrument.
 * Uses the KICK wave type with a punchy envelope.
 * Play with low notes (C1-C2) for best results.
 */
val kick =
    Instrument(
        index = 8,
        name = "kick",
        wave = Instrument.WaveType.KICK,
        attack = 0.001f, // Very fast attack for punch
        decay = 0.15f, // Short decay
        sustain = 0.0f, // No sustain
        release = 0.1f, // Quick release
        harmonics = floatArrayOf(1f),
    )

/**
 * Snare drum instrument.
 * Combines a pitched body with noise for snare wires.
 * Play with notes around C3-E3 for best results.
 */
val snare =
    Instrument(
        index = 9,
        name = "snare",
        wave = Instrument.WaveType.SNARE,
        attack = 0.001f, // Very fast attack
        decay = 0.1f, // Short decay
        sustain = 0.1f, // Low sustain for snare tail
        release = 0.15f, // Medium release for snare ring
        harmonics = floatArrayOf(1f),
    )

/**
 * Closed hi-hat instrument.
 * High-frequency metallic noise with very short decay.
 * The note pitch controls the brightness.
 */
val hihatClosed =
    Instrument(
        index = 10,
        name = "hihat_closed",
        wave = Instrument.WaveType.HIHAT_CLOSED,
        attack = 0.001f, // Instant attack
        decay = 0.03f, // Very short decay
        sustain = 0.0f, // No sustain
        release = 0.02f, // Very short release
        harmonics = floatArrayOf(1f),
    )

/**
 * Open hi-hat instrument.
 * Similar to closed hi-hat but with longer sustain and release.
 * The note pitch controls the brightness.
 */
val hihatOpen =
    Instrument(
        index = 11,
        name = "hihat_open",
        wave = Instrument.WaveType.HIHAT_OPEN,
        attack = 0.001f, // Instant attack
        decay = 0.1f, // Longer decay than closed
        sustain = 0.3f, // Some sustain
        release = 0.3f, // Longer release for open sound
        harmonics = floatArrayOf(1f),
    )
