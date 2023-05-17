package com.github.minigdx.tiny.lua

import com.github.mingdx.tiny.doc.TinyArg
import com.github.mingdx.tiny.doc.TinyCall
import com.github.mingdx.tiny.doc.TinyFunction
import com.github.mingdx.tiny.doc.TinyLib
import com.github.minigdx.tiny.engine.GameResourceAccess
import org.luaj.vm2.LuaTable
import org.luaj.vm2.LuaValue
import org.luaj.vm2.lib.OneArgFunction
import org.luaj.vm2.lib.TwoArgFunction

@TinyLib("sfx", "Sound API to play/loop/stop a sound.")
class SfxLib(private val resourceAccess: GameResourceAccess) : TwoArgFunction() {
    override fun call(arg1: LuaValue, arg2: LuaValue): LuaValue {
        val ctrl = LuaTable()
        ctrl.set("play", play())
        ctrl.set("loop", loop())
        ctrl.set("stop", stop())
        arg2.set("sfx", ctrl)
        arg2.get("package").get("loaded").set("sfx", ctrl)
        return ctrl
    }

    @TinyFunction(
        "Play a sound by it's index. " +
            "The index of a sound is given by it's position in the sounds field from the `_tiny.json` file." +
            "The first sound is at the index 0."
    )
    inner class play : OneArgFunction() {

        @TinyCall("Play the sound at the index 0.")
        override fun call(): LuaValue = super.call()

        @TinyCall("Play the sound by it's index.")
        override fun call(@TinyArg("sound") arg: LuaValue): LuaValue {
            val index = if (arg.isnumber()) {
                arg.checkint()
            } else {
                0
            }
            resourceAccess.sound(index)?.play()
            return NIL
        }
    }

    @TinyFunction("Play a sound and loop over it.")
    inner class loop : OneArgFunction() {

        @TinyCall("Play the sound at the index 0.")
        override fun call(): LuaValue = super.call()

        @TinyCall("Play the sound by it's index.")
        override fun call(@TinyArg("sound") arg: LuaValue): LuaValue {
            val index = if (arg.isnumber()) {
                arg.checkint()
            } else {
                0
            }
            resourceAccess.sound(index)?.loop()
            return NIL
        }
    }

    @TinyFunction("Stop a sound.")
    inner class stop : OneArgFunction() {

        @TinyCall("Stop the sound at the index 0.")
        override fun call(): LuaValue = super.call()

        @TinyCall("Stop the sound by it's index.")
        override fun call(@TinyArg("sound") arg: LuaValue): LuaValue {
            val index = if (arg.isnumber()) {
                arg.checkint()
            } else {
                0
            }
            resourceAccess.sound(index)?.stop()
            return NIL
        }
    }
}
