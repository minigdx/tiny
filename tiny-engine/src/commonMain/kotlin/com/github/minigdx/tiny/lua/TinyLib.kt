package com.github.minigdx.tiny.lua

import com.github.mingdx.tiny.doc.TinyArg
import com.github.mingdx.tiny.doc.TinyCall
import com.github.mingdx.tiny.doc.TinyFunction
import com.github.mingdx.tiny.doc.TinyLib
import com.github.mingdx.tiny.doc.TinyVariable
import org.luaj.vm2.LuaTable
import org.luaj.vm2.LuaValue
import org.luaj.vm2.lib.OneArgFunction
import org.luaj.vm2.lib.TwoArgFunction

@TinyLib(
    "tiny",
    "Tiny Lib which offer offer the current frame (`tiny.frame`), " +
        "the current time (`tiny.time`), delta time (`tiny.dt`) and " +
        "to switch to another script using `exit`."
)
class TinyLib(private val listener: StdLibListener) : TwoArgFunction() {

    private var time: Double = 0.0
    private var frame: Int = 0
    private val tiny = LuaTable()
    fun advance() {
        frame++
        time += 1 / 60.0

        tiny.set("t", valueOf(time))
        tiny.set("frame", valueOf(frame))
    }

    @TinyVariable(
        "dt",
        "Delta time between two frame. " +
            "As Tiny is a fixed frame engine, it's always equal to 1/60"
    )
    @TinyVariable("t", "Time elapsed since the start of the game.")
    @TinyVariable("frame", "Number of frames elapsed since the start of the game.")
    override fun call(arg1: LuaValue, arg2: LuaValue): LuaValue {
        tiny["dt"] = valueOf(1 / 60.0)
        tiny["t"] = valueOf(time)
        tiny["frame"] = valueOf(frame)
        tiny["exit"] = exit()
        arg2["tiny"] = tiny
        arg2["package"]["loaded"]["tiny"] = tiny
        return tiny
    }

    @TinyFunction(
        "Exit the actual script to switch to another one. " +
            "The next script to use is identified by it's index. " +
            "The index of the script is the index of it in the list of scripts from the `_tiny.json` file." +
            "The first script is at the index 0."
    )
    internal inner class exit : OneArgFunction() {

        @TinyCall("Exit the actual script to switch to another one.")
        override fun call(@TinyArg("scriptIndex")arg: LuaValue): LuaValue {
            listener.exit(arg.checkint())
            return NONE
        }
    }
}
