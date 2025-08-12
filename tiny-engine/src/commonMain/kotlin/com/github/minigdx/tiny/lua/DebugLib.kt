package com.github.minigdx.tiny.lua

import com.github.mingdx.tiny.doc.TinyArg
import com.github.mingdx.tiny.doc.TinyCall
import com.github.mingdx.tiny.doc.TinyFunction
import com.github.mingdx.tiny.doc.TinyLib
import com.github.minigdx.tiny.engine.DebugMessage
import com.github.minigdx.tiny.engine.GameResourceAccess
import com.github.minigdx.tiny.log.Logger
import org.luaj.vm2.LuaTable
import org.luaj.vm2.LuaValue
import org.luaj.vm2.Varargs
import org.luaj.vm2.lib.TwoArgFunction
import org.luaj.vm2.lib.VarArgFunction

@TinyLib("debug", "Helpers to debug your game by drawing or printing information on screen.")
class DebugLib(private val resourceAccess: GameResourceAccess, private val logger: Logger) : TwoArgFunction() {
    override fun call(
        arg1: LuaValue,
        arg2: LuaValue,
    ): LuaValue {
        val tiny = LuaTable()
        tiny["log"] = log()
        tiny["console"] = console()

        arg2["debug"] = tiny
        arg2["package"]["loaded"]["debug"] = tiny

        return tiny
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
            val message = (1..nbArgs).joinToString("") {
                formatValue(args.arg(it))
            }

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
}
