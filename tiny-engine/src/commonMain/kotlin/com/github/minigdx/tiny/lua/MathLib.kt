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
import kotlin.math.floor
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
        math["roverlap"] = roverlap()
        math["perlin"] = perlin(Random.nextLong())
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
                if (table.length() > 0) {
                    val index = Random.nextInt(1, table.length() + 1)
                    table[index]
                } else {
                    NIL
                }
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

    @TinyFunction("Check if two (r)ectangles overlaps.")
    inner class roverlap() : TwoArgFunction() {
        @TinyCall("Check if the rectangle rect1 overlaps with the rectangle rect2.")
        override fun call(
            @TinyArg("rect1", "Rectangle as a table {x, y, with, height}.") arg1: LuaValue,
            @TinyArg("rect2", "Rectangle as a table {x, y, with, height}.") arg2: LuaValue,
        ): LuaValue {
            val ax = arg1["x"].toint()
            val ay = arg1["y"].toint()
            val awidth = arg1["width"].toint()
            val aheight = arg1["height"].toint()

            val bx = arg2["x"].toint()
            val by = arg2["y"].toint()
            val bwidth = arg2["width"].toint()
            val bheight = arg2["height"].toint()

            return valueOf(ax < bx + bwidth && ax + awidth > bx && ay < by + bheight && ay + aheight > by)
        }
    }

    @TinyFunction("Perlin noise. The random generated value is between 0 and 1.")
    inner class perlin(seed: Long) : ThreeArgFunction() {

        private val permutation: MutableList<Int>

        init {
            val source = (0..255).toList().shuffled(Random(seed))
            permutation = mutableListOf<Int>().apply {
                repeat(512) {
                    add(source[it and 255])
                }
            }
        }

        fun noise(x: Double, y: Double, z: Double): Double {
            val xi = floor(x).toInt() and 255
            val yi = floor(y).toInt() and 255
            val zi = floor(z).toInt() and 255

            val xf = x - floor(x)
            val yf = y - floor(y)
            val zf = z - floor(z)

            val u = fade(xf)
            val v = fade(yf)
            val w = fade(zf)

            val aaa = permutation[permutation[permutation[xi] + yi] + zi]
            val aba = permutation[permutation[permutation[xi] + inc(yi)] + zi]
            val aab = permutation[permutation[permutation[xi] + yi] + inc(zi)]
            val abb = permutation[permutation[permutation[xi] + inc(yi)] + inc(zi)]
            val baa = permutation[permutation[permutation[inc(xi)] + yi] + zi]
            val bba = permutation[permutation[permutation[inc(xi)] + inc(yi)] + zi]
            val bab = permutation[permutation[permutation[inc(xi)] + yi] + inc(zi)]
            val bbb = permutation[permutation[permutation[inc(xi)] + inc(yi)] + inc(zi)]

            val x1 = lerp(grad(aaa, xf, yf, zf), grad(baa, xf - 1, yf, zf), u)
            val x2 = lerp(grad(aba, xf, yf - 1, zf), grad(bba, xf - 1, yf - 1, zf), u)
            val y1 = lerp(x1, x2, v)

            val x3 = lerp(grad(aab, xf, yf, zf - 1), grad(bab, xf - 1, yf, zf - 1), u)
            val x4 = lerp(grad(abb, xf, yf - 1, zf - 1), grad(bbb, xf - 1, yf - 1, zf - 1), u)
            val y2 = lerp(x3, x4, v)

            return (lerp(y1, y2, w) + 1) / 2
        }

        private fun fade(t: Double): Double {
            return t * t * t * (t * (t * 6 - 15) + 10)
        }

        private fun inc(num: Int): Int {
            return (num + 1) and 255
        }

        private fun grad(hash: Int, x: Double, y: Double, z: Double): Double {
            val h = hash and 15
            val u = if (h < 8) x else y
            val v = if (h < 4) y else if (h == 12 || h == 14) x else z
            return (if (h and 1 == 0) u else -u) + (if (h and 2 == 0) v else -v)
        }

        private fun lerp(a: Double, b: Double, t: Double): Double {
            return a + t * (b - a)
        }

        @TinyCall("Generate a random value regarding the parameters x,y and z.")
        override fun call(
            @TinyArg("x") arg1: LuaValue,
            @TinyArg("y") arg2: LuaValue,
            @TinyArg("z") arg3: LuaValue,
        ): LuaValue {
            return valueOf(noise(arg1.todouble(), arg2.todouble(), arg3.todouble()))
        }
    }

    companion object {

        private val MINUS_ONE = valueOf(-1)
    }
}
