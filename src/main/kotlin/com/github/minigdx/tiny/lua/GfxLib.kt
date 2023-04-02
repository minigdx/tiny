package com.github.minigdx.tiny.lua

import com.github.minigdx.tiny.resources.GameScript
import org.luaj.vm2.LuaTable
import org.luaj.vm2.LuaValue
import org.luaj.vm2.lib.LibFunction
import org.luaj.vm2.lib.TwoArgFunction

class GfxLib(private val parent: GameScript) : TwoArgFunction() {
    override fun call(arg1: LuaValue, arg2: LuaValue): LuaValue {
        val func = LuaTable()
        func.set("clip", clip())
        arg2.set("gfx", func)
        arg2.get("package").get("loaded").set("gfx", func)
        return func
    }

    inner class clip : LibFunction() {
        override fun call(): LuaValue {
            parent.frameBuffer.clipper.reset()
            return NONE
        }

        override fun call(a: LuaValue, b: LuaValue, c: LuaValue, d: LuaValue): LuaValue {
            parent.frameBuffer.clipper.set(a.checkint(), b.checkint(), c.checkint(), d.checkint())
            return NONE
        }
    }
}
