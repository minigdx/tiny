package com.github.minigdx.tiny.sound

import kotlinx.serialization.Serializable

/**
 *
 */
@Serializable
class Music(
    val instruments: Array<Instrument> = Array(8) { index -> Instrument(index) },
    val sequences: MutableList<MusicalSequence> = mutableListOf(),
)