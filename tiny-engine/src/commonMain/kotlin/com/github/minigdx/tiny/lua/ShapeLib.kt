package com.github.minigdx.tiny.lua

import com.github.mingdx.tiny.doc.TinyArg
import com.github.mingdx.tiny.doc.TinyArgs
import com.github.mingdx.tiny.doc.TinyCall
import com.github.mingdx.tiny.doc.TinyFunction
import com.github.mingdx.tiny.doc.TinyLib
import com.github.minigdx.tiny.ColorIndex
import com.github.minigdx.tiny.Pixel
import com.github.minigdx.tiny.engine.GameOptions
import com.github.minigdx.tiny.render.VirtualFrameBuffer
import org.luaj.vm2.LuaError
import org.luaj.vm2.LuaTable
import org.luaj.vm2.LuaValue
import org.luaj.vm2.Varargs
import org.luaj.vm2.lib.LibFunction
import org.luaj.vm2.lib.TwoArgFunction

private class Shape(private val gameOptions: GameOptions) {
    fun rectArgs(args: Varargs): List<Int>? {
        when (args.narg()) {
            // rect including color
            in 1..1 -> {
                val table = args.arg1().opttable(null) ?: return null
                return listOf(
                    table["x"].checkint(),
                    table["y"].checkint(),
                    table["width"].checkint(),
                    table["height"].checkint(),
                    table["color"].checkColorIndex(),
                )
            }
            // rect with color
            in 2..2 -> {
                val table = args.arg1().opttable(null) ?: return null
                return listOf(
                    table["x"].checkint(),
                    table["y"].checkint(),
                    table["width"].checkint(),
                    table["height"].checkint(),
                    args.arg(2).checkColorIndex(),
                )
            }
            // not supported
            in 3..4 -> {
                return null
            }
            // every args
            else -> {
                val x = args.arg(1).checkint()
                val y = args.arg(2).checkint()
                val width = args.arg(3).checkint()
                val height = args.arg(4).checkint()
                val color = args.arg(5).checkColorIndex()
                return listOf(x, y, width, height, color)
            }
        }
    }

    private fun LuaValue.checkColorIndex(): Int {
        return if (this.isnumber()) {
            this.checkint()
        } else {
            gameOptions.colors().getColorIndex(this.checkjstring()!!)
        }
    }
}

@TinyLib(
    "shape",
    "Shape API to draw...shapes. " +
        "Those shapes can be circle, rectangle, line or oval." +
        "All shapes can be draw filed or not filed.",
)
class ShapeLib(
    private val gameOptions: GameOptions,
    private val virtualFrameBuffer: VirtualFrameBuffer,
) : TwoArgFunction() {
    private val shape = Shape(gameOptions)

    override fun call(
        arg1: LuaValue,
        arg2: LuaValue,
    ): LuaValue {
        val shp = LuaTable()
        shp["line"] = line()
        shp["rect"] = rect()
        shp["rectf"] = rectf()
        shp["circle"] = circle()
        shp["circlef"] = circlef()
        shp["trianglef"] = trianglef()
        shp["triangle"] = triangle()
        shp["gradient"] = gradient()

        arg2.set("shape", shp)
        arg2.get("package").get("loaded").set("shape", shp)
        return shp
    }

    @TinyFunction("Draw a rectangle.", example = SHAPE_RECTF_EXAMPLE)
    internal inner class rect : LibFunction() {
        @TinyCall("Draw a rectangle.")
        override fun invoke(
            @TinyArgs(["x", "y", "width", "height", "color"]) args: Varargs,
        ): Varargs {
            val (x, y, width, height, color) = shape.rectArgs(args) ?: return NIL

            virtualFrameBuffer.drawRect(
                x,
                y,
                width,
                height,
                color,
                false,
            )

            return NIL
        }

        @TinyCall("Draw a rectangle.")
        override fun call(
            @TinyArg("rect", "A rectangle {x, y, width, height, color}") a: LuaValue,
        ): LuaValue {
            return super.call(a)
        }

        @TinyCall("Draw a rectangle using a rectangle and a color.")
        override fun call(
            @TinyArg("rect", "A rectangle {x, y, width, height}") a: LuaValue,
            @TinyArg("color") b: LuaValue,
        ): LuaValue = super.call(a, b)
    }

    @TinyFunction("Draw a filled rectangle.", example = SHAPE_RECTF_EXAMPLE)
    internal inner class rectf : LibFunction() {
        // cornerX: Int, cornerY: Int, width: Int, height: Int, color: Int
        @TinyCall("Draw a filled rectangle.")
        override fun invoke(
            @TinyArgs(["x", "y", "width", "height", "color"]) args: Varargs,
        ): Varargs {
            val (x, y, width, height, color) = shape.rectArgs(args) ?: return NIL

            virtualFrameBuffer.drawRect(x, y, width, height, color, filled = true)

            return NIL
        }

        @TinyCall("Draw a filled rectangle.")
        override fun call(
            @TinyArg("rect", "A rectangle {x, y, width, height, color}") a: LuaValue,
        ): LuaValue {
            return super.call(a)
        }

        @TinyCall("Draw a filled rectangle using a rectangle and a color.")
        override fun call(
            @TinyArg("rect", "A rectangle {x, y, width, height}") a: LuaValue,
            @TinyArg("color") b: LuaValue,
        ): LuaValue = super.call(a, b)
    }

    private fun LuaValue.checkColorIndex(): Int {
        return if (this.isnumber()) {
            this.checkint()
        } else {
            gameOptions.colors().getColorIndex(this.checkjstring()!!)
        }
    }

    @TinyFunction("Draw a filled circle.", example = SHAPE_CIRCLEF_EXAMPLE)
    internal inner class circlef : LibFunction() {
        @TinyCall("Draw a circle at the coordinate (centerX, centerY) with the radius and the color.")
        override fun call(
            @TinyArg("centerX") a: LuaValue,
            @TinyArg("centerY") b: LuaValue,
            @TinyArg("radius") c: LuaValue,
            @TinyArg("color") d: LuaValue,
        ): LuaValue {
            val centerX = a.checkint()
            val centerY = b.checkint()
            val radius = c.checkint()
            val color = d.checkColorIndex()

            virtualFrameBuffer.drawCircle(centerX, centerY, radius, color, filled = true)
            return NONE
        }
    }

    @TinyFunction("Draw a line.", example = SHAPE_LINE_EXAMPLE)
    internal inner class line : LibFunction() {
        @TinyCall("Draw a line.")
        override fun invoke(
            @TinyArgs(["x0", "y0", "x1", "y1", "color"])
            args: Varargs,
        ): Varargs {
            return when (args.narg()) {
                0 -> call()
                1 -> call(args.arg1())
                2 -> call(args.arg1(), args.arg(2))
                3 -> call(args.arg1(), args.arg(2), args.arg(3))
                4 -> call(args.arg1(), args.arg(2), args.arg(3), args.arg(4))
                else ->
                    draw(
                        args.arg1().checkint(),
                        args.arg(2).checkint(),
                        args.arg(3).checkint(),
                        args.arg(4).checkint(),
                        args.arg(5).checkColorIndex(),
                    )
            }
        }

        private fun draw(
            x0: Pixel,
            y0: Pixel,
            x1: Pixel,
            y1: Pixel,
            color: ColorIndex,
        ): LuaValue {
            if (x0 == x1 && y0 == y1) {
                return NONE
            } else {
                virtualFrameBuffer.drawLine(x0, y0, x1, y1, color)
            }
            return NONE
        }

        @TinyCall("Draw a line with a default color.")
        override fun call(
            @TinyArg("x0") a: LuaValue,
            @TinyArg("y0") b: LuaValue,
            @TinyArg("x1") c: LuaValue,
            @TinyArg("y1") d: LuaValue,
        ): LuaValue {
            val args: Array<LuaValue> = arrayOf(a, b, c, d, valueOf("#FFFFFF"))
            invoke(args)
            return NONE
        }
    }

    @TinyFunction("Draw a circle.", example = SHAPE_CIRCLEF_EXAMPLE)
    internal inner class circle : LibFunction() {
        @TinyCall("Draw a circle with the default color.")
        override fun call(
            a: LuaValue,
            b: LuaValue,
            c: LuaValue,
        ): LuaValue {
            return call(a, b, c, valueOf("#FFFFFF"))
        }

        @TinyCall("Draw a circle.")
        override fun call(
            @TinyArg("centerX") a: LuaValue,
            @TinyArg("centerY") b: LuaValue,
            @TinyArg("radius") c: LuaValue,
            @TinyArg("color") d: LuaValue,
        ): LuaValue {
            val centerX = a.checkint()
            val centerY = b.checkint()
            val radius = c.checkint()
            val color = d.checkColorIndex()

            virtualFrameBuffer.drawCircle(centerX, centerY, radius, color, filled = false)

            return NONE
        }
    }

    @TinyFunction(
        "Draw a filled triangle using the coordinates of (x1, y1), (x2, y2) and (x3, y3) and color.",
        example = SHAPE_TRIANGLEF_EXAMPLE,
    )
    inner class trianglef : LibFunction() {
        @TinyCall("Draw a filled triangle using the coordinates of (x1, y1), (x2, y2) and (x3, y3).")
        override fun invoke(
            @TinyArgs(["x1", "y1", "x2", "y2", "x3", "y3", "color"]) args: Varargs,
        ): Varargs {
            if (args.narg() < 7) throw LuaError("Expected 7 args")

            val x1 = args.checkint(1)
            val y1 = args.checkint(2)
            val x2 = args.checkint(3)
            val y2 = args.checkint(4)
            val x3 = args.checkint(5)
            val y3 = args.checkint(6)
            val color = args.arg(7).checkColorIndex()

            virtualFrameBuffer.drawTriangle(x1, y1, x2, y2, x3, y3, color, filled = true)
            return NONE
        }
    }

    @TinyFunction(
        "Draw a triangle using the coordinates of (x1, y1), (x2, y2) and (x3, y3) and color.",
        example = SHAPE_TRIANGLEF_EXAMPLE,
    )
    inner class triangle : LibFunction() {
        private val line = line()

        @TinyCall("Draw a triangle using the coordinates of (x1, y1), (x2, y2) and (x3, y3).")
        override fun invoke(
            @TinyArgs(["x1", "y1", "x2", "y2", "x3", "y3", "color"]) args: Varargs,
        ): Varargs {
            if (args.narg() < 7) throw LuaError("Expected 7 args")

            val x1 = args.checkint(1)
            val y1 = args.checkint(2)
            val x2 = args.checkint(3)
            val y2 = args.checkint(4)
            val x3 = args.checkint(5)
            val y3 = args.checkint(6)
            val color = args.arg(7).checkColorIndex()

            virtualFrameBuffer.drawTriangle(x1, y1, x2, y2, x3, y3, color, filled = false)
            return NONE
        }
    }

    @TinyFunction(
        "Draw a gradient using dithering, only from color c1 to color c2.",
        example = SHAPE_GRADIENT_EXAMPLE,
    )
    inner class gradient : LibFunction() {
        private val dithering =
            listOf(
                0x0000,
                0x0001,
                0x0401,
                0x0405,
                0x0505,
                0x0525,
                0x8525,
                0x85A5,
                0xA5A5,
                0xA5A7,
                0xADA7,
                0xADAF,
                0xAFAF,
                0xAFBF,
                0xEFBF,
                0xEFFF,
                0xFFFF,
            )

        @TinyCall("Draw a gradient using dithering, only from color c1 to color c2.")
        override fun invoke(
            @TinyArgs(["x", "y", "width", "height", "color1", "color2", "is_horizontal"]) args: Varargs,
        ): Varargs {
            if (args.narg() < 6) throw LuaError("Expected 6  args")

            val x = args.checkint(1)
            val y = args.checkint(2)
            val width = args.checkint(3)
            val height = args.checkint(4)

            val color2 = args.arg(6).checkColorIndex()

            val isHorizontal = args.optboolean(7, false)

            // Draw the background color
            virtualFrameBuffer.drawRect(
                args.arg(1).checkint(),
                args.arg(2).checkint(),
                args.arg(3).checkint(),
                args.arg(4).checkint(),
                args.arg(5).checkint(),
                filled = true,
            )

            val previous = virtualFrameBuffer.dithering(0xFFFF)
            dithering.forEachIndexed { index, pattern ->
                if (isHorizontal) {
                    val xx = x + width * index / dithering.size
                    val xx2 = x + width * (index + 1) / dithering.size
                    virtualFrameBuffer.dithering(pattern)
                    virtualFrameBuffer.drawRect(
                        xx,
                        y,
                        xx2 - xx,
                        height,
                        color2,
                        filled = true,
                    )
                } else {
                    val yy = y + height * index / dithering.size
                    val yy2 = y + height * (index + 1) / dithering.size
                    virtualFrameBuffer.dithering(pattern)
                    virtualFrameBuffer.drawRect(
                        x,
                        yy,
                        width,
                        yy2 - yy,
                        color2,
                        filled = true,
                    )
                }
            }
            virtualFrameBuffer.dithering(previous)
            return NIL
        }
    }
}
