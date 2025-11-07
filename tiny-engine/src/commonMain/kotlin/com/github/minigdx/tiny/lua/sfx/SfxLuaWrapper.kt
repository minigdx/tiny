package com.github.minigdx.tiny.lua.sfx

import com.github.minigdx.tiny.lua.Note
import com.github.minigdx.tiny.lua.WrapperLuaTable
import com.github.minigdx.tiny.platform.Platform
import com.github.minigdx.tiny.resources.Sound
import com.github.minigdx.tiny.sound.MusicalBar
import com.github.minigdx.tiny.sound.VirtualSoundBoard
import org.luaj.vm2.LuaTable

class SfxLuaWrapper(
    private val origin: Sound,
    private val sfx: MusicalBar,
    private val soundBoard: VirtualSoundBoard,
    private val platform: Platform,
) : WrapperLuaTable() {
    init {
        wrap(
            "index",
            { valueOf(sfx.index) },
        )

        wrap(
            "all",
            {
                val luaTable = LuaTable()
                origin.data.music.musicalBars.map { it.index }.forEach {
                    luaTable.insert(0, valueOf(it))
                }
                luaTable
            },
        )

        wrap(
            "bpm",
            { valueOf(sfx.tempo) },
            { sfx.tempo = it.checkint() },
        )

        function1("set_volume") { arg ->
            val beat = arg["beat"].todouble().toFloat()
            val volume = arg["volume"].todouble().toFloat()

            sfx.setVolume(beat, volume)

            NONE
        }

        function1("set_note") { arg ->
            val beat = arg["beat"].todouble().toFloat()
            val note = Note.Companion.fromName(arg["note"].tojstring())
            val duration = arg["duration"].todouble().toFloat()
            val uniqueOnBeat = arg["unique"].optboolean(false)

            sfx.setNote(note, beat, duration, uniqueOnBeat)

            NONE
        }

        function1("remove_note") { arg ->
            val beat = arg["beat"].todouble().toFloat()
            val note = Note.Companion.fromName(arg["note"].tojstring())

            sfx.removeNote(note, beat)

            NONE
        }

        function1("note_data") { arg ->
            val note = Note.Companion.fromName(arg.tojstring())

            LuaTable().apply {
                this.set("note", valueOf(note.name))
                this.set("notei", valueOf(note.index))
                this.set("octave", valueOf(note.octave))
            }
        }

        wrap("notes") {
            val result = LuaTable()
            sfx.beats.sortedBy { it.beat }
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

        function0("play") {
            val handler = soundBoard.prepare(sfx).also { it.play() }
            val result = WrapperLuaTable()
            result.function0("stop") {
                handler.stop()
                NONE
            }
            result
        }

        function1("set_instrument") { arg ->
            if (arg.isnil()) {
                sfx.instrument?.let { valueOf(it.index) } ?: NIL
            } else {
                val index = arg.checkint()
                val instrument = origin.data.music.instruments.getOrNull(index) ?: return@function1 NIL
                sfx.instrument = instrument
                sfx.instrumentIndex = instrument.index
                NONE
            }
        }

        wrap("instrument") {
            sfx.instrument?.index?.let { valueOf(it) } ?: NIL
        }

        function0("export") {
            val sound = soundBoard.convert(sfx)
            platform.saveWave(sound)
            NONE
        }
    }
}
