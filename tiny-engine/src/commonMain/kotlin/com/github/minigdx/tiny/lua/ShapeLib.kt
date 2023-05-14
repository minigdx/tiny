package com.github.minigdx.tiny.lua

import com.github.mingdx.tiny.doc.TinyArg
import com.github.mingdx.tiny.doc.TinyArgs
import com.github.mingdx.tiny.doc.TinyCall
import com.github.mingdx.tiny.doc.TinyFunction
import com.github.mingdx.tiny.doc.TinyLib
import com.github.minigdx.tiny.ColorIndex
import com.github.minigdx.tiny.Pixel
import com.github.minigdx.tiny.engine.GameResourceAccess
import org.luaj.vm2.LuaError
import org.luaj.vm2.LuaTable
import org.luaj.vm2.LuaValue
import org.luaj.vm2.LuaValue.Companion.valueOf
import org.luaj.vm2.Varargs
import org.luaj.vm2.lib.LibFunction
import org.luaj.vm2.lib.TwoArgFunction
import kotlin.math.abs

@TinyLib(
    "shape",
    "Shape API to draw...shapes. " +
        "Those shapes can be circle, rectangle, line or oval." +
        "All shapes can be draw filed or not filed."
)
class ShapeLib(private val resourceAccess: GameResourceAccess) : TwoArgFunction() {
    override fun call(arg1: LuaValue, arg2: LuaValue): LuaValue {
        val shp = LuaTable()
        shp["line"] = line()
        shp["oval"] = oval()
        shp["ovalf"] = ovalf()
        shp["rect"] = rect()
        shp["rectf"] = rectf()
        shp["circle"] = circle()
        shp["circlef"] = circlef()
        shp["trianglef"] = trianglef()
        shp["triangle"] = triangle()

        arg2.set("shape", shp)
        arg2.get("package").get("loaded").set("shape", shp)
        return shp
    }

    @TinyFunction("Draw a rectangle.")
    internal inner class rect : LibFunction() {
        @TinyCall("Draw a rectangle.")
        override fun invoke(@TinyArgs(arrayOf("x", "y", "width", "height", "color")) args: Varargs): Varargs {
            if (args.narg() < 5) return NONE
            val x = args.arg(1).checkint()
            val y = args.arg(2).checkint()
            val width = args.arg(3).checkint()
            val height = args.arg(4).checkint()
            val color = args.arg(5).checkColorIndex()
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

    @TinyFunction("Draw an oval.")
    internal inner class oval : LibFunction() {
        @TinyCall("Draw an oval using the default color.")
        override fun call(
            @TinyArg("centerX") a: LuaValue,
            @TinyArg("centerY") b: LuaValue,
            @TinyArg("radiusX") c: LuaValue,
            @TinyArg("radiusY") d: LuaValue,
        ): LuaValue {
            return super.invoke(arrayOf(a, b, c, d, valueOf("#FFFFFF"))).arg1()
        }

        @TinyCall("Draw an oval using the specified color.")
        override fun invoke(
            @TinyArgs(
                arrayOf(
                    "centerX",
                    "centerY",
                    "radiusX",
                    "radiusY",
                    "color"
                )
            ) args: Varargs
        ): Varargs {
            val centerX = args.checkint(1)
            val centerY = args.checkint(2)
            val radiusX = args.checkint(3)
            val radiusY = args.checkint(4)
            val color = args.arg(5).checkColorIndex()

            val frameBuffer = resourceAccess.frameBuffer

            var x = 0
            var y = radiusY
            var p: Int = (radiusY * radiusY) - (radiusX * radiusX * radiusY) + ((radiusX * radiusX) / 4)

            while (2 * x * radiusY * radiusY <= 2 * y * radiusX * radiusX) {
                frameBuffer.pixel(centerX + x, centerY + y, color)
                frameBuffer.pixel(centerX - x, centerY + y, color)
                frameBuffer.pixel(centerX + x, centerY - y, color)
                frameBuffer.pixel(centerX - x, centerY - y, color)

                x++

                if (p < 0) {
                    p += 2 * radiusY * radiusY * x + radiusY * radiusY
                } else {
                    y--
                    p += 2 * radiusY * radiusY * x - 2 * radiusX * radiusX * y + radiusY * radiusY
                }
            }

            p =
                (radiusY * radiusY) * (x * x + x) + (radiusX * radiusX) * (y * y - y) - (radiusX * radiusX * radiusY * radiusY)

            while (y >= 0) {
                frameBuffer.pixel(centerX + x, centerY + y, color)
                frameBuffer.pixel(centerX - x, centerY + y, color)
                frameBuffer.pixel(centerX + x, centerY - y, color)
                frameBuffer.pixel(centerX - x, centerY - y, color)

                y--

                if (p > 0) {
                    p -= 2 * radiusX * radiusX * y + radiusX * radiusX
                } else {
                    x++
                    p += 2 * radiusY * radiusY * x - 2 * radiusX * radiusX * y + radiusX * radiusX
                }
            }
            return NONE
        }
    }

    @TinyFunction("Draw an oval filled.")
    internal inner class ovalf : LibFunction() {
        @TinyCall("Draw a filled oval using the default color.")
        override fun call(
            @TinyArg("centerX") a: LuaValue,
            @TinyArg("centerY") b: LuaValue,
            @TinyArg("radiusX") c: LuaValue,
            @TinyArg("radiusY") d: LuaValue,
        ): LuaValue {
            return super.invoke(arrayOf(a, b, c, d, valueOf("#FFFFFF"))).arg1()
        }

        @TinyCall("Draw a filled oval using the specified color.")
        override fun invoke(
            @TinyArgs(
                arrayOf(
                    "centerX",
                    "centerY",
                    "radiusX",
                    "radiusY",
                    "color"
                )
            ) args: Varargs
        ): Varargs {
            val centerX = args.checkint(1)
            val centerY = args.checkint(2)
            val radiusX = args.checkint(3)
            val radiusY = args.checkint(4)
            val color = args.arg(5).checkColorIndex()

            val frameBuffer = resourceAccess.frameBuffer

            var x = 0
            var y = radiusY
            var p = (radiusY * radiusY) - (radiusX * radiusX * radiusY) + ((radiusX * radiusX) / 4)

            while (2 * x * radiusY * radiusY <= 2 * y * radiusX * radiusX) {
                // filled oval
                frameBuffer.fill(centerX - x, centerX + x, centerY + y, color)
                frameBuffer.fill(centerX - x, centerX + x, centerY - y, color)

                x++

                if (p < 0) {
                    p += 2 * radiusY * radiusY * x + radiusY * radiusY
                } else {
                    y--
                    p += 2 * radiusY * radiusY * x - 2 * radiusX * radiusX * y + radiusY * radiusY
                }
            }

            p =
                (radiusY * radiusY) * (x * x + x) + (radiusX * radiusX) * (y * y - y) - (radiusX * radiusX * radiusY * radiusY)

            while (y >= 0) {
                // filled oval
                frameBuffer.fill(centerX - x, centerX + x, centerY + y, color)
                frameBuffer.fill(centerX - x, centerX + x, centerY - y, color)

                y--

                if (p > 0) {
                    p -= 2 * radiusX * radiusX * y + radiusX * radiusX
                } else {
                    x++
                    p += 2 * radiusY * radiusY * x - 2 * radiusX * radiusX * y + radiusX * radiusX
                }
            }
            return NONE
        }
    }

    @TinyFunction("Draw a filled rectangle.", example = SHAPE_RECTF_EXAMPLE)
    internal inner class rectf : LibFunction() {
        // cornerX: Int, cornerY: Int, width: Int, height: Int, color: Int
        @TinyCall("Draw a filled rectangle.")
        override fun invoke(@TinyArgs(arrayOf("x", "y", "width", "height", "color")) args: Varargs): Varargs {
            if (args.narg() < 5) return NONE
            val x = args.arg(1).checkint()
            val y = args.arg(2).checkint()
            val width = args.arg(3).checkint()
            val height = args.arg(4).checkint()
            val color = args.arg(5).checkColorIndex()

            for (j in y until y + height) {
                resourceAccess.frameBuffer.fill(x, x + width, j, color)
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

    @TinyFunction("Draw a filled circle.", example = SHAPE_CIRCLEF_EXAMPLE)
    internal inner class circlef : LibFunction() {
        @TinyCall("Draw a circle at the coordinate (centerX, centerY) with the radius and the color.")
        override fun call(
            @TinyArg("centerX") a: LuaValue,
            @TinyArg("centerY") b: LuaValue,
            @TinyArg("radius") c: LuaValue,
            @TinyArg("color") d: LuaValue
        ): LuaValue {
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
                resourceAccess.frameBuffer.fill(centerX - x, centerX + x, centerY + y, color)
                resourceAccess.frameBuffer.fill(centerX - x, centerX + x, centerY - y, color)
                resourceAccess.frameBuffer.fill(centerX - y, centerX + y, centerY + x, color)
                resourceAccess.frameBuffer.fill(centerX - y, centerX + y, centerY - x, color)

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

    @TinyFunction("Draw a line.")
    internal inner class line : LibFunction() {

        @TinyCall("Draw a line.")
        override fun invoke(
            @TinyArgs(arrayOf("x0", "y0", "x1", "y2", "color"))
            args: Varargs
        ): Varargs {
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
                    args.arg(5).checkColorIndex()
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

        @TinyCall("Draw a line with a default color.")
        override fun call(
            @TinyArg("x0") a: LuaValue,
            @TinyArg("y0") b: LuaValue,
            @TinyArg("x1") c: LuaValue,
            @TinyArg("y1") d: LuaValue
        ): LuaValue {
            val args: Array<LuaValue> = arrayOf(a, b, c, d, valueOf("#FFFFFF"))
            invoke(args)
            return NONE
        }
    }

    @TinyFunction("Draw a circle.")
    internal inner class circle : LibFunction() {

        @TinyCall("Draw a circle with the default color.")
        override fun call(a: LuaValue, b: LuaValue, c: LuaValue): LuaValue {
            return call(a, b, c, valueOf("#FFFFFF"))
        }

        @TinyCall("Draw a circle.")
        override fun call(
            @TinyArg("centerX") a: LuaValue,
            @TinyArg("centerY") b: LuaValue,
            @TinyArg("radius") c: LuaValue,
            @TinyArg("color") d: LuaValue
        ): LuaValue {
            val centerX = a.checkint()
            val centerY = b.checkint()
            val radius = c.checkint()
            val color = d.checkColorIndex()

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

    @TinyFunction("Draw a filled triangle using the coordinates of (x1, y1), (x2, y2) and (x3, y3) and color.")
    inner class trianglef : LibFunction() {

        @TinyCall("Draw a filled triangle using the coordinates of (x1, y1), (x2, y2) and (x3, y3).")
        override fun invoke(
            @TinyArgs(arrayOf("x1", "y1", "x2", "y2", "x3", "y3", "color")) args: Varargs
        ): Varargs {
            if (args.narg() < 7) throw LuaError("Expected 7 args")

            val x1 = args.checkint(1)
            val y1 = args.checkint(2)
            val x2 = args.checkint(3)
            val y2 = args.checkint(4)
            val x3 = args.checkint(5)
            val y3 = args.checkint(6)
            val color = args.arg(7).checkColorIndex()

            // Sort the vertices from top to bottom
            val vertices = listOf(Pair(x1, y1), Pair(x2, y2), Pair(x3, y3))
            val sortedVertices = vertices.sortedBy { it.second }

            // Retrieve the sorted vertices
            val topVertex = sortedVertices[0]
            val middleVertex = sortedVertices[1]
            val bottomVertex = sortedVertices[2]

            // Calculate the slopes of the two sides of the triangle
            val slope1 = (middleVertex.first - topVertex.first).toFloat() / (middleVertex.second - topVertex.second)
            val slope2 = (bottomVertex.first - topVertex.first).toFloat() / (bottomVertex.second - topVertex.second)

            // Draw the upper part of the triangle
            for (y in topVertex.second until middleVertex.second) {
                val xx1 = topVertex.first + ((y - topVertex.second) * slope1).toInt()
                val xx2 = topVertex.first + ((y - topVertex.second) * slope2).toInt()
                resourceAccess.frameBuffer.fill(xx1, xx2, y, color)
            }

            // Calculate the slopes of the two sides of the bottom part of the triangle
            val slope3 = (bottomVertex.first - middleVertex.first).toFloat() / (bottomVertex.second - middleVertex.second)
            val slope4 = (bottomVertex.first - topVertex.first).toFloat() / (bottomVertex.second - topVertex.second)

            // Draw the lower part of the triangle
            for (y in middleVertex.second until bottomVertex.second) {
                val xx1 = middleVertex.first + ((y - middleVertex.second) * slope3).toInt()
                val xx2 = topVertex.first + ((y - topVertex.second) * slope4).toInt()
                resourceAccess.frameBuffer.fill(xx1, xx2, y, color)
            }

            return NONE
        }
    }

    @TinyFunction("Draw a triangle using the coordinates of (x1, y1), (x2, y2) and (x3, y3) and color.")
    inner class triangle : LibFunction() {

        private val line = line()

        @TinyCall("Draw a triangle using the coordinates of (x1, y1), (x2, y2) and (x3, y3).")
        override fun invoke(
            @TinyArgs(arrayOf("x1", "y1", "x2", "y2", "x3", "y3", "color")) args: Varargs
        ): Varargs {
            if (args.narg() < 7) throw LuaError("Expected 7 args")

            val x1 = args.checkint(1)
            val y1 = args.checkint(2)
            val x2 = args.checkint(3)
            val y2 = args.checkint(4)
            val x3 = args.checkint(5)
            val y3 = args.checkint(6)
            val color = args.arg(7).checkColorIndex()

            line.invoke(
                varargsOf(
                    arrayOf(
                        valueOf(x1), valueOf(y1),
                        valueOf(x2), valueOf(y2),
                        valueOf(color)
                    )
                )
            )

            line.invoke(
                varargsOf(
                    arrayOf(
                        valueOf(x2), valueOf(y2),
                        valueOf(x3), valueOf(y3),
                        valueOf(color)
                    )
                )
            )

            line.invoke(
                varargsOf(
                    arrayOf(
                        valueOf(x3), valueOf(y3),
                        valueOf(x1), valueOf(y1),
                        valueOf(color)
                    )
                )
            )

            return NONE
        }
    }
}
