package com.github.minigdx.tiny.sound

import kotlinx.serialization.EncodeDefault
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.Serializable

@OptIn(ExperimentalSerializationApi::class)
@Serializable
data class MusicConfiguration(
    @EncodeDefault val root: String = "C",
    @EncodeDefault val scaleName: String = "Major",
    @EncodeDefault val progressionName: String = "Classic",
    @EncodeDefault val leadStyle: String = "Stepwise",
    @EncodeDefault val drumPattern: String = "Rock",
    @EncodeDefault val chordInstrument: Int = 0,
    @EncodeDefault val bassInstrument: Int = 1,
    @EncodeDefault val leadInstrument: Int = 2,
    @EncodeDefault val drumInstrument: Int = 3,
    @EncodeDefault val chordVolume: Float = 0.3f,
    @EncodeDefault val bassVolume: Float = 0.4f,
    @EncodeDefault val leadVolume: Float = 0.25f,
    @EncodeDefault val drumVolume: Float = 0.35f,
    @EncodeDefault val bpm: Int = 120,
    @EncodeDefault val seed: Long = 42L,
)
