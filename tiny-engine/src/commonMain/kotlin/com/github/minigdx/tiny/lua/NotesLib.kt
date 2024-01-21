package com.github.minigdx.tiny.lua

import org.luaj.vm2.LuaTable
import org.luaj.vm2.LuaValue
import org.luaj.vm2.lib.TwoArgFunction

enum class Note(val frequency: Float) {
    C1(65.4064f),
    D1(73.4162f),
    E1(82.4069f),
}

class NotesLib : TwoArgFunction() {

    override fun call(arg1: LuaValue, arg2: LuaValue): LuaValue {
        val keys = LuaTable()

        Note.values().forEach { note ->
            keys[note.name.lowercase()] = valueOf(note.ordinal)
        }

        arg2["notes"] = keys
        arg2["package"]["loaded"]["notes"] = keys
        return keys
    }
}
