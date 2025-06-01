package com.github.minigdx.tiny.sound

import com.github.minigdx.tiny.Beats
import com.github.minigdx.tiny.Percent
import com.github.minigdx.tiny.lua.Note
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient

/**
 * Note that being played.
 * It can't be played without an instrument, that will generate the sound for this note.
 */
@Serializable
data class MusicalNote(
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
    /**
     * Volume of this note
     */
    var volume: Percent,
    /**
     * Index of the override instrument.
     */
    var instrumentIndex: Int? = null,
    /**
     * Override instrument.
     */
    @Transient
    var instrument: Instrument? = null,
    /**
     * Inside a track, is the next note will be a repeat of this note?
     */
    var isRepeating: Boolean = false,
    /**
     * Inside a track, stop the sound on this note.
     */
    var isOffNote: Boolean = false,
)
