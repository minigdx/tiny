package com.github.minigdx.tiny.lua.sfx

import com.github.minigdx.tiny.lua.Note
import com.github.minigdx.tiny.lua.WrapperLuaTable
import com.github.minigdx.tiny.platform.Platform
import com.github.minigdx.tiny.resources.Sound
import com.github.minigdx.tiny.sound.MusicalSequence
import com.github.minigdx.tiny.sound.VirtualSoundBoard
import org.luaj.vm2.LuaTable

/**
 * Lua wrapper for editing a MusicalSequence (music).
 * This wrapper is used by the EditorLib to allow editing music sequences.
 */
class MusicLuaWrapper(
    private val origin: Sound,
    private val sequence: MusicalSequence,
    private val soundBoard: VirtualSoundBoard,
    private val platform: Platform,
) : WrapperLuaTable() {
    init {
        wrap(
            "index",
            { valueOf(sequence.index) },
        )

        wrap(
            "all",
            {
                val luaTable = LuaTable()
                origin.data.music.sequences.map { it.index }.forEach {
                    luaTable.insert(0, valueOf(it))
                }
                luaTable
            },
        )

        wrap(
            "bpm",
            { valueOf(sequence.tempo) },
            { sequence.tempo = it.checkint() },
        )

        // Access a specific track by index (0-3)
        function1("track") { arg ->
            val trackIndex = arg.checkint()
            val track = sequence.tracks.getOrNull(trackIndex) ?: return@function1 NIL
            TrackLuaWrapper(origin, sequence, track, soundBoard)
        }

        // Get all tracks as a table
        wrap("tracks") {
            val result = LuaTable()
            sequence.tracks.forEachIndexed { index, track ->
                result.insert(index + 1, TrackLuaWrapper(origin, sequence, track, soundBoard))
            }
            result
        }

        // Play the music (computes sound just before playing)
        function0("play") {
            val handler = soundBoard.prepare(sequence).also { it.play() }
            val result = WrapperLuaTable()
            result.function0("stop") {
                handler.stop()
                NONE
            }
            result
        }

        // Loop the music
        function0("loop") {
            val handler = soundBoard.prepare(sequence).also { it.loop() }
            val result = WrapperLuaTable()
            result.function0("stop") {
                handler.stop()
                NONE
            }
            result
        }

        // Export music as WAV
        function0("export") {
            val sound = soundBoard.convert(sequence)
            platform.saveWave(sound)
            NONE
        }
    }

    /**
     * Wrapper for a single track within a music sequence.
     */
    class TrackLuaWrapper(
        private val origin: Sound,
        private val sequence: MusicalSequence,
        private val track: MusicalSequence.Track,
        private val soundBoard: VirtualSoundBoard,
    ) : WrapperLuaTable() {
        init {
            wrap(
                "index",
                { valueOf(track.index) },
            )

            wrap(
                "mute",
                { valueOf(track.mute) },
                { track.mute = it.checkboolean() },
            )

            wrap(
                "volume",
                { valueOf(track.volume.toDouble()) },
                { track.volume = it.checkdouble().toFloat() },
            )

            wrap(
                "instrument",
                { valueOf(track.instrumentIndex) },
                {
                    val index = it.checkint()
                    val instrument = origin.data.music.instruments.getOrNull(index) ?: return@wrap
                    track.instrument = instrument
                    track.instrumentIndex = index
                },
            )

            // Set a note at a specific beat
            function1("set_note") { arg ->
                val beat = arg["beat"].todouble().toFloat()
                val noteName = arg["note"].optjstring(null)
                val duration = arg["duration"].optdouble(1.0).toFloat()
                val volume = arg["volume"].optdouble(1.0).toFloat()
                val isRepeating = arg["repeat"].optboolean(false)
                val isOffNote = arg["off"].optboolean(false)

                val note = noteName?.let { Note.fromName(it) }
                val beatIndex = beat.toInt()

                if (beatIndex in 0 until track.beats.size) {
                    track.beats[beatIndex].apply {
                        this.note = note
                        this.duration = duration
                        this.volume = volume
                        this.isRepeating = isRepeating
                        this.isOffNote = isOffNote
                    }
                }

                NONE
            }

            // Remove/clear a note at a specific beat
            function1("remove_note") { arg ->
                val beat = arg["beat"].todouble().toFloat()
                val beatIndex = beat.toInt()

                if (beatIndex in 0 until track.beats.size) {
                    track.beats[beatIndex].apply {
                        this.note = null
                        this.volume = 1f
                        this.isRepeating = false
                        this.isOffNote = false
                    }
                }

                NONE
            }

            // Get all notes in the track
            wrap("notes") {
                val result = LuaTable()
                track.beats.sortedBy { it.beat }
                    .map { musicalNote ->
                        LuaTable().apply {
                            this.set("note", musicalNote.note?.name?.let { valueOf(it) } ?: NIL)
                            this.set("notei", musicalNote.note?.index?.let { valueOf(it) } ?: NIL)
                            this.set("octave", musicalNote.note?.octave?.let { valueOf(it) } ?: NIL)
                            this.set("volume", valueOf(musicalNote.volume.toDouble()))
                            this.set("beat", valueOf(musicalNote.beat.toDouble()))
                            this.set("duration", valueOf(musicalNote.duration.toDouble()))
                            this.set("repeat", valueOf(musicalNote.isRepeating))
                            this.set("off", valueOf(musicalNote.isOffNote))
                        }
                    }.forEachIndexed { index, value ->
                        result.insert(index + 1, value)
                    }
                result
            }

            // Play only this track
            function0("play") {
                val handler = soundBoard.prepare(track).also { it.play() }
                val result = WrapperLuaTable()
                result.function0("stop") {
                    handler.stop()
                    NONE
                }
                result
            }
        }
    }
}
