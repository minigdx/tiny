package com.github.minigdx.tiny.lua

import com.github.mingdx.tiny.doc.TinyArg
import com.github.mingdx.tiny.doc.TinyArgs
import com.github.mingdx.tiny.doc.TinyCall
import com.github.mingdx.tiny.doc.TinyFunction
import com.github.mingdx.tiny.doc.TinyLib
import com.github.minigdx.tiny.engine.DebugEnabled
import com.github.minigdx.tiny.engine.DebugMessage
import com.github.minigdx.tiny.engine.DebugRect
import com.github.minigdx.tiny.engine.GameResourceAccess
import org.luaj.vm2.LuaTable
import org.luaj.vm2.LuaValue
import org.luaj.vm2.Varargs
import org.luaj.vm2.lib.LibFunction
import org.luaj.vm2.lib.OneArgFunction
import org.luaj.vm2.lib.TwoArgFunction

private class DebugShape {

    fun rectArgs(args: Varargs): List<LuaValue>? {
        when (args.narg()) {
            // rect including color
            in 1..1 -> {
                val table = args.arg1().opttable(null) ?: return null
                return listOf(
                    table["x"],
                    table["y"],
                    table["width"],
                    table["height"],
                    table["color"],
                )
            }
            // rect with color
            in 2..2 -> {
                val table = args.arg1().opttable(null) ?: return null
                return listOf(
                    table["x"],
                    table["y"],
                    table["width"],
                    table["height"],
                    args.arg(2),
                )
            }
            // not supported
            in 3..3 -> {
                return null
            }
            // every args
            else -> {
                val x = args.arg(1)
                val y = args.arg(2)
                val width = args.arg(3)
                val height = args.arg(4)
                val color = args.arg(5)
                return listOf(x, y, width, height, color)
            }
        }
    }
}

@TinyLib("debug", "Helpers to debug your game by drawing or printing information on screen.")
class DebugLib(private val resourceAccess: GameResourceAccess) : TwoArgFunction() {

    private val shape = DebugShape()

    override fun call(arg1: LuaValue, arg2: LuaValue): LuaValue {
        val tiny = LuaTable()
        tiny["enabled"] = enabled()
        tiny["log"] = log()
        tiny["rect"] = rect()
        tiny["table"] = table()

        arg2["debug"] = tiny
        arg2["package"]["loaded"]["debug"] = tiny

        return tiny
    }

    @TinyFunction("Enable or disable debug feature.", example = DEBUG_ENABLED_EXAMPLE)
    internal inner class enabled : OneArgFunction() {

        private var status = false

        @TinyCall("Enable or disable debug by passing true to enable, false to disable.")
        override fun call(@TinyArg("enabled") arg: LuaValue): LuaValue {
            if (arg.isnil()) {
                return valueOf(status)
            }

            val enabled = arg.optboolean(true)
            status = enabled
            resourceAccess.debug(DebugEnabled(enabled))
            return valueOf(status)
        }

        @TinyCall("Return true if debug is enabled. False otherwise.")
        override fun call(): LuaValue = super.call()
    }

    @TinyFunction("Display a table.", example = DEBUG_EXAMPLE)
    internal inner class table : OneArgFunction() {
        @TinyCall("Display a table.")
        override fun call(@TinyArg("table") arg: LuaValue): LuaValue {
            val luaTable = arg.opttable(null) ?: return NIL
            val keys = luaTable.keys()
            val str = keys.joinToString("") { k ->
                val value = luaTable[k]
                val v = if (value.isnumber() || value.isstring()) {
                    value.optjstring("nil")
                } else {
                    "nil"
                }
                "[$k:$v]"
            }
            resourceAccess.debug(DebugMessage(str, "#32CD32"))
            return NIL
        }
    }

    @TinyFunction("Log a message on the screen.", example = DEBUG_EXAMPLE)
    internal inner class log : TwoArgFunction() {

        @TinyCall("Log a message on the screen.")
        override fun call(@TinyArg("str") arg1: LuaValue, @TinyArg("color") arg2: LuaValue): LuaValue {
            val message = arg1.optjstring("")!!
            val color = arg2.optjstring("#32CD32")!!
            resourceAccess.debug(DebugMessage(message, color))
            return NIL
        }

        @TinyCall("Log a message on the screen.")
        override fun call(@TinyArg("str") arg: LuaValue): LuaValue = super.call(arg)
    }

    @TinyFunction("Draw a rectangle on the screen", example = DEBUG_ENABLED_EXAMPLE)
    internal inner class rect : LibFunction() {

        @TinyCall("Draw a debug rectangle.")
        override fun invoke(@TinyArgs(["x", "y", "width", "height", "color"]) args: Varargs): Varargs {
            val (x, y, width, height, color) = shape.rectArgs(args) ?: return NIL
            resourceAccess.debug(
                DebugRect(
                    x = x.checkint(),
                    y = y.checkint(),
                    width = width.checkint(),
                    height = height.checkint(),
                    color = color.optjstring("#32CD32")!!,
                ),
            )
            return NIL
        }

        @TinyCall("Draw a debug rectangle.")
        override fun call(@TinyArg("rect", "A rectangle {x, y, width, height, color}") a: LuaValue): LuaValue {
            return super.call(a)
        }

        @TinyCall("Draw a debug rectangle using a rectangle and a color.")
        override fun call(
            @TinyArg("rect", "A rectangle {x, y, width, height}") a: LuaValue,
            @TinyArg("color") b: LuaValue,
        ): LuaValue = super.call(a, b)
    }
}
