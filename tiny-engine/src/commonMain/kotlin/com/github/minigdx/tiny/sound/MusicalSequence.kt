package com.github.minigdx.tiny.sound

import com.github.minigdx.tiny.BPM
import com.github.minigdx.tiny.Percent
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient

@Serializable
class MusicalSequence(
    val index: Int,
    val tracks: Array<Track> = Array(4) { Track(it, 0) },
    var tempo: BPM = 120,
) {
    @Serializable
    class Track(
        val index: Int = 0,
        var instrumentIndex: Int,
        var mute: Boolean = false,
        @Transient var instrument: Instrument? = null,
        val beats: MutableList<MusicalNote> = mutableListOf(),
        var volume: Percent = 1f,
    ) {
        init {
            (0..32).forEach { i ->
                beats.add(MusicalNote(null, i.toFloat(), 1f, 1f))
            }
        }
    }
}
