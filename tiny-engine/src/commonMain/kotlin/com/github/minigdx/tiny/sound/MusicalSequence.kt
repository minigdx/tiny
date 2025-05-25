package com.github.minigdx.tiny.sound

import com.github.minigdx.tiny.BPM
import com.github.minigdx.tiny.Percent
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient

@Serializable
class MusicalSequence(
    val tracks: Array<Track> = Array(4) { Track() },
    var tempo: BPM = 120,
) {
    @Serializable
    class Track(
        var mute: Boolean = false,
        var instrumentIndex: Int? = null,
        @Transient var instrument: Instrument? = null,
        val beats: MutableList<MusicalNote> = mutableListOf(),
        val volume: Percent = 1f,
    )
}
