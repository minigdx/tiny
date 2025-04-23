package com.github.minigdx.tiny.sound

import kotlinx.serialization.Serializable

@Serializable
class MusicalSequence(
    val tracks: Array<Track> = Array(4) { Track() },
) {
    /**
     * A track is a sequence of musical bars.
     */
    @Serializable
    class Track(
        var mute: Boolean = false,
        val bars: MutableList<MusicalBar> = mutableListOf(),
    )
}