package com.github.minigdx.tiny.lua

import org.luaj.vm2.LuaTable
import org.luaj.vm2.LuaValue
import org.luaj.vm2.lib.TwoArgFunction

enum class Note(val frequency: Float) {
    C0(16.35f),
    Cs0(17.32f),
    Db0(17.32f),
    D0(18.35f),
    Ds0(19.45f),
    Eb0(19.45f),
    E0(20.60f),
    F0(21.83f),
    Fs0(23.12f),
    Gb0(23.12f),
    G0(24.50f),
    Gs0(25.96f),
    Ab0(25.96f),
    A0(27.50f),
    As0(29.14f),
    Bb0(29.14f),
    B0(30.87f),

    C1(32.70f),
    Cs1(34.65f),
    Db1(34.65f),
    D1(36.71f),
    Ds1(38.89f),
    Eb1(38.89f),
    E1(41.20f),
    F1(43.65f),
    Fs1(46.25f),
    Gb1(46.25f),
    G1(49.00f),
    Gs1(51.91f),
    Ab1(51.91f),
    A1(55.00f),
    As1(58.27f),
    Bb1(58.27f),
    B1(61.74f),

    C2(65.41f),
    Cs2(69.30f),
    Db2(69.30f),
    D2(73.42f),
    Ds2(77.78f),
    Eb2(77.78f),
    E2(82.41f),
    F2(87.31f),
    Fs2(92.50f),
    Gb2(92.50f),
    G2(98.00f),
    Gs2(103.83f),
    Ab2(103.83f),
    A2(110.00f),
    As2(116.54f),
    Bb2(116.54f),
    B2(123.47f),
}

class NotesLib : TwoArgFunction() {

    override fun call(arg1: LuaValue, arg2: LuaValue): LuaValue {
        val keys = LuaTable()

        Note.values().forEach { note ->
            keys[note.name] = valueOf(note.ordinal)
        }

        arg2["notes"] = keys
        arg2["package"]["loaded"]["notes"] = keys
        return keys
    }
}
