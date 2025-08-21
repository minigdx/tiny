package com.github.minigdx.tiny.lua

import com.github.mingdx.tiny.doc.TinyArg
import com.github.mingdx.tiny.doc.TinyCall
import com.github.mingdx.tiny.doc.TinyFunction
import com.github.mingdx.tiny.doc.TinyLib
import com.github.minigdx.tiny.engine.GameOptions
import com.github.minigdx.tiny.engine.GameResourceAccess2
import com.github.minigdx.tiny.platform.DrawingMode
import com.github.minigdx.tiny.render.VirtualFrameBuffer
import org.luaj.vm2.LuaTable
import org.luaj.vm2.LuaValue
import org.luaj.vm2.lib.LibFunction
import org.luaj.vm2.lib.OneArgFunction
import org.luaj.vm2.lib.ThreeArgFunction
import org.luaj.vm2.lib.TwoArgFunction
import kotlin.math.max
import kotlin.math.min

@TinyLib(
    "gfx",
    "Access to graphical API like updating the color palette or applying a dithering pattern.",
)
class GfxLib(
    private val resourceAccess: GameResourceAccess2,
    private val gameOptions: GameOptions,
    virtualFrameBuffer: VirtualFrameBuffer
) : TwoArgFunction() {
    override fun call(
        arg1: LuaValue,
        arg2: LuaValue,
    ): LuaValue {
        val func = LuaTable()
        func["clip"] = clip()
        func["dither"] = dither()
        func["pal"] = pal()
        func["camera"] = camera()
        func["to_sheet"] = toSheet()
        func["pset"] = pset()
        func["pget"] = pget()
        func["cls"] = cls()
        func["draw_mode"] = drawMode()

        arg2["gfx"] = func
        arg2["package"]["loaded"]["gfx"] = func
        return func
    }

    @TinyFunction(
        """Switch to another draw mode.
        |- 0: default. 
        |- 1: drawing with transparent (ie: can erase part of the screen)
        |- 2: drawing a stencil that will be use with the next mode
        |- 3: drawing using a stencil test (ie: drawing only in the stencil) 
        |- 4: drawing using a stencil test (ie: drawing everywhere except in the stencil) 
    """,
        "draw_mode",
    )
    internal inner class drawMode : LibFunction() {
        private var current: Int = 0

        private val modes = arrayOf(
            DrawingMode.DEFAULT,
            DrawingMode.ALPHA_BLEND,
            DrawingMode.STENCIL_WRITE,
            DrawingMode.STENCIL_TEST,
            DrawingMode.STENCIL_NOT_TEST,
        )

        @TinyCall("Return the actual mode. Switch back to the default mode.")
        override fun call(): LuaValue {
            return valueOf(current).also {
                current = 0
                // FIXME:
                // resourceAccess.addOp(DrawingModeOperation(modes[current]))
            }
        }

        @TinyCall("Switch to another draw mode. Return the previous mode.")
        override fun call(
            @TinyArg("mode") a: LuaValue,
        ): LuaValue {
            val before = current
            val index = a.checkint()
            // Invalid index
            if (index !in (0 until modes.size)) {
                return NIL
            }
            current = index
            val f = modes[current]
            // FIXME:
            // resourceAccess.addOp(DrawingModeOperation(f))

            return valueOf(before)
        }
    }

    @TinyFunction("clear the screen", example = GFX_CLS_EXAMPLE)
    internal inner class cls : OneArgFunction() {
        @TinyCall("Clear the screen with a default color.")
        override fun call(): LuaValue = super.call()

        @TinyCall("Clear the screen with a color.")
        override fun call(
            @TinyArg("color") arg: LuaValue,
        ): LuaValue {
            val color =
                if (arg.isnil()) {
                    valueOf("#000000").checkColorIndex()
                } else {
                    arg.checkColorIndex()
                }
            // FIXME:
            /*
            resourceAccess.frameBuffer.clear(color)
            resourceAccess.addOp(FrameBufferOperation)

             */
            return NIL
        }
    }

    @TinyFunction("Set the color index at the coordinate (x,y).", example = GFX_PSET_EXAMPLE)
    internal inner class pset : ThreeArgFunction() {
        @TinyCall("set the color index at the coordinate (x,y).")
        override fun call(
            @TinyArg("x") arg1: LuaValue,
            @TinyArg("y") arg2: LuaValue,
            @TinyArg("color") arg3: LuaValue,
        ): LuaValue {
            // FIXME:
            /*
            resourceAccess.frameBuffer.pixel(arg1.checkint(), arg2.checkint(), arg3.checkint())
            resourceAccess.addOp(FrameBufferOperation)

             */
            return NIL
        }
    }

    @TinyFunction("Get the color index at the coordinate (x,y).", example = GFX_PGET_EXAMPLE)
    internal inner class pget : TwoArgFunction() {
        @TinyCall("get the color index at the coordinate (x,y).")
        override fun call(
            @TinyArg("x") arg1: LuaValue,
            @TinyArg("y") arg2: LuaValue,
        ): LuaValue {
            val x = min(max(0, arg1.checkint()), gameOptions.width - 1)
            val y = min(max(0, arg2.checkint()), gameOptions.height - 1)

            // val index = resourceAccess.readPixel(x, y)
            // FIXME:
            val index = 0
            return valueOf(index)
        }
    }

    @TinyFunction(
        "Transform the current frame buffer into a spritesheeet. \n\n" +
            "- If the index of the spritesheet already exist, the spritesheet will be replaced\n" +
            "- If the index of the spritesheet doesn't exist, a new spritesheet at this index will be created\n" +
            "- If the index of the spritesheet is negative, " +
            "a new spritesheet will be created at the last positive index.\n",
        "to_sheet",
        example = GFX_TO_SHEET_EXAMPLE,
    )
    inner class toSheet : LibFunction() {
        @TinyCall("Copy the current frame buffer to an new or existing sheet index.")
        override fun call(
            @TinyArg("sheet") a: LuaValue,
        ): LuaValue {
            val (index, name) = getIndexAndName(a)
// FIXME:
            /*
            val frameBuffer = resourceAccess.readFrame()
            val sheet = SpriteSheet(
                0,
                index,
                name,
                ResourceType.GAME_SPRITESHEET,
                frameBuffer.colorIndexBuffer,
                frameBuffer.width,
                frameBuffer.height,
            )

            resourceAccess.spritesheet(sheet)

             */
            return valueOf(index)
        }

        @TinyCall(
            "Create a blank spritesheet. " +
                "Execute the operation from the closure on the blank spritesheet and " +
                "copy it to an new or existing sheet index.",
        )
        override fun call(
            @TinyArg("sheet") a: LuaValue,
            @TinyArg("closure") b: LuaValue,
        ): LuaValue {
            val (index, name) = getIndexAndName(a)
            val closure = b.checkclosure() ?: return call(a)

            // FIXME:
            /*

            val frameBuffer = resourceAccess.renderAsBuffer { closure.invoke() }
            val sheet = SpriteSheet(
                0,
                index,
                name,
                ResourceType.GAME_SPRITESHEET,
                frameBuffer.colorIndexBuffer,
                frameBuffer.width,
                frameBuffer.height,
            )
            resourceAccess.spritesheet(sheet)
             */
            return valueOf(index)
        }

        private fun getIndexAndName(arg: LuaValue): Pair<Int, String> {
            /*
            return if (arg.isstring()) {
                val index = resourceAccess.spritesheet(arg.tojstring()) ?: resourceAccess.newSpritesheetIndex()
                index to arg.tojstring()
            } else {
                val spriteSheet = resourceAccess.spritesheet(arg.checkint())
                arg.toint() to (spriteSheet?.name ?: "frame_buffer_${arg.toint()}")
            }

             */
            // FIXME:
            TODO()
        }
    }

    @TinyFunction(
        "Change a color from the palette to another color.",
        example = GFX_PAL_EXAMPLE,
    )
    inner class pal : LibFunction() {
        @TinyCall("Reset all previous color changes.")
        override fun call(): LuaValue {
            /*
            resourceAccess.addOp(PaletteOperation)
            resourceAccess.frameBuffer.blender.pal()

             */
            // FIXME:
            return NONE
        }

        @TinyCall("Replace the color a for the color b.")
        override fun call(
            a: LuaValue,
            b: LuaValue,
        ): LuaValue {
            // FIXME:
            /*
            resourceAccess.addOp(PaletteOperation)
            resourceAccess.frameBuffer.blender.pal(a.checkint(), b.checkint())

             */
            return NONE
        }
    }

    @TinyFunction("Move the game camera.", example = GFX_CAMERA_EXAMPLE)
    inner class camera : TwoArgFunction() {
        @TinyCall("Reset the game camera to it's default position (0,0).")
        override fun call(): LuaValue {
            /*
            resourceAccess.addOp(CameraOperation)
            val previous = coordinates()
            resourceAccess.frameBuffer.camera.set(0, 0)

             */
            // FIXME:
            val previous = coordinates()
            return previous
        }

        @TinyCall("Set game camera to the position x, y.")
        override fun call(
            @TinyArg("x") arg1: LuaValue,
            @TinyArg("y") arg2: LuaValue,
        ): LuaValue {
            /*
            resourceAccess.addOp(CameraOperation)
            val previous = coordinates()
            resourceAccess.frameBuffer.camera.set(arg1.toint(), arg2.toint())

             */
            // FIXME:
            val previous = coordinates()
            return previous
        }

        private fun coordinates(): LuaTable {
            return LuaTable().apply {
                // FIXME:
                // set("x", resourceAccess.frameBuffer.camera.x)
                // set("y", resourceAccess.frameBuffer.camera.y)
            }
        }
    }

    @TinyFunction(
        "Apply a dithering pattern on every new draw call. " +
            "The pattern is using the bits value of a 2 octet value. " +
            "The first bits is the one on the far left and represent " +
            "the pixel of the top left of a 4x4 matrix. " +
            "The last bit is the pixel from the bottom right of this matrix.",
        example = GFX_DITHER_EXAMPLE,
    )
    inner class dither : LibFunction() {
        @TinyCall("Reset dithering pattern. The previous dithering pattern is returned.")
        override fun call(): LuaValue {
            /*

            resourceAccess.addOp(DitheringOperation)
            return valueOf(resourceAccess.frameBuffer.blender.dither(0xFFFF))
             */
            // FIXME:
            TODO()
        }

        @TinyCall("Apply dithering pattern. The previous dithering pattern is returned.")
        override fun call(
            @TinyArg("pattern", "Dither pattern. For example: 0xA5A5 or 0x3030") a: LuaValue,
        ): LuaValue {
            /*
            resourceAccess.addOp(DitheringOperation)
            return valueOf(resourceAccess.frameBuffer.blender.dither(a.checkint()))

             */
            // FIXME:
            TODO()
        }
    }

    @TinyFunction(
        "Clip the draw surface (ie: limit the drawing area).",
        example = GFX_CLIP_EXAMPLE,
    )
    inner class clip : LibFunction() {
        @TinyCall("Reset the clip and draw on the fullscreen.")
        override fun call(): LuaValue {
            /*
            resourceAccess.addOp(ClipOperation)
            resourceAccess.frameBuffer.clipper.reset()

             */
            // FIXME:
            return NONE
        }

        @TinyCall("Clip and limit the drawing area.")
        override fun call(
            @TinyArg("x") a: LuaValue,
            @TinyArg("y") b: LuaValue,
            @TinyArg("width") c: LuaValue,
            @TinyArg("height") d: LuaValue,
        ): LuaValue {
            /*

            resourceAccess.addOp(ClipOperation)
            resourceAccess.frameBuffer.clipper.set(a.checkint(), b.checkint(), c.checkint(), d.checkint())
             */
            // FIXME:
            return NONE
        }
    }

    private fun LuaValue.checkColorIndex(): Int {
        return if (this.isnumber()) {
            this.checkint()
        } else {
            // resourceAccess.frameBuffer.gamePalette.getColorIndex(this.checkjstring()!!)
            // FIXME:
            return 0
        }
    }
}
