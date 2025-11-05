package com.github.minigdx.tiny.lua.sfx

import com.github.minigdx.tiny.lua.Note
import com.github.minigdx.tiny.lua.WrapperLuaTable
import com.github.minigdx.tiny.sound.Instrument
import com.github.minigdx.tiny.sound.Music
import com.github.minigdx.tiny.sound.Sweep
import com.github.minigdx.tiny.sound.Vibrato
import com.github.minigdx.tiny.sound.VirtualSoundBoard
import org.luaj.vm2.LuaTable

class InstrumentLuaWrapper(
    private val music: Music,
    private val instrument: Instrument,
    private val soundBoard: VirtualSoundBoard,
) : WrapperLuaTable() {
    init {
        wrap(
            "index",
            { valueOf(instrument.index) },
        )

        wrap("all") {
            val luaTable = LuaTable()
            music.instruments.mapNotNull { it?.index }.forEach {
                luaTable.insert(0, valueOf(it))
            }
            luaTable
        }

        wrap(
            "name",
            { instrument.name?.let { valueOf(it) } ?: NIL },
            { instrument.name = it.optjstring(null) },
        )

        wrap(
            "wave",
            { valueOf(instrument.wave.name) },
            {
                instrument.wave = it.checkjstring()
                    ?.let { Instrument.WaveType.valueOf(it) }
                    ?: Instrument.WaveType.SINE
            },
        )

        wrap(
            "attack",
            { valueOf(instrument.attack.toDouble()) },
            { instrument.attack = it.optdouble(0.0).toFloat() },
        )

        wrap(
            "decay",
            { valueOf(instrument.decay.toDouble()) },
            { instrument.decay = it.optdouble(0.0).toFloat() },
        )

        wrap(
            "sustain",
            { valueOf(instrument.sustain.toDouble()) },
            { instrument.sustain = it.optdouble(0.0).toFloat() },
        )

        wrap(
            "release",
            { valueOf(instrument.release.toDouble()) },
            { instrument.release = it.optdouble(0.0).toFloat() },
        )

        wrap("sweep") {
            val sweep = WrapperLuaTable()
            sweep.wrap(
                "active",
                {
                    val value = instrument.modulations[0].active
                    valueOf(value)
                },
                {
                    val newValue = it.optboolean(false)
                    instrument.modulations[0].active = newValue
                },
            )
            sweep.wrap(
                "acceleration",
                { valueOf((instrument.modulations[0] as Sweep).acceleration.toDouble()) },
                { (instrument.modulations[0] as Sweep).acceleration = it.optdouble(0.0).toFloat() },
            )
            sweep.wrap(
                "sweep",
                { valueOf((instrument.modulations[0] as Sweep).sweep.toDouble()) },
                { (instrument.modulations[0] as Sweep).sweep = it.optdouble(0.0).toFloat() },
            )
            sweep
        }

        wrap("vibrato") {
            val vibrato = WrapperLuaTable()
            vibrato.wrap(
                "active",
                {
                    val value = instrument.modulations[1].active
                    valueOf(value)
                },
                {
                    val newValue = it.optboolean(false)
                    instrument.modulations[1].active = newValue
                },
            )
            vibrato.wrap(
                "frequency",
                { valueOf((instrument.modulations[1] as Vibrato).vibratoFrequency.toDouble()) },
                { (instrument.modulations[1] as Vibrato).vibratoFrequency = it.optdouble(0.0).toFloat() },
            )
            vibrato.wrap(
                "depth",
                { valueOf((instrument.modulations[1] as Vibrato).depth.toDouble()) },
                { (instrument.modulations[1] as Vibrato).depth = it.optdouble(0.0).toFloat() },
            )
            vibrato
        }

        function1("note_on") { noteName ->
            val note = Note.fromName(noteName.tojstring())
            soundBoard.noteOn(note, instrument)
            NONE
        }

        function1("note_off") { noteName ->
            val note = Note.fromName(noteName.tojstring())
            soundBoard.noteOff(note)
            NONE
        }

        wrap("harmonics") {
            WrapperLuaTable().apply {
                (0 until instrument.harmonics.size).forEach { index ->
                    wrap(
                        "${index + 1}",
                        { valueOf(instrument.harmonics[index].toDouble()) },
                        { instrument.harmonics[index] = it.tofloat() },
                    )
                }
            }
        }
    }
}
