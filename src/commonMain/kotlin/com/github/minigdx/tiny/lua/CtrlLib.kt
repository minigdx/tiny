package com.github.minigdx.tiny.lua

import com.github.minigdx.tiny.input.InputHandler
import com.github.minigdx.tiny.input.Key
import org.luaj.vm2.LuaTable
import org.luaj.vm2.LuaValue
import org.luaj.vm2.lib.LibFunction
import org.luaj.vm2.lib.OneArgFunction
import org.luaj.vm2.lib.TwoArgFunction

class CtrlLib(private val inputHandler: InputHandler) : TwoArgFunction() {
    override fun call(arg1: LuaValue, arg2: LuaValue): LuaValue {
        val ctrl = LuaTable()
        ctrl.set("key", key())
        ctrl.set("down", down())
        ctrl.set("x", todo())
        ctrl.set("y", todo())
        ctrl.set("touched", todo())
        ctrl.set("touching", todo())
        arg2.set("ctrl", ctrl)
        arg2.get("package").get("loaded").set("ctrl", ctrl)
        return ctrl
    }

    private val keys = listOf(Key.ARROW_LEFT, Key.ARROW_UP, Key.ARROW_RIGHT, Key.ARROW_DOWN)

    inner class key : OneArgFunction() {
        override fun call(arg: LuaValue): LuaValue {
            val int = arg.checkint()
            val k = keys.getOrNull(int) ?: return valueOf(false)

            return valueOf(inputHandler.isKeyJustPressed(k))
        }
    }

    inner class down : OneArgFunction() {
        override fun call(arg: LuaValue): LuaValue {
            val int = arg.checkint()
            val k = keys.getOrNull(int) ?: return valueOf(false)

            return valueOf(inputHandler.isKeyPressed(k))
        }
    }

    inner class todo() : LibFunction()
}
