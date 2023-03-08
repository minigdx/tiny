package com.github.minigdx.tiny.lua

import com.github.minigdx.tiny.engine.GameScript
import org.luaj.vm2.LuaTable
import org.luaj.vm2.LuaValue
import org.luaj.vm2.Varargs
import org.luaj.vm2.lib.LibFunction
import org.luaj.vm2.lib.OneArgFunction
import org.luaj.vm2.lib.ThreeArgFunction
import org.luaj.vm2.lib.TwoArgFunction
import org.luaj.vm2.lib.VarArgFunction
import org.luaj.vm2.lib.ZeroArgFunction
import kotlin.random.Random

class TinyLib(val parent: GameScript) : TwoArgFunction() {
    override fun call(modname: LuaValue, env: LuaValue): LuaValue {
        val tiny = LuaTable()
        env["exit"] = exit()
        env["line"] = line()
        env["pset"] = pset()
        env["pget"] = pget()
        env["rnd"] = rnd()
        env["cls"] = cls()
        env["circle"] = circle()
        env["circlef"] = circlef()
        return tiny
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

    internal inner class rnd : TwoArgFunction() {
        override fun call(): LuaValue {
            return LuaValue.valueOf(Random.nextInt())
        }

        override fun call(arg: LuaValue): LuaValue {
            return LuaValue.valueOf(Random.nextInt(arg.checkint()))
        }

        override fun call(arg1: LuaValue, arg2: LuaValue): LuaValue {
            if (arg2.isnil()) {
                return call(arg1)
            }
            return LuaValue.valueOf(Random.nextInt(arg1.checkint(), arg2.checkint()))
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

    internal inner class line : VarArgFunction() {

        override fun call(): LuaValue {
            // no op
            return NONE
        }

        override fun call(arg: LuaValue): LuaValue {
            // line(x1)
            // parent.frameBuffer.pixel(x, y, color)
            return super.call(arg)
        }

        override fun call(arg1: LuaValue, arg2: LuaValue): LuaValue {
            // line(x1, x2)
            return super.call(arg1, arg2)
        }

        override fun call(arg1: LuaValue, arg2: LuaValue, arg3: LuaValue): LuaValue {
            // line(x1, y1, x2)
            return super.call(arg1, arg2, arg3)
        }

        override fun invoke(args: Varargs): Varargs {
            // line(x1, y1, x2, y2)
            // line(x1, y1, x2, y2, colorIndex)
            return super.invoke(args)
        }
    }

}
