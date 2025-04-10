package com.github.minigdx.tiny.lua

import com.github.mingdx.tiny.doc.TinyArg
import com.github.mingdx.tiny.doc.TinyArgs
import com.github.mingdx.tiny.doc.TinyCall
import com.github.mingdx.tiny.doc.TinyFunction
import com.github.mingdx.tiny.doc.TinyLib
import com.github.minigdx.tiny.engine.DebugEnabled
import com.github.minigdx.tiny.engine.DebugLine
import com.github.minigdx.tiny.engine.DebugMessage
import com.github.minigdx.tiny.engine.DebugPoint
import com.github.minigdx.tiny.engine.DebugRect
import com.github.minigdx.tiny.engine.GameResourceAccess
import com.github.minigdx.tiny.log.Logger
import org.luaj.vm2.LuaTable
import org.luaj.vm2.LuaValue
import org.luaj.vm2.Varargs
import org.luaj.vm2.lib.LibFunction
import org.luaj.vm2.lib.OneArgFunction
import org.luaj.vm2.lib.TwoArgFunction
import org.luaj.vm2.lib.VarArgFunction

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

    fun lineArgs(args: Varargs): List<LuaValue>? {
        return when (args.narg()) {
            // destructured args + color
            5 -> {
                val x1 = args.arg(1)
                val y1 = args.arg(2)
                val x2 = args.arg(3)
                val y2 = args.arg(4)
                val color = args.arg(5)
                return listOf(x1, y1, x2, y2, color)
            }

            // destructured args without color
            4 -> {
                val x1 = args.arg(1)
                val y1 = args.arg(2)
                val x2 = args.arg(3)
                val y2 = args.arg(4)
                return listOf(x1, y1, x2, y2, LuaValue.NIL)
            }

            // structured args
            2, 3 -> {
                val a = args.arg(1)
                val b = args.arg(2)
                val color = args.arg(3)
                return listOf(a.get("x"), a.get("y"), b.get("x"), b.get("y"), color)
            }

            else -> {
                null
            }
        }
    }

    fun pointArgs(args: Varargs): List<LuaValue>? {
        return when (args.narg()) {
            // structured args
            3 -> {
                val a = args.arg(1)
                val b = args.arg(2)
                val color = args.arg(3)
                return listOf(a, b, color)
            }

            2 -> {
                val a = args.arg(1)
                val b = args.arg(2)
                if (a.istable()) {
                    listOf(a.get("x"), a.get("y"), b)
                } else {
                    listOf(a, b, LuaValue.NIL)
                }
            }

            1 -> {
                val a = args.arg(1)
                return listOf(a.get("x"), a.get("y"), LuaValue.NIL)
            }

            else -> {
                null
            }
        }
    }
}

@TinyLib("debug", "Helpers to debug your game by drawing or printing information on screen.")
class DebugLib(private val resourceAccess: GameResourceAccess, private val logger: Logger) : TwoArgFunction() {
    private val shape = DebugShape()

    override fun call(
        arg1: LuaValue,
        arg2: LuaValue,
    ): LuaValue {
        val tiny = LuaTable()
        tiny["enabled"] = enabled()
        tiny["log"] = log()
        tiny["console"] = console()
        tiny["rect"] = rect()
        tiny["point"] = point()
        tiny["line"] = line()
        tiny["table"] = table()

        arg2["debug"] = tiny
        arg2["package"]["loaded"]["debug"] = tiny

        return tiny
    }

    @TinyFunction("Enable or disable debug feature.", example = DEBUG_ENABLED_EXAMPLE)
    internal inner class enabled : OneArgFunction() {
        private var status = false

        @TinyCall("Enable or disable debug by passing true to enable, false to disable.")
        override fun call(
            @TinyArg("enabled") arg: LuaValue,
        ): LuaValue {
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
        override fun call(
            @TinyArg("table") arg: LuaValue,
        ): LuaValue {
            val luaTable = arg.opttable(null) ?: return NIL
            val keys = luaTable.keys()
            val str =
                keys.joinToString("") { k ->
                    val value = luaTable[k]
                    val v =
                        if (value.isnumber() || value.isstring()) {
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

    private fun formatValue(
        arg: LuaValue,
        recursiveSecurity: MutableSet<Int> = mutableSetOf(),
    ): String =
        if (arg.istable()) {
            val table = arg as LuaTable
            if (recursiveSecurity.contains(table.hashCode())) {
                "table[<${table.hashCode()}>]"
            } else {
                recursiveSecurity.add(table.hashCode())
                val keys = table.keys()
                val str =
                    keys.joinToString(" ") {
                        it.optjstring("nil") + ":" + formatValue(table[it], recursiveSecurity)
                    }
                "table[$str]"
            }
        } else if (arg.isfunction()) {
            "function(" + (0 until arg.narg()).joinToString(", ") { "arg" } + ")"
        } else {
            arg.toString()
        }

    @TinyFunction("Log a message on the screen.", example = DEBUG_EXAMPLE)
    internal inner class log : VarArgFunction() {
        @TinyCall("Log a message on the screen.")
        override fun invoke(
            @TinyArg("str") args: Varargs,
        ): Varargs {
            val nbArgs = args.narg()
            val message =
                (1..nbArgs).map {
                    formatValue(args.arg(it))
                }.joinToString("")

            resourceAccess.debug(DebugMessage(message, "#32CD32"))
            return NIL
        }
    }

    @TinyFunction("Log a message into the console.", example = DEBUG_EXAMPLE)
    internal inner class console : VarArgFunction() {
        @TinyCall("Log a message into the console.")
        override fun invoke(
            @TinyArg("str") args: Varargs,
        ): Varargs {
            val nbArgs = args.narg()
            val message =
                (1..nbArgs).map {
                    formatValue(args.arg(it))
                }.joinToString(" ")

            logger.debug("\uD83D\uDC1B") { message }
            return NIL
        }
    }

    @TinyFunction("Draw a rectangle on the screen", example = DEBUG_ENABLED_EXAMPLE)
    internal inner class rect : LibFunction() {
        @TinyCall("Draw a debug rectangle.")
        override fun invoke(
            @TinyArgs(["x", "y", "width", "height", "color"]) args: Varargs,
        ): Varargs {
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
        override fun call(
            @TinyArg("rect", "A rectangle {x, y, width, height, color}") a: LuaValue,
        ): LuaValue {
            return super.call(a)
        }

        @TinyCall("Draw a debug rectangle using a rectangle and a color.")
        override fun call(
            @TinyArg("rect", "A rectangle {x, y, width, height}") a: LuaValue,
            @TinyArg("color") b: LuaValue,
        ): LuaValue = super.call(a, b)
    }

    @TinyFunction("Draw a point on the screen")
    internal inner class point : LibFunction() {
        @TinyCall("Draw a debug point.")
        override fun invoke(
            @TinyArgs(["x", "y", "color"]) args: Varargs,
        ): Varargs {
            val (x, y, color) = shape.pointArgs(args) ?: return NIL
            resourceAccess.debug(
                DebugPoint(
                    x = x.checkint(),
                    y = y.checkint(),
                    color = color.optjstring("#32CD32")!!,
                ),
            )
            return NIL
        }

        @TinyCall("Draw a debug point.")
        override fun call(
            @TinyArg("point", "A point {x, y, color}") a: LuaValue,
        ): LuaValue {
            return super.call(a)
        }

        @TinyCall("Draw a debug point.")
        override fun call(
            @TinyArg("point", "A point {x, y}") a: LuaValue,
            @TinyArg("color") b: LuaValue,
        ): LuaValue = super.call(a, b)
    }

    @TinyFunction("Draw a point on the screen")
    internal inner class line : LibFunction() {
        @TinyCall("Draw a debug line.")
        override fun invoke(
            @TinyArgs(["x1", "y1", "x2", "y2", "color"]) args: Varargs,
        ): Varargs {
            val (x1, y1, x2, y2, color) = shape.lineArgs(args) ?: return NIL
            resourceAccess.debug(
                DebugLine(
                    x1 = x1.checkint(),
                    y1 = y1.checkint(),
                    x2 = x2.checkint(),
                    y2 = y2.checkint(),
                    color = color.optjstring("#32CD32")!!,
                ),
            )
            return NIL
        }

        @TinyCall("Draw a debug line.")
        override fun call(
            @TinyArg("v1", "A point {x, y}") a: LuaValue,
            @TinyArg("v2", "A point {x, y}") b: LuaValue,
        ): LuaValue = super.call(a, b)

        @TinyCall("Draw a debug line.")
        override fun call(
            @TinyArg("v1", "A point {x, y}") a: LuaValue,
            @TinyArg("v2", "A point {x, y}") b: LuaValue,
            @TinyArg("color") c: LuaValue,
        ): LuaValue = super.call(a, b, c)
    }
}
