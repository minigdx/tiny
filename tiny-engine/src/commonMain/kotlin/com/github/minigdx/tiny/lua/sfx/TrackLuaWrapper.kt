package com.github.minigdx.tiny.lua.sfx

import com.github.minigdx.tiny.lua.Note
import com.github.minigdx.tiny.lua.WrapperLuaTable
import com.github.minigdx.tiny.sound.Music
import com.github.minigdx.tiny.sound.MusicalNote
import com.github.minigdx.tiny.sound.MusicalPhrase
import org.luaj.vm2.LuaTable

class TrackLuaWrapper(
    private val music: Music,
    private val phrase: MusicalPhrase,
) : WrapperLuaTable() {
    init {
        wrap(
            "index",
            { valueOf(phrase.index) },
        )

        wrap(
            "instrument",
            { valueOf(phrase.instrumentIndex) },
            {
                val index = it.checkint()
                val instrument = music.instruments.getOrNull(index)
                if (instrument != null) {
                    phrase.instrumentIndex = index
                    phrase.instrument = instrument
                }
            },
        )

        wrap(
            "volume",
            { valueOf(phrase.volume.toDouble()) },
            { phrase.volume = it.todouble().toFloat().coerceIn(0f, 1f) },
        )

        wrap(
            "mute",
            { valueOf(phrase.mute) },
            { phrase.mute = it.checkboolean() },
        )

        wrap(
            "duration",
            { valueOf(phrase.duration) },
            { phrase.duration = it.checkint() },
        )

        wrap("beats") {
            val result = LuaTable()
            phrase.beats.sortedBy { it.beat }
                .map {
                    LuaTable().apply {
                        this.set("note", it.note?.name?.let { valueOf(it) } ?: NIL)
                        this.set("notei", it.note?.index?.let { valueOf(it) } ?: NIL)
                        this.set("octave", it.note?.octave?.let { valueOf(it) } ?: NIL)
                        this.set("volume", valueOf(it.volume.toDouble()))
                        this.set("beat", valueOf(it.beat.toDouble()))
                        this.set("duration", valueOf(it.duration.toDouble()))
                        this.set("off", valueOf(it.isOffNote))
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
            val isOff = arg["off"].optboolean(false)

            phrase.setBeatAt(
                beat,
                MusicalNote(
                    note = note,
                    beat = beat.toFloat(),
                    duration = duration,
                    volume = volume,
                    isOffNote = isOff,
                ),
            )
            NONE
        }

        function0("clear") {
            phrase.resetBeats()
            NONE
        }
    }
}
