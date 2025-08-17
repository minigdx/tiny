package com.github.minigdx.tiny.lua

import com.github.mingdx.tiny.doc.TinyArg
import com.github.mingdx.tiny.doc.TinyArgs
import com.github.mingdx.tiny.doc.TinyCall
import com.github.mingdx.tiny.doc.TinyFunction
import com.github.mingdx.tiny.doc.TinyLib
import com.github.minigdx.tiny.ColorIndex
import com.github.minigdx.tiny.Pixel
import com.github.minigdx.tiny.engine.GameOptions
import com.github.minigdx.tiny.engine.GameResourceAccess2
import com.github.minigdx.tiny.render.batch.BatchManager
import org.luaj.vm2.LuaTable
import org.luaj.vm2.LuaValue
import org.luaj.vm2.Varargs
import org.luaj.vm2.lib.LibFunction
import org.luaj.vm2.lib.TwoArgFunction
import kotlin.math.abs

private class Shape(private val resourceAccess: GameResourceAccess2) {
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
            // FIXME:
            TODO()
/*
            resourceAccess.frameBuffer.gamePalette.getColorIndex(this.checkjstring()!!)

 */
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
    private val resourceAccess: GameResourceAccess2,
    private val gameOptions: GameOptions,
    private val batchManager: BatchManager,
) : TwoArgFunction() {
    private val shape = Shape(resourceAccess)

    override fun call(
        arg1: LuaValue,
        arg2: LuaValue,
    ): LuaValue {
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

            // FIXME:
            TODO()
/*
            for (i in x until x + width) {
                resourceAccess.frameBuffer.pixel(i, y, color)
                resourceAccess.frameBuffer.pixel(i, y + height - 1, color)
            }
            for (i in y until y + height) {
                resourceAccess.frameBuffer.pixel(x, i, color)
                resourceAccess.frameBuffer.pixel(x + width - 1, i, color)
            }
            resourceAccess.addOp(FrameBufferOperation)
            return NIL

 */
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

    @TinyFunction("Draw an oval.", example = SHAPE_OVALF_EXAMPLE)
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
                    "color",
                ),
            ) args: Varargs,
        ): Varargs {
            val centerX = args.checkint(1)
            val centerY = args.checkint(2)
            val radiusX = args.checkint(3)
            val radiusY = args.checkint(4)
            val color = args.arg(5).checkColorIndex()

            // FIXME:
            TODO()
/*
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
            resourceAccess.addOp(FrameBufferOperation)
            return NONE

 */
        }
    }

    @TinyFunction("Draw an oval filled.", example = SHAPE_OVALF_EXAMPLE)
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
                    "color",
                ),
            ) args: Varargs,
        ): Varargs {
            val centerX = args.checkint(1)
            val centerY = args.checkint(2)
            val radiusX = args.checkint(3)
            val radiusY = args.checkint(4)
            val color = args.arg(5).checkColorIndex()

            // FIXME:
            TODO()
/*
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
            resourceAccess.addOp(FrameBufferOperation)
            return NIL

 */
        }
    }

    @TinyFunction("Draw a filled rectangle.", example = SHAPE_RECTF_EXAMPLE)
    internal inner class rectf : LibFunction() {
        // cornerX: Int, cornerY: Int, width: Int, height: Int, color: Int
        @TinyCall("Draw a filled rectangle.")
        override fun invoke(
            @TinyArgs(["x", "y", "width", "height", "color"]) args: Varargs,
        ): Varargs {
            val (x, y, width, height, color) = shape.rectArgs(args) ?: return NIL

            // FIXME:
            TODO()
/*
            // Add the framebuffer in the batch
            batchManager.submitSprite(
                source = resourceAccess.frameBuffer.asSpriteSheet,
                sourceX = 0,
                sourceY = 0,
                sourceWidth = resourceAccess.frameBuffer.width,
                sourceHeight = resourceAccess.frameBuffer.height,
                destinationX = 0,
                destinationY = 0,
                flipX = false,
                flipY = false,
                dither = resourceAccess.frameBuffer.blender.dithering,
                palette = resourceAccess.frameBuffer.blender.switch,
                camera = resourceAccess.frameBuffer.camera,
                clipper = resourceAccess.frameBuffer.clipper,
            )

            // Draw the shape in the current frame buffer
            for (j in y until y + height) {
                resourceAccess.frameBuffer.fill(x, x + width, j, color)
            }

            return NIL

 */
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
            // FIXME:
            TODO()
/*
            resourceAccess.frameBuffer.gamePalette.getColorIndex(this.checkjstring()!!)

 */
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

            var x = 0
            var y = radius
            var dst = 3 - 2 * radius
            // FIXME:
            TODO()
/*
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
            resourceAccess.addOp(FrameBufferOperation)
            return NIL

 */
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
            // (x1, y1), (x2, y2)
            val dx = abs(x1 - x0)
            val dy = abs(y1 - y0)
            val sx = if (x0 < x1) 1 else -1
            val sy = if (y0 < y1) 1 else -1
            var err = dx - dy

            var x = x0
            var y = y0
            // FIXME:
            TODO()
/*
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
            // FIXME(Performance): Add the current primitive as a sprite.
            resourceAccess.addOp(FrameBufferOperation)
            return NONE

 */
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
            // FIXME:
            TODO()
/*
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
            resourceAccess.addOp(FrameBufferOperation)
            return NONE

 */
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
            // FIXME:
            TODO()
/*
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
            val slope3 =
                (bottomVertex.first - middleVertex.first).toFloat() / (bottomVertex.second - middleVertex.second)
            val slope4 = (bottomVertex.first - topVertex.first).toFloat() / (bottomVertex.second - topVertex.second)

            // Draw the lower part of the triangle
            for (y in middleVertex.second until bottomVertex.second) {
                val xx1 = middleVertex.first + ((y - middleVertex.second) * slope3).toInt()
                val xx2 = topVertex.first + ((y - topVertex.second) * slope4).toInt()
                resourceAccess.frameBuffer.fill(xx1, xx2, y, color)
            }
            resourceAccess.addOp(FrameBufferOperation)
            return NONE

 */
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
            // FIXME:
            TODO()
/*
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
                        valueOf(x1),
                        valueOf(y1),
                        valueOf(x2),
                        valueOf(y2),
                        valueOf(color),
                    ),
                ),
            )

            line.invoke(
                varargsOf(
                    arrayOf(
                        valueOf(x2),
                        valueOf(y2),
                        valueOf(x3),
                        valueOf(y3),
                        valueOf(color),
                    ),
                ),
            )

            line.invoke(
                varargsOf(
                    arrayOf(
                        valueOf(x3),
                        valueOf(y3),
                        valueOf(x1),
                        valueOf(y1),
                        valueOf(color),
                    ),
                ),
            )
            resourceAccess.addOp(FrameBufferOperation)
            return NONE

 */
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
            ).map { v -> valueOf(v) }

        private val rectf = rectf()

        private val dither = GfxLib(resourceAccess, gameOptions).dither()

        @TinyCall("Draw a gradient using dithering, only from color c1 to color c2.")
        override fun invoke(
            @TinyArgs(["x", "y", "width", "height", "color1", "color2", "is_horizontal"]) args: Varargs,
        ): Varargs {
            // FIXME:
            TODO()
/*
            if (args.narg() < 6) throw LuaError("Expected 6  args")

            val x = args.checkint(1)
            val y = args.checkint(2)
            val width = args.checkint(3)
            val height = args.checkint(4)

            val color2 = args.arg(6).checkColorIndex()

            val isHorizontal = args.optboolean(7, false)

            // Draw the background color
            rectf.invoke(arrayOf(args.arg(1), args.arg(2), args.arg(3), args.arg(4), args.arg(5)))

            val previous = dither.call()
            dithering.forEachIndexed { index, pattern ->
                if (isHorizontal) {
                    val xx = x + width * index / dithering.size
                    val xx2 = x + width * (index + 1) / dithering.size
                    dither.call(pattern)
                    rectf.invoke(
                        arrayOf(
                            valueOf(xx),
                            valueOf(y),
                            valueOf(xx2 - xx),
                            valueOf(height),
                            valueOf(color2),
                        ),
                    )
                } else {
                    val yy = y + height * index / dithering.size
                    val yy2 = y + height * (index + 1) / dithering.size
                    dither.call(pattern)
                    rectf.invoke(
                        arrayOf(
                            valueOf(x),
                            valueOf(yy),
                            valueOf(width),
                            valueOf(yy2 - yy),
                            valueOf(color2),
                        ),
                    )
                }
            }
            dither.call(previous)

            resourceAccess.addOp(FrameBufferOperation)

            return NIL

 */
        }
    }
}
