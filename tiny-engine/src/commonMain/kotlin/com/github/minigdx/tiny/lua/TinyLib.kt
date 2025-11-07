package com.github.minigdx.tiny.lua

import com.github.mingdx.tiny.doc.LuaType
import com.github.mingdx.tiny.doc.TinyArg
import com.github.mingdx.tiny.doc.TinyCall
import com.github.mingdx.tiny.doc.TinyFunction
import com.github.mingdx.tiny.doc.TinyLib
import com.github.mingdx.tiny.doc.TinyVariable
import com.github.minigdx.tiny.engine.Exit
import com.github.minigdx.tiny.engine.GameOptions
import org.luaj.vm2.LuaTable
import org.luaj.vm2.LuaValue
import org.luaj.vm2.lib.OneArgFunction
import org.luaj.vm2.lib.TwoArgFunction

internal expect fun platformValue(): Int

@TinyLib(
    "tiny",
    "Tiny Lib which offer offer the current frame (`tiny.frame`), " +
        "the current time (`tiny.time`), delta time (`tiny.dt`), " +
        "game dimensions (`tiny.width`, `tiny.height`), " +
        "platform information (`tiny.platform`) and " +
        "to switch to another script using `exit`.",
)
class TinyLib(
    private val gameScript: List<String>,
    private val gameOptions: GameOptions,
) : TwoArgFunction() {
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
            "As Tiny is a fixed frame engine, it's always equal to 1/60",
    )
    @TinyVariable("t", "Time elapsed since the start of the game.")
    @TinyVariable("frame", "Number of frames elapsed since the start of the game.")
    @TinyVariable("width", "Width of the game in pixels.")
    @TinyVariable("height", "Height of the game in pixels.")
    @TinyVariable("platform", "Current platform: 1 for desktop, 2 for web.")
    override fun call(
        arg1: LuaValue,
        arg2: LuaValue,
    ): LuaValue {
        val platformType = platformValue()

        tiny["dt"] = valueOf(1 / 60.0)
        tiny["t"] = valueOf(time)
        tiny["frame"] = valueOf(frame)
        tiny["width"] = valueOf(gameOptions.width)
        tiny["height"] = valueOf(gameOptions.height)
        tiny["platform"] = valueOf(platformType)
        tiny["exit"] = exit()
        arg2["tiny"] = tiny
        arg2["package"]["loaded"]["tiny"] = tiny
        return tiny
    }

    @TinyFunction(
        "Exit the actual script to switch to another one. " +
            "The next script to use is identified by it's index. " +
            "The index of the script is the index of it in the list of scripts from the `_tiny.json` file." +
            "The first script is at the index 0.",
    )
    internal inner class exit : OneArgFunction() {
        @TinyCall("Exit the actual script to switch to another one.")
        override fun call(
            @TinyArg("scriptIndex", type = LuaType.ANY) arg: LuaValue,
        ): LuaValue {
            if (arg.isint()) {
                throw Exit(arg.toint())
            } else {
                val scriptName = arg.checkjstring()!!
                val index = gameScript.indexOfFirst { it == scriptName }
                throw Exit(index)
            }
        }
    }
}
