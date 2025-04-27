package com.github.minigdx.tiny.lua

import com.github.mingdx.tiny.doc.TinyArg
import com.github.mingdx.tiny.doc.TinyCall
import com.github.mingdx.tiny.doc.TinyFunction
import com.github.mingdx.tiny.doc.TinyLib
import org.luaj.vm2.LuaTable
import org.luaj.vm2.LuaValue
import org.luaj.vm2.lib.OneArgFunction
import org.luaj.vm2.lib.TwoArgFunction
import kotlin.math.abs

private const val OCTAVE_0 = 0
private const val OCTAVE_1 = OCTAVE_0 + 12
private const val OCTAVE_2 = OCTAVE_1 + 12
private const val OCTAVE_3 = OCTAVE_2 + 12
private const val OCTAVE_4 = OCTAVE_3 + 12
private const val OCTAVE_5 = OCTAVE_4 + 12
private const val OCTAVE_6 = OCTAVE_5 + 12
private const val OCTAVE_7 = OCTAVE_6 + 12
private const val OCTAVE_8 = OCTAVE_7 + 12

enum class Note(val frequency: Float, val index: Int) {
    C0(16.35f, OCTAVE_0 + 1),
    Cs0(17.32f, OCTAVE_0 + 2),
    Db0(17.32f, OCTAVE_0 + 2),
    D0(18.35f, OCTAVE_0 + 3),
    Ds0(19.45f, OCTAVE_0 + 4),
    Eb0(19.45f, OCTAVE_0 + 4),
    E0(20.60f, OCTAVE_0 + 5),
    F0(21.83f, OCTAVE_0 + 6),
    Fs0(23.12f, OCTAVE_0 + 7),
    Gb0(23.12f, OCTAVE_0 + 7),
    G0(24.50f, OCTAVE_0 + 8),
    Gs0(25.96f, OCTAVE_0 + 9),
    Ab0(25.96f, OCTAVE_0 + 9),
    A0(27.50f, OCTAVE_0 + 10),
    As0(29.14f, OCTAVE_0 + 11),
    Bb0(29.14f, OCTAVE_0 + 11),
    B0(30.87f, OCTAVE_0 + 12),

    C1(32.70f, OCTAVE_1 + 1),
    Cs1(34.65f, OCTAVE_1 + 2),
    Db1(34.65f, OCTAVE_1 + 2),
    D1(36.71f, OCTAVE_1 + 3),
    Ds1(38.89f, OCTAVE_1 + 4),
    Eb1(38.89f, OCTAVE_1 + 4),
    E1(41.20f, OCTAVE_1 + 5),
    F1(43.65f, OCTAVE_1 + 6),
    Fs1(46.25f, OCTAVE_1 + 7),
    Gb1(46.25f, OCTAVE_1 + 7),
    G1(49.00f, OCTAVE_1 + 8),
    Gs1(51.91f, OCTAVE_1 + 9),
    Ab1(51.91f, OCTAVE_1 + 9),
    A1(55.00f, OCTAVE_1 + 10),
    As1(58.27f, OCTAVE_1 + 11),
    Bb1(58.27f, OCTAVE_1 + 11),
    B1(61.74f, OCTAVE_1 + 12),

    C2(65.41f, OCTAVE_2 + 1),
    Cs2(69.30f, OCTAVE_2 + 2),
    Db2(69.30f, OCTAVE_2 + 2),
    D2(73.42f, OCTAVE_2 + 3),
    Ds2(77.78f, OCTAVE_2 + 4),
    Eb2(77.78f, OCTAVE_2 + 4),
    E2(82.41f, OCTAVE_2 + 5),
    F2(87.31f, OCTAVE_2 + 6),
    Fs2(92.50f, OCTAVE_2 + 7),
    Gb2(92.50f, OCTAVE_2 + 7),
    G2(98.00f, OCTAVE_2 + 8),
    Gs2(103.83f, OCTAVE_2 + 9),
    Ab2(103.83f, OCTAVE_2 + 9),
    A2(110.00f, OCTAVE_2 + 10),
    As2(116.54f, OCTAVE_2 + 11),
    Bb2(116.54f, OCTAVE_2 + 11),
    B2(123.47f, OCTAVE_2 + 12),

    C3(130.81f, OCTAVE_3 + 1),
    Cs3(138.59f, OCTAVE_3 + 2),
    Db3(138.59f, OCTAVE_3 + 2),
    D3(146.83f, OCTAVE_3 + 3),
    Ds3(155.56f, OCTAVE_3 + 4),
    Eb3(155.56f, OCTAVE_3 + 4),
    E3(164.81f, OCTAVE_3 + 5),
    F3(174.61f, OCTAVE_3 + 6),
    Fs3(185.00f, OCTAVE_3 + 7),
    Gb3(185.00f, OCTAVE_3 + 7),
    G3(196.00f, OCTAVE_3 + 8),
    Gs3(207.65f, OCTAVE_3 + 9),
    Ab3(207.65f, OCTAVE_3 + 9),
    A3(220.00f, OCTAVE_3 + 10),
    As3(233.08f, OCTAVE_3 + 11),
    Bb3(233.08f, OCTAVE_3 + 11),
    B3(246.94f, OCTAVE_3 + 12),

    C4(261.63f, OCTAVE_4 + 1),
    Cs4(277.18f, OCTAVE_4 + 2),
    Db4(277.18f, OCTAVE_4 + 2),
    D4(293.66f, OCTAVE_4 + 3),
    Ds4(311.13f, OCTAVE_4 + 4),
    Eb4(311.13f, OCTAVE_4 + 4),
    E4(329.63f, OCTAVE_4 + 5),
    F4(349.23f, OCTAVE_4 + 6),
    Fs4(369.99f, OCTAVE_4 + 7),
    Gb4(369.99f, OCTAVE_4 + 7),
    G4(392.00f, OCTAVE_4 + 8),
    Gs4(415.30f, OCTAVE_4 + 9),
    Ab4(415.30f, OCTAVE_4 + 9),
    A4(440.00f, OCTAVE_4 + 10),
    As4(466.16f, OCTAVE_4 + 11),
    Bb4(466.16f, OCTAVE_4 + 11),
    B4(493.88f, OCTAVE_4 + 12),

    C5(523.25f, OCTAVE_5 + 1),
    Cs5(554.37f, OCTAVE_5 + 2),
    Db5(554.37f, OCTAVE_5 + 2),
    D5(587.33f, OCTAVE_5 + 3),
    Ds5(622.25f, OCTAVE_5 + 4),
    Eb5(622.25f, OCTAVE_5 + 4),
    E5(659.26f, OCTAVE_5 + 5),
    F5(698.46f, OCTAVE_5 + 6),
    Fs5(739.99f, OCTAVE_5 + 7),
    Gb5(739.99f, OCTAVE_5 + 7),
    G5(783.99f, OCTAVE_5 + 8),
    Gs5(830.61f, OCTAVE_5 + 9),
    Ab5(830.61f, OCTAVE_5 + 9),
    A5(880.00f, OCTAVE_5 + 10),
    As5(932.33f, OCTAVE_5 + 11),
    Bb5(932.33f, OCTAVE_5 + 11),
    B5(987.77f, OCTAVE_5 + 12),

    C6(1046.50f, OCTAVE_6 + 1),
    Cs6(1108.73f, OCTAVE_6 + 2),
    Db6(1108.73f, OCTAVE_6 + 2),
    D6(1174.66f, OCTAVE_6 + 3),
    Ds6(1244.51f, OCTAVE_6 + 4),
    Eb6(1244.51f, OCTAVE_6 + 4),
    E6(1318.51f, OCTAVE_6 + 5),
    F6(1396.91f, OCTAVE_6 + 6),
    Fs6(1479.98f, OCTAVE_6 + 7),
    Gb6(1479.98f, OCTAVE_6 + 7),
    G6(1567.98f, OCTAVE_6 + 8),
    Gs6(1661.22f, OCTAVE_6 + 9),
    Ab6(1661.22f, OCTAVE_6 + 9),
    A6(1760.00f, OCTAVE_6 + 10),
    As6(1864.66f, OCTAVE_6 + 11),
    Bb6(1864.66f, OCTAVE_6 + 11),
    B6(1975.53f, OCTAVE_6 + 12),

    C7(2093.00f, OCTAVE_7 + 1),
    Cs7(2217.46f, OCTAVE_7 + 2),
    Db7(2217.46f, OCTAVE_7 + 2),
    D7(2349.32f, OCTAVE_7 + 3),
    Ds7(2489.02f, OCTAVE_7 + 4),
    Eb7(2489.02f, OCTAVE_7 + 4),
    E7(2637.02f, OCTAVE_7 + 5),
    F7(2793.83f, OCTAVE_7 + 6),
    Fs7(2959.96f, OCTAVE_7 + 7),
    Gb7(2959.96f, OCTAVE_7 + 7),
    G7(3135.96f, OCTAVE_7 + 8),
    Gs7(3322.44f, OCTAVE_7 + 9),
    Ab7(3322.44f, OCTAVE_7 + 9),
    A7(3520.00f, OCTAVE_7 + 10),
    As7(3729.31f, OCTAVE_7 + 11),
    Bb7(3729.31f, OCTAVE_7 + 11),
    B7(3951.07f, OCTAVE_7 + 12),

    C8(4186.01f, OCTAVE_8 + 1),
    Cs8(4434.92f, OCTAVE_8 + 2),
    Db8(4434.92f, OCTAVE_8 + 2),
    D8(4698.63f, OCTAVE_8 + 3),
    Ds8(4978.03f, OCTAVE_8 + 4),
    Eb8(4978.03f, OCTAVE_8 + 4),
    E8(5274.04f, OCTAVE_8 + 5),
    F8(5587.65f, OCTAVE_8 + 6),
    Fs8(5919.91f, OCTAVE_8 + 7),
    Gb8(5919.91f, OCTAVE_8 + 7),
    G8(6271.93f, OCTAVE_8 + 8),
    Gs8(6644.88f, OCTAVE_8 + 9),
    Ab8(6644.88f, OCTAVE_8 + 9),
    A8(7040.00f, OCTAVE_8 + 10),
    As8(7458.62f, OCTAVE_8 + 11),
    Bb8(7458.62f, OCTAVE_8 + 11),
    B8(7902.13f, OCTAVE_8 + 12),
    ;

    companion object {
        private val notesPerIndex = entries.toTypedArray().distinctBy { it.index }.sortedBy { it.index }.toTypedArray()

        fun fromIndex(noteIndex: Int): Note {
            return notesPerIndex[noteIndex]
        }

        fun fromFrequency(frequency: Float): Note {
            return entries.toTypedArray().minBy { abs(it.frequency - frequency) }
        }

        fun fromFrequency(frequency: Int): Note = fromFrequency(frequency.toFloat())

        fun fromName(name: String): Note {
            return Note.valueOf(name)
        }
    }
}

@TinyLib(
    "notes",
    "List all notes from C0 to B8. " +
        "Please note that bemols are the note with b (ie: Gb2) while sharps are the note with s (ie: As3).",
)
class NotesLib : TwoArgFunction() {
    override fun call(
        arg1: LuaValue,
        arg2: LuaValue,
    ): LuaValue {
        val keys = LuaTable()

        Note.entries.forEach { note ->
            keys[note.name] = valueOf(note.index)
        }

        keys["note"] = note()

        arg2["notes"] = keys
        arg2["package"]["loaded"]["notes"] = keys
        return keys
    }

    @TinyFunction("Get the name of a note regarding the note index (ie: C0 = 0, Cs0 = 1, ...)")
    inner class note : OneArgFunction() {
        @TinyCall("Get the name of a note regarding the note index (ie: C0 = 0, Cs0 = 1, ...)")
        override fun call(
            @TinyArg("note_index") arg: LuaValue,
        ): LuaValue {
            return valueOf(Note.fromIndex(arg.checkint()).name)
        }
    }
}
