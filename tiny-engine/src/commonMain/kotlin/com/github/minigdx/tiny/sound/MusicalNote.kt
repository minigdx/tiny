package com.github.minigdx.tiny.sound

import com.github.minigdx.tiny.Beats
import com.github.minigdx.tiny.Percent
import com.github.minigdx.tiny.lua.Note
import kotlinx.serialization.Serializable

/**
 * Note that being played.
 * It can't be played without an instrument, that will generate the sound for this note.
 */
@Serializable
class MusicalNote(
    /**
     * The note played.
     * if null, this is a silence. The volume will be 0f also.
     *
     */
    var note: Note?,
    /**
     * On which beat this note is positioned.
     */
    var beat: Beats,
    /**
     * How many beats this note will last.
     */
    var duration: Beats,
    var volume: Percent,
) {
    val endBeat: Float
        get() = beat + duration

    val isSilence: Boolean
        get() = note == null || volume == 0f
}
