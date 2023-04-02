package com.github.minigdx.tiny.lua

import com.github.minigdx.tiny.ColorIndex
import com.github.minigdx.tiny.Pixel
import com.github.minigdx.tiny.graphic.FrameBuffer
import com.github.minigdx.tiny.resources.GameScript
import com.github.minigdx.tiny.resources.ResourceType.BOOT_SPRITESHEET
import com.github.minigdx.tiny.resources.ResourceType.GAME_SPRITESHEET
import org.luaj.vm2.LuaError
import org.luaj.vm2.LuaTable
import org.luaj.vm2.LuaValue
import org.luaj.vm2.Varargs
import org.luaj.vm2.lib.LibFunction
import org.luaj.vm2.lib.OneArgFunction
import org.luaj.vm2.lib.ThreeArgFunction
import org.luaj.vm2.lib.TwoArgFunction
import org.luaj.vm2.lib.ZeroArgFunction
import java.awt.Frame
import kotlin.annotation.AnnotationRetention.SOURCE
import kotlin.annotation.AnnotationTarget.CLASS
import kotlin.annotation.AnnotationTarget.FUNCTION
import kotlin.annotation.AnnotationTarget.VALUE_PARAMETER
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin
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
        env["cos"] = cos()
        env["sin"] = sin()
        env["min"] = min()
        env["max"] = max()
        env["abs"] = abs()
        env["rect"] = rect()
        env["rectf"] = rectf()
        env["circle"] = circle()
        env["circlef"] = circlef()
        env["sspr"] = sspr()
        env["spr"] = spr()
        env["print"] = print()
        return tiny
    }

    @DocFunction(
        name = "sspr",
    )
    internal inner class sspr : LibFunction() {
        // x, y, spr x, spr y, width, height, flip x, flip y
        override fun invoke(args: Varargs): Varargs {
            if (args.narg() < 6) return NONE
            val x = args.arg(1).checkint()
            val y = args.arg(2).checkint()
            val sprX = args.arg(3).checkint()
            val sprY = args.arg(4).checkint()
            val sprWidth = args.arg(5).checkint()
            val sprHeight = args.arg(6).checkint()
            val flipX = args.arg(7).optboolean(false)
            val flipY = args.arg(8).optboolean(false)

            val spritesheet = parent.spriteSheets[GAME_SPRITESHEET] ?: return NONE


            parent.frameBuffer.colorIndexBuffer.copyFrom(
                spritesheet.pixels, x, y, sprX,
                sprY,
                sprWidth,
                sprHeight,
                flipX,
                flipY,
            )

            return NONE
        }
    }

    @DocFunction(
        name = "spr",
    )
    internal inner class spr : LibFunction() {
        // sprn, x, y, flip x, flip y
        override fun call(a: LuaValue, b: LuaValue, c: LuaValue): LuaValue {
            val sprN = a.checkint()
            val x = b.checkint()
            val y = c.checkint()
            val spritesheet = parent.spriteSheets[GAME_SPRITESHEET] ?: return NONE

            val (sw, sh) = parent.gameOption.spriteSize
            val nbSpritePerRow = spritesheet.width / sw

            val column = sprN % nbSpritePerRow
            val row = (sprN - column) / nbSpritePerRow
            parent.frameBuffer.colorIndexBuffer.copyFrom(
                spritesheet.pixels, x, y,
                column * sw,
                row * sh,
                sw, sh
            )

            return NONE
        }

        override fun invoke(args: Varargs): Varargs {
            if (args.narg() < 3) return NONE
            val sprN = args.arg(1).checkint()
            val x = args.arg(2).checkint()
            val y = args.arg(3).checkint()
            val flipX = args.arg(4).optboolean(false)
            val flipY = args.arg(5).optboolean(false)

            val spritesheet = parent.spriteSheets[GAME_SPRITESHEET] ?: return NONE

            val (sw, sh) = parent.gameOption.spriteSize
            val nbSpritePerRow = spritesheet.width / sw

            val column = sprN % nbSpritePerRow
            val row = (sprN - column) / nbSpritePerRow
            parent.frameBuffer.colorIndexBuffer.copyFrom(
                source = spritesheet.pixels,
                dstX = x,
                dstY = y,
                sourceX = column * sw,
                sourceY = row * sh,
                width = sw,
                height = sh,
                reverseX = flipX,
                reverseY = flipY
            )

            return NONE
        }
    }

    @DocFunction(
        name = "print",
    )
    internal inner class print : LibFunction() {

        @DocCall(
            documentation = "print on the screen a string",
            mainCall = true
        )
        override fun call(arg: LuaValue): LuaValue {
            return NONE
        }

        override fun call(a: LuaValue, b: LuaValue, c: LuaValue): LuaValue {
            return NONE
        }

        override fun call(a: LuaValue, b: LuaValue, c: LuaValue, d: LuaValue): LuaValue {
            val str = a.checkjstring() ?: return NONE
            val x = b.checkint()
            val y = c.checkint()
            val color = d.checkColorIndex() ?: 1

            val spritesheet = parent.spriteSheets[BOOT_SPRITESHEET] ?: return NONE

            val space = 4
            var currentX = x
            str.forEach { char ->
                if(char.isLetter()) {
                    val index = char.lowercaseChar() - 'a'
                    parent.frameBuffer.colorIndexBuffer.copyFrom(
                        spritesheet.pixels, currentX, y,
                        index * 4,
                        0,
                        4, 4,
                    ) { pixel: Array<Int> ->
                        if(pixel[0] == 0) {
                            null
                        } else {
                            pixel[0] = color
                            pixel
                        }
                    }
                }
                currentX += space
            }

            return NONE
        }
    }

    @DocFunction(
        name = "abs",
    )
    internal inner class abs : OneArgFunction() {
        @DocCall(
            documentation = "absolute value.",
            mainCall = true
        )
        override fun call(arg: LuaValue): LuaValue {
            return valueOf(abs(arg.todouble()))
        }

    }

    @DocFunction(
        name = "cos",
    )
    internal inner class cos : OneArgFunction() {
        @DocCall(
            documentation = "cosinus of the value passed as parameter.",
            mainCall = true
        )
        override fun call(arg: LuaValue): LuaValue {
            return valueOf(cos(arg.todouble()))
        }

    }

    @DocFunction(
        name = "sin",
    )
    internal inner class sin : OneArgFunction() {
        @DocCall(
            documentation = "sinus of the value passed as parameter.",
            mainCall = true
        )
        override fun call(arg: LuaValue): LuaValue {
            return valueOf(sin(arg.todouble()))
        }

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

    @DocFunction("rect", "Draw a rectangle")
    internal inner class rect : LibFunction() {
        // cornerX: Int, cornerY: Int, width: Int, height: Int, color: Int
        @DocCall(mainCall = true)
        override fun invoke(args: Varargs): Varargs {
            if (args.narg() < 5) return NONE
            val x = args.arg(1).checkint()
            val y = args.arg(2).checkint()
            val width = args.arg(3).checkint()
            val height = args.arg(4).checkint()
            val color = args.arg(5).checkint()
            for (i in x until x + width) {
                parent.frameBuffer.pixel(i, y, color)
                parent.frameBuffer.pixel(i, y + height - 1, color)
            }
            for (i in y until y + height) {
                parent.frameBuffer.pixel(x, i, color)
                parent.frameBuffer.pixel(x + width - 1, i, color)
            }
            return NONE
        }
    }

    @DocFunction("rectf", "Draw a filled rectangle")
    internal inner class rectf : LibFunction() {
        // cornerX: Int, cornerY: Int, width: Int, height: Int, color: Int
        @DocCall(mainCall = true)
        override fun invoke(args: Varargs): Varargs {
            if (args.narg() < 5) return NONE
            val x = args.arg(1).checkint()
            val y = args.arg(2).checkint()
            val width = args.arg(3).checkint()
            val height = args.arg(4).checkint()
            val color = args.arg(5).checkint()

            for (i in x until x + width) {
                for (j in y until y + height) {
                    parent.frameBuffer.pixel(i, j, color)
                }
            }
            return NONE
        }
    }

    private fun LuaValue.checkColorIndex(): Int {
        return if(this.isnumber()) {
            this.checkint()
        } else {
            FrameBuffer.gamePalette.getColorIndex(this.checkjstring()!!)
        }
    }

    internal inner class circlef : LibFunction() {
        // centerX: Int, centerY: Int, radius: Int, color: Int
        override fun call(a: LuaValue, b: LuaValue, c: LuaValue, d: LuaValue): LuaValue {
            val centerX = a.checkint()
            val centerY = b.checkint()
            val radius = c.checkint()
            val color = d.checkColorIndex() ?: 1

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

        @DocCall(
            "Generate a random value between 1 and the argument. " +
                "If a table is passed, it'll return a random element of the table."
        )
        override fun call(arg: LuaValue): LuaValue {
            return if (arg.istable()) {
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
            parent.frameBuffer.pixel(arg1.checkint(), arg2.checkint(), arg3.checkint())
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
