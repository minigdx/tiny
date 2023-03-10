package com.github.minigdx.tiny.lua

import com.github.minigdx.tiny.ColorIndex
import com.github.minigdx.tiny.Pixel
import com.github.minigdx.tiny.engine.GameScript
import org.luaj.vm2.LuaError
import org.luaj.vm2.LuaTable
import org.luaj.vm2.LuaValue
import org.luaj.vm2.Varargs
import org.luaj.vm2.lib.LibFunction
import org.luaj.vm2.lib.OneArgFunction
import org.luaj.vm2.lib.ThreeArgFunction
import org.luaj.vm2.lib.TwoArgFunction
import org.luaj.vm2.lib.ZeroArgFunction
import kotlin.math.max
import kotlin.math.min
import kotlin.random.Random

@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.SOURCE)
annotation class DocFunction(
    val name: String,
    val documentation: String = "",
)

@Target(AnnotationTarget.VALUE_PARAMETER)
@Retention(AnnotationRetention.SOURCE)
annotation class DocArg(
    val name: String,
    val documentation: String = "",
)

@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.SOURCE)
annotation class DocCall(
    val documentation: String = "",
    val mainCall: Boolean = false
)

class TinyLib(val parent: GameScript) : TwoArgFunction() {
    override fun call(modname: LuaValue, env: LuaValue): LuaValue {
        val tiny = LuaTable()
        env["exit"] = exit()
        env["line"] = line()
        env["pset"] = pset()
        env["pget"] = pget()
        env["rnd"] = rnd()
        env["cls"] = cls()
        env["min"] = min()
        env["max"] = max()
        env["circle"] = circle()
        env["circlef"] = circlef()
        return tiny
    }

    @DocFunction(
        name = "min",
    )
    internal inner class min : TwoArgFunction() {
        @DocCall(
            documentation = "minimun value between two values. Those values can be numbers or string.",
            mainCall = true
        )
        override fun call(arg1: LuaValue, arg2: LuaValue): LuaValue {
            if (arg1.type() != arg2.type()) {
                throw LuaError("Trying to min two value with different type.")
            }
            return if (arg1.isint() || arg1.islong()) {
                valueOf(min(arg1.checkint(), arg2.checkint()))
            } else if (arg1.isnumber()) {
                valueOf(min(arg1.checkdouble(), arg2.checkdouble()))
            } else {
                valueOf(listOf(arg1.checkjstring() ?: "", arg2.checkjstring() ?: "").sorted().first())
            }
        }
    }

    internal inner class max : TwoArgFunction() {
        override fun call(arg1: LuaValue, arg2: LuaValue): LuaValue {
            if (arg1.type() != arg2.type()) {
                throw LuaError("Trying to min two value with different type.")
            }
            return if (arg1.isint() || arg1.islong()) {
                valueOf(max(arg1.checkint(), arg2.checkint()))
            } else if (arg1.isnumber()) {
                valueOf(max(arg1.checkdouble(), arg2.checkdouble()))
            } else {
                valueOf(listOf(arg1.checkjstring() ?: "", arg2.checkjstring() ?: "").sorted().last())
            }
        }
    }

    internal inner class exit : ZeroArgFunction() {
        override fun call(): LuaValue {
            parent.exited = true
            return NONE
        }
    }

    internal inner class circlef : LibFunction() {
        // centerX: Int, centerY: Int, radius: Int, color: Int
        override fun call(a: LuaValue, b: LuaValue, c: LuaValue, d: LuaValue): LuaValue {
            val centerX = a.checkint()
            val centerY = b.checkint()
            val radius = c.checkint()
            val color = d.checkint()

            var x = 0
            var y = radius
            var dst = 3 - 2 * radius

            while (x <= y) {
                // Draw the outline of the circle
                parent.frameBuffer.pixel(centerX + x, centerY + y, color)
                parent.frameBuffer.pixel(centerX - x, centerY + y, color)
                parent.frameBuffer.pixel(centerX + x, centerY - y, color)
                parent.frameBuffer.pixel(centerX - x, centerY - y, color)
                parent.frameBuffer.pixel(centerX + y, centerY + x, color)
                parent.frameBuffer.pixel(centerX - y, centerY + x, color)
                parent.frameBuffer.pixel(centerX + y, centerY - x, color)
                parent.frameBuffer.pixel(centerX - y, centerY - x, color)

                // Fill the circle
                for (i in centerX - x..centerX + x) {
                    parent.frameBuffer.pixel(i, centerY + y, color)
                    parent.frameBuffer.pixel(i, centerY - y, color)
                }
                for (i in centerX - y..centerX + y) {
                    parent.frameBuffer.pixel(i, centerY + x, color)
                    parent.frameBuffer.pixel(i, centerY - x, color)
                }

                if (dst < 0) {
                    dst += 4 * x + 6
                } else {
                    dst += 4 * (x - y) + 10
                    y--
                }
                x++
            }
            return NONE
        }
    }


    internal inner class line : LibFunction() {

        override fun invoke(args: Varargs): Varargs {
            return when (args.narg()) {
                0 -> call()
                1 -> call(args.arg1())
                2 -> call(args.arg1(), args.arg(2))
                3 -> call(args.arg1(), args.arg(2), args.arg(3))
                4 -> call(args.arg1(), args.arg(2), args.arg(3), args.arg(4))
                else -> draw(
                    args.arg1().checkint(),
                    args.arg(2).checkint(),
                    args.arg(3).checkint(),
                    args.arg(4).checkint(),
                    args.arg(5).checkint()
                )
            }
        }
        private fun draw(x0: Pixel, y0: Pixel, x1: Pixel, y1: Pixel, color: ColorIndex): LuaValue {
            // (x1, y1), (x2, y2)
            val dx = Math.abs(x1 - x0)
            val dy = Math.abs(y1 - y0)
            val sx = if (x0 < x1) 1 else -1
            val sy = if (y0 < y1) 1 else -1
            var err = dx - dy

            var x = x0
            var y = y0

            while (true) {
                parent.frameBuffer.pixel(x, y, color)
                if (x == x1 && y == y1) break
                val e2 = 2 * err
                if (e2 > -dy) {
                    err -= dy
                    x += sx
                }
                if (e2 < dx) {
                    err += dx
                    y += sy
                }
            }
            return NONE
        }

        override fun call(a: LuaValue, b: LuaValue, c: LuaValue, d: LuaValue): LuaValue {
            val x0 = a.checkint()
            val y0 = b.checkint()
            val x1 = c.checkint()
            val y1 = d.checkint()

            return draw(x0, y0, x1, y1, 0)
        }
    }
    internal inner class circle : LibFunction() {
        // centerX: Int, centerY: Int, radius: Int, color: Int
        override fun call(a: LuaValue, b: LuaValue, c: LuaValue, d: LuaValue): LuaValue {
            val centerX = a.checkint()
            val centerY = b.checkint()
            val radius = c.checkint()
            val color = d.checkint()

            var x = 0
            var y = radius
            var dst = 3 - 2 * radius

            while (x <= y) {
                parent.frameBuffer.pixel(centerX + x, centerY + y, color)
                parent.frameBuffer.pixel(centerX - x, centerY + y, color)
                parent.frameBuffer.pixel(centerX + x, centerY - y, color)
                parent.frameBuffer.pixel(centerX - x, centerY - y, color)
                parent.frameBuffer.pixel(centerX + y, centerY + x, color)
                parent.frameBuffer.pixel(centerX - y, centerY + x, color)
                parent.frameBuffer.pixel(centerX + y, centerY - x, color)
                parent.frameBuffer.pixel(centerX - y, centerY - x, color)

                if (dst < 0) {
                    dst += 4 * x + 6
                } else {
                    dst += 4 * (x - y) + 10
                    y--
                }
                x++
            }
            return NONE
        }
    }

    internal inner class cls : OneArgFunction() {
        override fun call(): LuaValue {
            parent.frameBuffer.clear(0)
            return NONE
        }

        override fun call(arg: LuaValue): LuaValue {
            parent.frameBuffer.clear(arg.checkint())
            return NONE
        }
    }

    @DocFunction("rnd", "generate random values")
    internal inner class rnd : TwoArgFunction() {
        @DocCall("Generate a random int (negative or positive value)", mainCall = true)
        override fun call(): LuaValue {
            return valueOf(Random.nextInt())
        }

        @DocCall("Generate a random value between 1 and the argument. " +
            "If a table is passed, it'll return a random element of the table.")
        override fun call(arg: LuaValue): LuaValue {
            return if(arg.istable()) {
                val table = arg.checktable()!!
                val index = Random.nextInt(1, table.arrayLength + 1)
                table[index]
            } else {

                valueOf(Random.nextInt(arg.checkint()))
            }
        }

        @DocCall("Generate a random value between the first and the second argument.")
        override fun call(arg1: LuaValue, arg2: LuaValue): LuaValue {
            if (arg2.isnil()) {
                return call(arg1)
            }
            return valueOf(Random.nextInt(arg1.checkint(), arg2.checkint()))
        }
    }

    internal inner class pset : ThreeArgFunction() {
        // x, y, index
        override fun call(arg1: LuaValue, arg2: LuaValue, arg3: LuaValue): LuaValue {
            if (arg1.isint() && arg2.isint() && arg3.isint()) {
                parent.frameBuffer.pixel(arg1.checkint(), arg2.checkint(), arg3.checkint())
            }
            return NONE
        }

    }

    internal inner class pget : TwoArgFunction() {
        override fun call(arg1: LuaValue, arg2: LuaValue): LuaValue {
            val index = parent.frameBuffer.pixel(arg1.checkint(), arg2.checkint())
            return valueOf(index)
        }
    }
}
