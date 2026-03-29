package com.github.minigdx.tiny.lua

import com.github.mingdx.tiny.doc.LuaType
import com.github.mingdx.tiny.doc.TinyArg
import com.github.mingdx.tiny.doc.TinyCall
import com.github.mingdx.tiny.doc.TinyFunction
import com.github.mingdx.tiny.doc.TinyLib
import com.github.minigdx.tiny.log.Logger
import org.luaj.vm2.LuaTable
import org.luaj.vm2.LuaValue
import org.luaj.vm2.Varargs
import org.luaj.vm2.lib.TwoArgFunction
import org.luaj.vm2.lib.VarArgFunction

@TinyLib("console", "Helpers to log information in the console.", icon = "terminal")
class ConsoleLib(private val logger: Logger) : TwoArgFunction() {
    override fun call(
        arg1: LuaValue,
        arg2: LuaValue,
    ): LuaValue {
        val tiny = LuaTable()
        tiny["log"] = log()

        arg2["console"] = tiny
        arg2["package"]["loaded"]["console"] = tiny

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

    @TinyFunction("Log a message into the console.", example = DEBUG_EXAMPLE)
    internal inner class log : VarArgFunction() {
        @TinyCall("Log a message into the console.")
        override fun invoke(
            @TinyArg("str", type = LuaType.ANY) args: Varargs,
        ): Varargs {
            val nbArgs = args.narg()
            val message = (1..nbArgs).joinToString(" ") {
                formatValue(args.arg(it))
            }

            logger.debug("\uD83D\uDC1B") { message }
            return NIL
        }
    }
}
