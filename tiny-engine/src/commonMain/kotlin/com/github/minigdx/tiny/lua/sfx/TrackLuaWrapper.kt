package com.github.minigdx.tiny.lua.sfx

import com.github.minigdx.tiny.lua.Note
import com.github.minigdx.tiny.lua.WrapperLuaTable
import com.github.minigdx.tiny.sound.Music
import com.github.minigdx.tiny.sound.MusicalNote
import com.github.minigdx.tiny.sound.MusicalSequence
import org.luaj.vm2.LuaTable

class TrackLuaWrapper(
    private val music: Music,
    private val track: MusicalSequence.Track,
) : WrapperLuaTable() {
    init {
        wrap(
            "index",
            { valueOf(track.index) },
        )

        wrap(
            "instrument",
            { valueOf(track.instrumentIndex) },
            {
                val index = it.checkint()
                val instrument = music.instruments.getOrNull(index)
                if (instrument != null) {
                    track.instrumentIndex = index
                    track.instrument = instrument
                }
            },
        )

        wrap(
            "volume",
            { valueOf(track.volume.toDouble()) },
            { track.volume = it.todouble().toFloat().coerceIn(0f, 1f) },
        )

        wrap(
            "mute",
            { valueOf(track.mute) },
            { track.mute = it.checkboolean() },
        )

        wrap("beats") {
            val result = LuaTable()
            track.beats.sortedBy { it.beat }
                .map {
                    LuaTable().apply {
                        this.set("note", it.note?.name?.let { valueOf(it) } ?: NIL)
                        this.set("notei", it.note?.index?.let { valueOf(it) } ?: NIL)
                        this.set("octave", it.note?.octave?.let { valueOf(it) } ?: NIL)
                        this.set("volume", valueOf(it.volume.toDouble()))
                        this.set("beat", valueOf(it.beat.toDouble()))
                        this.set("duration", valueOf(it.duration.toDouble()))
                    }
                }.forEachIndexed { index, value ->
                    result.insert(index + 1, value)
                }
            result
        }

        function1("set_note") { arg ->
            val beat = arg["beat"].checkint()
            val noteName = arg["note"].tojstring()
            val note = Note.Companion.fromName(noteName)
            val volume = arg["volume"].optdouble(1.0).toFloat().coerceIn(0f, 1f)
            val duration = arg["duration"].optdouble(1.0).toFloat()

            if (beat in track.beats.indices) {
                track.beats[beat] = MusicalNote(note, beat.toFloat(), duration, volume)
            }
            NONE
        }

        function0("clear") {
            track.beats.indices.forEach { i ->
                track.beats[i] = MusicalNote(null, i.toFloat(), 1f, 1f)
            }
            NONE
        }
    }
}
