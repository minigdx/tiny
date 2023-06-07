package com.github.minigdx.tiny.lua

import com.github.mingdx.tiny.doc.TinyArg
import com.github.mingdx.tiny.doc.TinyCall
import com.github.mingdx.tiny.doc.TinyFunction
import com.github.mingdx.tiny.doc.TinyLib
import com.github.mingdx.tiny.doc.TinyVariable
import org.luaj.vm2.LuaValue
import org.luaj.vm2.lib.LibFunction
import org.luaj.vm2.lib.OneArgFunction
import org.luaj.vm2.lib.ThreeArgFunction
import org.luaj.vm2.lib.TwoArgFunction
import kotlin.math.abs
import kotlin.math.sqrt
import kotlin.random.Random

@TinyLib(
    "math",
    "Math functions. Please note that standard Lua math methods are also available.",
)
class MathLib : org.luaj.vm2.lib.MathLib() {

    @TinyVariable("pi", "value of pi (~3.14)")
    // Provided by Luak Math lib.
    @TinyVariable("huge", "positive infinity value.")
    override fun call(modname: LuaValue, env: LuaValue): LuaValue {
        val math = super.call(modname, env)
        math["rnd"] = rnd()
        math["clamp"] = clamp()
        math["dst"] = dst()
        math["dst2"] = dst2()
        math["sign"] = sign()
        return math
    }

    @TinyFunction("Return the sign of the number: -1 if negative. 1 otherwise.")
    internal inner class sign : OneArgFunction() {

        @TinyCall("Return the sign of the number.")
        override fun call(@TinyArg("number") arg: LuaValue): LuaValue {
            return if (arg.todouble() >= 0) {
                ONE
            } else {
                MINUS_ONE
            }
        }
    }

    @TinyFunction("Clamp the value between 2 values.")
    internal inner class clamp : ThreeArgFunction() {

        @TinyCall("Clamp the value between a and b. If a is greater than b, then b will be returned.")
        override fun call(
            @TinyArg("a", "The minimum value.") arg1: LuaValue,
            @TinyArg("value", "The value to be clamped.") arg2: LuaValue,
            @TinyArg("b", "The maximum value.") arg3: LuaValue,
        ): LuaValue {
            val max = if (arg1.todouble() > arg2.todouble()) {
                arg1
            } else {
                arg2
            }
            val min = if (max.todouble() < arg3.todouble()) {
                max
            } else {
                arg3
            }
            return min
        }
    }

    @TinyFunction("Compute the distance between two points.")
    internal inner class dst : LibFunction() {

        private val dst2 = dst2()

        @TinyCall(
            description = "Distance between (x1, y1) and (x2, y2).",
        )
        override fun call(
            @TinyArg("x1") a: LuaValue,
            @TinyArg("y1") b: LuaValue,
            @TinyArg("x2") c: LuaValue,
            @TinyArg("y2") d: LuaValue,
        ): LuaValue {
            val luaValue = dst2.call(a, b, c, d)
            return valueOf(sqrt(luaValue.todouble()))
        }
    }

    @TinyFunction(
        "Compute the distance between two points not squared. " +
            "Use this method to know if an coordinate is closer than another.",
    )
    internal inner class dst2 : LibFunction() {

        @TinyCall(
            description = "Distance not squared between (x1, y1) and (x2, y2).",
        )
        override fun call(
            @TinyArg("x1") a: LuaValue,
            @TinyArg("y1") b: LuaValue,
            @TinyArg("x2") c: LuaValue,
            @TinyArg("y2") d: LuaValue,
        ): LuaValue {
            val xDiff = c.todouble() - a.todouble()
            val yDiff = d.todouble() - b.todouble()
            return valueOf(xDiff * xDiff + yDiff * yDiff)
        }
    }

    @TinyFunction("Generate random values")
    internal inner class rnd : TwoArgFunction() {
        @TinyCall("Generate a random int (negative or positive value)")
        override fun call(): LuaValue {
            return valueOf(Random.nextInt())
        }

        @TinyCall(
            "Generate a random value between 1 until the argument. " +
                "If a table is passed, it'll return a random element of the table.",
        )
        override fun call(@TinyArg("until") arg: LuaValue): LuaValue {
            if (arg.isnil()) return call()
            return if (arg.istable()) {
                val table = arg.checktable()!!
                val index = Random.nextInt(1, table.length() + 1)
                table[index]
            } else {
                if (arg.isint()) {
                    valueOf(Random.nextInt(abs(arg.toint())))
                } else {
                    valueOf(Random.nextDouble(abs(arg.todouble())))
                }
            }
        }

        @TinyCall("Generate a random value between a and b.")
        override fun call(@TinyArg("a") arg1: LuaValue, @TinyArg("b") arg2: LuaValue): LuaValue {
            if (arg2.isnil()) {
                return call(arg1)
            }
            return valueOf(Random.nextInt(arg1.toint(), arg2.toint()))
        }
    }

    companion object {

        private val MINUS_ONE = valueOf(-1)
    }
}
