package com.github.minigdx.tiny.lua

import com.github.mingdx.tiny.doc.TinyLib
import com.github.mingdx.tiny.doc.TinyVariable
import com.github.minigdx.tiny.input.Key
import org.luaj.vm2.LuaInteger
import org.luaj.vm2.LuaTable
import org.luaj.vm2.LuaValue
import org.luaj.vm2.lib.TwoArgFunction

@TinyLib(
    "keys",
    "List of the available keys. To be used with ctrl.\n\n" +
        "- `keys.up`, `keys.down`, `keys.left`, `keys.right` for directions.\n" +
        "- `keys.a` to `keys.z` and `keys.0` to `keys.9` for letters and numbers.\n" +
        "- `keys.space` and `keys.enter` for other keys.\n",
)
class KeysLib : TwoArgFunction() {
    @TinyVariable("a", "the key a", hideInDocumentation = true)
    @TinyVariable("b", "the key b", hideInDocumentation = true)
    @TinyVariable("c", "the key c", hideInDocumentation = true)
    @TinyVariable("d", "the key d", hideInDocumentation = true)
    @TinyVariable("e", "the key e", hideInDocumentation = true)
    @TinyVariable("f", "the key f", hideInDocumentation = true)
    @TinyVariable("g", "the key g", hideInDocumentation = true)
    @TinyVariable("h", "the key h", hideInDocumentation = true)
    @TinyVariable("i", "the key i", hideInDocumentation = true)
    @TinyVariable("j", "the key j", hideInDocumentation = true)
    @TinyVariable("k", "the key k", hideInDocumentation = true)
    @TinyVariable("l", "the key l", hideInDocumentation = true)
    @TinyVariable("m", "the key m", hideInDocumentation = true)
    @TinyVariable("n", "the key n", hideInDocumentation = true)
    @TinyVariable("o", "the key o", hideInDocumentation = true)
    @TinyVariable("p", "the key p", hideInDocumentation = true)
    @TinyVariable("q", "the key q", hideInDocumentation = true)
    @TinyVariable("r", "the key r", hideInDocumentation = true)
    @TinyVariable("s", "the key s", hideInDocumentation = true)
    @TinyVariable("t", "the key t", hideInDocumentation = true)
    @TinyVariable("u", "the key u", hideInDocumentation = true)
    @TinyVariable("v", "the key v", hideInDocumentation = true)
    @TinyVariable("w", "the key w", hideInDocumentation = true)
    @TinyVariable("x", "the key x", hideInDocumentation = true)
    @TinyVariable("y", "the key y", hideInDocumentation = true)
    @TinyVariable("z", "the key z", hideInDocumentation = true)
    @TinyVariable("space", "the key space", hideInDocumentation = true)
    @TinyVariable("up", "the key arrow up", hideInDocumentation = true)
    @TinyVariable("down", "the key arrow down", hideInDocumentation = true)
    @TinyVariable("left", "the key left down", hideInDocumentation = true)
    @TinyVariable("right", "the key right down", hideInDocumentation = true)
    @TinyVariable("enter", "the key enter down", hideInDocumentation = true)
    @TinyVariable("shift", "the key shift down", hideInDocumentation = true)
    @TinyVariable("ctrl", "the key ctrl down", hideInDocumentation = true)
    @TinyVariable("alt", "the key alt down", hideInDocumentation = true)
    override fun call(
        arg1: LuaValue,
        arg2: LuaValue,
    ): LuaValue {
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
        keys["up"] = LuaInteger.valueOf(Key.ARROW_UP.ordinal)
        keys["down"] = LuaInteger.valueOf(Key.ARROW_DOWN.ordinal)
        keys["right"] = LuaInteger.valueOf(Key.ARROW_RIGHT.ordinal)
        keys["left"] = LuaInteger.valueOf(Key.ARROW_LEFT.ordinal)

        keys["space"] = LuaInteger.valueOf(Key.SPACE.ordinal)
        keys["enter"] = LuaInteger.valueOf(Key.ENTER.ordinal)
        keys["shift"] = LuaInteger.valueOf(Key.SHIFT.ordinal)
        keys["ctrl"] = LuaInteger.valueOf(Key.CTRL.ordinal)
        keys["alt"] = LuaInteger.valueOf(Key.ALT.ordinal)

        arg2["keys"] = keys
        arg2["package"]["loaded"]["keys"] = keys
        return keys
    }
}
