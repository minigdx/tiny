package com.github.minigdx.tiny.lua

import com.github.mingdx.tiny.doc.TinyArg
import com.github.mingdx.tiny.doc.TinyCall
import com.github.mingdx.tiny.doc.TinyFunction
import com.github.mingdx.tiny.doc.TinyLib
import org.luaj.vm2.LuaValue
import org.luaj.vm2.lib.LibFunction
import org.luaj.vm2.lib.ThreeArgFunction
import org.luaj.vm2.lib.TwoArgFunction
import kotlin.math.abs
import kotlin.math.sqrt
import kotlin.random.Random

@TinyLib(
    "math",
    "Math functions. Please note that standard LUA math methods are also available."
)
class MathLib : org.luaj.vm2.lib.MathLib() {

    override fun call(modname: LuaValue, env: LuaValue): LuaValue {
        val math = super.call(modname, env)
        math["rnd"] = rnd()
        math["clamp"] = clamp()
        math["dst"] = dst()
        math["dst2"] = dst2()
        return math
    }

    @TinyFunction("Clamp the value between 2 values.")
    internal inner class clamp : ThreeArgFunction() {

        @TinyCall(
            description = "Clamp the value between a and b.",
        )
        override fun call(arg1: LuaValue, arg2: LuaValue, arg3: LuaValue): LuaValue {
            val max = if (arg1.todouble() > arg2.todouble()) {
                arg1
            } else {
                arg2
            }
            val min = if (max.todouble() < arg3.todouble()) {
                arg2
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
            @TinyArg("y2") d: LuaValue
        ): LuaValue {
            val luaValue = dst2.call(a, b, c, d)
            return valueOf(sqrt(luaValue.todouble()))
        }
    }

    @TinyFunction(
        "Compute the distance between two points not squared. " +
            "Use this method to know if an coordinate is closer than another."
    )
    internal inner class dst2 : LibFunction() {

        @TinyCall(
            description = "Distance not squared between (x1, y1) and (x2, y2).",
        )
        override fun call(
            @TinyArg("x1") a: LuaValue,
            @TinyArg("y1") b: LuaValue,
            @TinyArg("x2") c: LuaValue,
            @TinyArg("y2") d: LuaValue
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
            "Generate a random value between 1 and the argument. " +
                "If a table is passed, it'll return a random element of the table."
        )
        override fun call(arg: LuaValue): LuaValue {
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

        @TinyCall("Generate a random value between the first and the second argument.")
        override fun call(arg1: LuaValue, arg2: LuaValue): LuaValue {
            if (arg2.isnil()) {
                return call(arg1)
            }
            return valueOf(Random.nextInt(arg1.toint(), arg2.toint()))
        }
    }
}
