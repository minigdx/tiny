package com.github.minigdx.tiny.lua

import com.github.minigdx.tiny.engine.GameResourceAccess
import com.github.minigdx.tiny.resources.GameScript
import kotlinx.coroutines.NonDisposableHandle.parent
import org.luaj.vm2.LuaTable
import org.luaj.vm2.LuaValue
import org.luaj.vm2.lib.LibFunction
import org.luaj.vm2.lib.TwoArgFunction

class GfxLib(private val resourceAccess: GameResourceAccess) : TwoArgFunction() {
    override fun call(arg1: LuaValue, arg2: LuaValue): LuaValue {
        val func = LuaTable()
        func.set("clip", clip())
        func.set("dither", dither())
        func.set("pal", pal())
        arg2.set("gfx", func)
        arg2.get("package").get("loaded").set("gfx", func)
        return func
    }

    inner class pal : LibFunction() {
        override fun call(): LuaValue {
            resourceAccess.frameBuffer.blender.pal()
            return NONE
        }

        override fun call(a: LuaValue, b: LuaValue): LuaValue {
            resourceAccess.frameBuffer.blender.pal(a.checkint(), b.checkint())
            return NONE
        }
    }

    inner class dither : LibFunction() {
        override fun call(): LuaValue {
            resourceAccess.frameBuffer.blender.dither(0)
            return NONE
        }

        override fun call(a: LuaValue): LuaValue {
            resourceAccess.frameBuffer.blender.dither(a.checkint())
            return NONE
        }
    }

    inner class clip : LibFunction() {
        override fun call(): LuaValue {
            resourceAccess.frameBuffer.clipper.reset()
            return NONE
        }

        override fun call(a: LuaValue, b: LuaValue, c: LuaValue, d: LuaValue): LuaValue {
            resourceAccess.frameBuffer.clipper.set(a.checkint(), b.checkint(), c.checkint(), d.checkint())
            return NONE
        }
    }
}
