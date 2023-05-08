package com.github.minigdx.tiny.lua

import com.github.mingdx.tiny.doc.TinyLib
import com.github.minigdx.tiny.input.Key
import org.luaj.vm2.LuaInteger
import org.luaj.vm2.LuaTable
import org.luaj.vm2.LuaValue
import org.luaj.vm2.lib.TwoArgFunction

@TinyLib(
    "keys",
    "List of the available keys. To be used with ctrl."
)
class KeysLib : TwoArgFunction() {
    override fun call(arg1: LuaValue, arg2: LuaValue): LuaValue {
        val keys = LuaTable()
        // chars
        ('a'..'z').forEach { char ->
            val index = char - 'a'
            keys[char.toString()] = LuaInteger.valueOf(Key.A.ordinal + index)
        }

        // numbers
        ('0'..'9').forEach { char ->
            val index = char - '0'
            keys[char.toString()] = LuaInteger.valueOf(Key.NUM0.ordinal + index)
        }

        // arrows
        keys["⬆\uFE0F"] = LuaInteger.valueOf(Key.ARROW_UP.ordinal)
        keys["⬆"] = LuaInteger.valueOf(Key.ARROW_UP.ordinal)
        keys["up"] = LuaInteger.valueOf(Key.ARROW_UP.ordinal)
        keys["⬇\uFE0F"] = LuaInteger.valueOf(Key.ARROW_DOWN.ordinal)
        keys["⬇"] = LuaInteger.valueOf(Key.ARROW_DOWN.ordinal)
        keys["down"] = LuaInteger.valueOf(Key.ARROW_DOWN.ordinal)
        keys["➡\uFE0F"] = LuaInteger.valueOf(Key.ARROW_RIGHT.ordinal)
        keys["➡"] = LuaInteger.valueOf(Key.ARROW_RIGHT.ordinal)
        keys["right"] = LuaInteger.valueOf(Key.ARROW_RIGHT.ordinal)
        keys["⬅\uFE0F"] = LuaInteger.valueOf(Key.ARROW_LEFT.ordinal)
        keys["⬅"] = LuaInteger.valueOf(Key.ARROW_LEFT.ordinal)
        keys["left"] = LuaInteger.valueOf(Key.ARROW_LEFT.ordinal)

        keys["space"] = LuaInteger.valueOf(Key.SPACE.ordinal)
        keys["enter"] = LuaInteger.valueOf(Key.ENTER.ordinal)

        arg2.set("keys", keys)
        arg2.get("package").get("loaded").set("keys", keys)
        return keys
    }
}
