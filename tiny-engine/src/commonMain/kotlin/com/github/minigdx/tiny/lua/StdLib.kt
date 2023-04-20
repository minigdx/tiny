package com.github.minigdx.tiny.lua

import com.github.mingdx.tiny.doc.TinyCall
import com.github.mingdx.tiny.doc.TinyFunction
import com.github.minigdx.tiny.ColorIndex
import com.github.minigdx.tiny.Pixel
import com.github.minigdx.tiny.engine.GameOptions
import com.github.minigdx.tiny.engine.GameResourceAccess
import org.luaj.vm2.LuaTable
import org.luaj.vm2.LuaValue
import org.luaj.vm2.Varargs
import org.luaj.vm2.lib.LibFunction
import org.luaj.vm2.lib.OneArgFunction
import org.luaj.vm2.lib.ThreeArgFunction
import org.luaj.vm2.lib.TwoArgFunction
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin
import kotlin.random.Random

interface StdLibListener {
    fun exit(nextScriptIndex: Int)
}

class StdLib(val gameOptions: GameOptions, val resourceAccess: GameResourceAccess, val listener: StdLibListener) :
    TwoArgFunction() {

    private var currentSpritesheet: Int = 0

    override fun call(arg1: LuaValue, arg2: LuaValue): LuaValue {
        val tiny = LuaTable()
        arg2["exit"] = exit()
        arg2["line"] = line()
        arg2["pset"] = pset()
        arg2["pget"] = pget()
        arg2["rnd"] = rnd()
        arg2["cls"] = cls()
        arg2["cos"] = cos()
        arg2["sin"] = sin()
        arg2["min"] = min()
        arg2["max"] = max()
        arg2["abs"] = abs()
        arg2["rect"] = rect()
        arg2["rectf"] = rectf()
        arg2["circle"] = circle()
        arg2["circlef"] = circlef()
        arg2["sspr"] = sspr()
        arg2["spr"] = spr()
        arg2["print"] = print()
        arg2["debug"] = debug()
        arg2["PI"] = valueOf(PI)
        return tiny
    }

    internal inner class debug : OneArgFunction() {
        override fun call(arg: LuaValue): LuaValue {
            println("🐞 -> $arg")
            // resourceAccess.sound(0)?.play()
            resourceAccess.sound(0)?.play()
            return NONE
        }
    }

    @TinyFunction
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

            val spritesheet = resourceAccess.spritesheet(currentSpritesheet) ?: return NONE

            resourceAccess.frameBuffer.copyFrom(
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

    @TinyFunction
    internal inner class spr : LibFunction() {
        // sprn, x, y, flip x, flip y
        override fun call(a: LuaValue, b: LuaValue, c: LuaValue): LuaValue {
            val sprN = a.checkint()
            val x = b.checkint()
            val y = c.checkint()
            val spritesheet = resourceAccess.spritesheet(currentSpritesheet) ?: return NONE

            val (sw, sh) = gameOptions.spriteSize
            val nbSpritePerRow = spritesheet.width / sw

            val column = sprN % nbSpritePerRow
            val row = (sprN - column) / nbSpritePerRow
            resourceAccess.frameBuffer.copyFrom(
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

            val spritesheet = resourceAccess.spritesheet(currentSpritesheet) ?: return NONE

            val (sw, sh) = gameOptions.spriteSize
            val nbSpritePerRow = spritesheet.width / sw

            val column = sprN % nbSpritePerRow
            val row = (sprN - column) / nbSpritePerRow
            resourceAccess.frameBuffer.copyFrom(
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

    @TinyFunction
    internal inner class print : LibFunction() {

        @TinyCall(
            description = "print on the screen a string",
            mainCall = true
        )
        override fun call(a: LuaValue): LuaValue {
            return NONE
        }

        override fun call(a: LuaValue, b: LuaValue, c: LuaValue): LuaValue {
            return call(a, b, c, valueOf("#FFFFFF"))
        }

        override fun call(a: LuaValue, b: LuaValue, c: LuaValue, d: LuaValue): LuaValue {
            val str = a.checkjstring() ?: return NONE
            val x = b.checkint()
            val y = c.checkint()
            val color = d.checkColorIndex()

            val spritesheet = resourceAccess.bootSpritesheet ?: return NONE

            val space = 4
            var currentX = x
            str.forEach { char ->

                val coord = if (char.isLetter()) {
                    // The character has an accent. Let's try to get rid of it
                    val l = if (char.hasAccent) {
                        ACCENT_MAP[char.lowercaseChar()] ?: char.lowercaseChar()
                    } else {
                        char.lowercaseChar()
                    }
                    val index = l - 'a'
                    index to 0
                } else if (char.isDigit()) {
                    val index = char.lowercaseChar() - '0'
                    index to 4
                } else if (char in '!'..'@') {
                    val index = char.lowercaseChar() - '!'
                    index to 8
                } else if (char in '['..'\'') {
                    val index = char.lowercaseChar() - '['
                    index to 12
                } else if (char in '{'..'~') {
                    val index = char.lowercaseChar() - '{'
                    index to 16
                } else {
                    // Maybe it's an emoji: try EMOJI MAP conversion
                    EMOJI_MAP[char]
                }
                if (coord != null) {
                    val (index, sheetY) = coord
                    resourceAccess.frameBuffer.copyFrom(
                        spritesheet.pixels, currentX, y,
                        index * 4,
                        sheetY,
                        4, 4,
                    ) { pixel: Array<Int>, _, _ ->
                        if (pixel[0] == 0) {
                            pixel
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

        val Char.hasAccent: Boolean
            get() = this.isLetter() && this.lowercaseChar() !in 'a'..'z'
    }

    @TinyFunction(
        name = "abs",
    )
    internal inner class abs : OneArgFunction() {
        @TinyCall(
            description = "absolute value.",
            mainCall = true
        )
        override fun call(arg: LuaValue): LuaValue {
            return valueOf(abs(arg.todouble()))
        }
    }

    @TinyFunction(
        name = "cos",
    )
    internal inner class cos : OneArgFunction() {
        @TinyCall(
            description = "cosinus of the value passed as parameter.",
            mainCall = true
        )
        override fun call(arg: LuaValue): LuaValue {
            return valueOf(cos(arg.todouble()))
        }
    }

    @TinyFunction(
        name = "sin",
    )
    internal inner class sin : OneArgFunction() {
        @TinyCall(
            description = "sinus of the value passed as parameter.",
            mainCall = true
        )
        override fun call(arg: LuaValue): LuaValue {
            return valueOf(sin(arg.todouble()))
        }
    }

    @TinyFunction(
        name = "min",
    )
    internal inner class min : TwoArgFunction() {
        @TinyCall(
            description = "minimun value between two values. Those values can be numbers or string.",
            mainCall = true
        )
        override fun call(arg1: LuaValue, arg2: LuaValue): LuaValue {
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
            return if (arg1.isint() || arg1.islong()) {
                valueOf(max(arg1.checkint(), arg2.checkint()))
            } else if (arg1.isnumber()) {
                valueOf(max(arg1.checkdouble(), arg2.checkdouble()))
            } else {
                valueOf(listOf(arg1.checkjstring() ?: "", arg2.checkjstring() ?: "").sorted().last())
            }
        }
    }

    internal inner class exit : OneArgFunction() {

        override fun call(arg: LuaValue): LuaValue {
            listener.exit(arg.checkint())
            return NONE
        }
    }

    @TinyFunction("rect", "Draw a rectangle")
    internal inner class rect : LibFunction() {
        // cornerX: Int, cornerY: Int, width: Int, height: Int, color: Int
        @TinyCall(mainCall = true)
        override fun invoke(args: Varargs): Varargs {
            if (args.narg() < 5) return NONE
            val x = args.arg(1).checkint()
            val y = args.arg(2).checkint()
            val width = args.arg(3).checkint()
            val height = args.arg(4).checkint()
            val color = args.arg(5).checkint()
            for (i in x until x + width) {
                resourceAccess.frameBuffer.pixel(i, y, color)
                resourceAccess.frameBuffer.pixel(i, y + height - 1, color)
            }
            for (i in y until y + height) {
                resourceAccess.frameBuffer.pixel(x, i, color)
                resourceAccess.frameBuffer.pixel(x + width - 1, i, color)
            }
            return NONE
        }
    }

    @TinyFunction("rectf", "Draw a filled rectangle")
    internal inner class rectf : LibFunction() {
        // cornerX: Int, cornerY: Int, width: Int, height: Int, color: Int
        @TinyCall(mainCall = true)
        override fun invoke(args: Varargs): Varargs {
            if (args.narg() < 5) return NONE
            val x = args.arg(1).checkint()
            val y = args.arg(2).checkint()
            val width = args.arg(3).checkint()
            val height = args.arg(4).checkint()
            val color = args.arg(5).checkint()

            for (i in x until x + width) {
                for (j in y until y + height) {
                    resourceAccess.frameBuffer.pixel(i, j, color)
                }
            }
            return NONE
        }
    }

    private fun LuaValue.checkColorIndex(): Int {
        return if (this.isnumber()) {
            this.checkint()
        } else {
            resourceAccess.frameBuffer.gamePalette.getColorIndex(this.checkjstring()!!)
        }
    }

    internal inner class circlef : LibFunction() {
        // centerX: Int, centerY: Int, radius: Int, color: Int
        override fun call(a: LuaValue, b: LuaValue, c: LuaValue, d: LuaValue): LuaValue {
            val centerX = a.checkint()
            val centerY = b.checkint()
            val radius = c.checkint()
            val color = d.checkColorIndex()

            var x = 0
            var y = radius
            var dst = 3 - 2 * radius

            while (x <= y) {
                // Draw the outline of the circle
                resourceAccess.frameBuffer.pixel(centerX + x, centerY + y, color)
                resourceAccess.frameBuffer.pixel(centerX - x, centerY + y, color)
                resourceAccess.frameBuffer.pixel(centerX + x, centerY - y, color)
                resourceAccess.frameBuffer.pixel(centerX - x, centerY - y, color)
                resourceAccess.frameBuffer.pixel(centerX + y, centerY + x, color)
                resourceAccess.frameBuffer.pixel(centerX - y, centerY + x, color)
                resourceAccess.frameBuffer.pixel(centerX + y, centerY - x, color)
                resourceAccess.frameBuffer.pixel(centerX - y, centerY - x, color)

                // Fill the circle
                for (i in centerX - x..centerX + x) {
                    resourceAccess.frameBuffer.pixel(i, centerY + y, color)
                    resourceAccess.frameBuffer.pixel(i, centerY - y, color)
                }
                for (i in centerX - y..centerX + y) {
                    resourceAccess.frameBuffer.pixel(i, centerY + x, color)
                    resourceAccess.frameBuffer.pixel(i, centerY - x, color)
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
            val dx = abs(x1 - x0)
            val dy = abs(y1 - y0)
            val sx = if (x0 < x1) 1 else -1
            val sy = if (y0 < y1) 1 else -1
            var err = dx - dy

            var x = x0
            var y = y0

            while (true) {
                resourceAccess.frameBuffer.pixel(x, y, color)
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
                resourceAccess.frameBuffer.pixel(centerX + x, centerY + y, color)
                resourceAccess.frameBuffer.pixel(centerX - x, centerY + y, color)
                resourceAccess.frameBuffer.pixel(centerX + x, centerY - y, color)
                resourceAccess.frameBuffer.pixel(centerX - x, centerY - y, color)
                resourceAccess.frameBuffer.pixel(centerX + y, centerY + x, color)
                resourceAccess.frameBuffer.pixel(centerX - y, centerY + x, color)
                resourceAccess.frameBuffer.pixel(centerX + y, centerY - x, color)
                resourceAccess.frameBuffer.pixel(centerX - y, centerY - x, color)

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
            resourceAccess.frameBuffer.clear(0)
            return NONE
        }

        override fun call(arg: LuaValue): LuaValue {
            resourceAccess.frameBuffer.clear(arg.checkint())
            return NONE
        }
    }

    @TinyFunction("rnd", "generate random values")
    internal inner class rnd : TwoArgFunction() {
        @TinyCall("Generate a random int (negative or positive value)", mainCall = true)
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
                val index = Random.nextInt(1, table.arrayLength + 1)
                table[index]
            } else {

                valueOf(Random.nextInt(abs(arg.checkint())))
            }
        }

        @TinyCall("Generate a random value between the first and the second argument.")
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
            resourceAccess.frameBuffer.pixel(arg1.checkint(), arg2.checkint(), arg3.checkint())
            return NONE
        }
    }

    internal inner class pget : TwoArgFunction() {
        override fun call(arg1: LuaValue, arg2: LuaValue): LuaValue {
            val index = resourceAccess.frameBuffer.pixel(arg1.checkint(), arg2.checkint())
            return valueOf(index)
        }
    }

    companion object {
        val ACCENT_MAP = mapOf(
            'à' to 'a', 'á' to 'a', 'â' to 'a', 'ã' to 'a', 'ä' to 'a', 'å' to 'a',
            'ç' to 'c',
            'è' to 'e', 'é' to 'e', 'ê' to 'e', 'ë' to 'e',
            'ì' to 'i', 'í' to 'i', 'î' to 'i', 'ï' to 'i',
            'ñ' to 'n',
            'ò' to 'o', 'ó' to 'o', 'ô' to 'o', 'õ' to 'o', 'ö' to 'o',
            'ù' to 'u', 'ú' to 'u', 'û' to 'u', 'ü' to 'u',
            'ý' to 'y', 'ÿ' to 'y'
        )

        val EMOJI_MAP = mapOf(
            '⚠' to (0 to 0),
        )
    }
}
