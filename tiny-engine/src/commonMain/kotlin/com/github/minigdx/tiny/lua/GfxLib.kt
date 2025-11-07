package com.github.minigdx.tiny.lua

import com.github.mingdx.tiny.doc.LuaType
import com.github.mingdx.tiny.doc.TinyArg
import com.github.mingdx.tiny.doc.TinyCall
import com.github.mingdx.tiny.doc.TinyFunction
import com.github.mingdx.tiny.doc.TinyLib
import com.github.minigdx.tiny.engine.GameOptions
import com.github.minigdx.tiny.engine.GameResourceAccess
import com.github.minigdx.tiny.graphic.PixelArray
import com.github.minigdx.tiny.platform.DrawingMode
import com.github.minigdx.tiny.render.VirtualFrameBuffer
import com.github.minigdx.tiny.resources.ResourceType
import com.github.minigdx.tiny.resources.SpriteSheet
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
    private val resourceAccess: GameResourceAccess,
    private val gameOptions: GameOptions,
    private val virtualFrameBuffer: VirtualFrameBuffer,
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
        "Switch to another draw mode. \n\n" +
            "- 0: default.\n " +
            "- 1: drawing with transparent (ie: can erase part of the screen)\n  " +
            "- 2: drawing a stencil that will be use with the next mode\n  " +
            "- 3: drawing using a stencil test (ie: drawing only in the stencil)\n  " +
            "- 4: drawing using a stencil test (ie: drawing everywhere except in the stencil)\n",
        "draw_mode",
        GFX_DRAW_MODE_EXAMPLE,
        spritePath = "resources/tiny-town.png",
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
                virtualFrameBuffer.setDrawMode(DrawingMode.DEFAULT)
            }
        }

        @TinyCall("Switch to another draw mode. Return the previous mode.")
        override fun call(
            @TinyArg("mode", type = LuaType.NUMBER) a: LuaValue,
        ): LuaValue {
            val before = current
            val index = a.checkint()
            // Invalid index
            if (index !in (0 until modes.size)) {
                return NIL
            }
            current = index
            val f = modes[current]

            virtualFrameBuffer.setDrawMode(f)

            return valueOf(before)
        }
    }

    @TinyFunction("clear the screen", example = GFX_CLS_EXAMPLE)
    internal inner class cls : OneArgFunction() {
        @TinyCall("Clear the screen with a default color.")
        override fun call(): LuaValue = super.call()

        @TinyCall("Clear the screen with a color.")
        override fun call(
            @TinyArg("color", type = LuaType.NUMBER) arg: LuaValue,
        ): LuaValue {
            val color = if (arg.isnil()) {
                valueOf("#000000").checkColorIndex()
            } else {
                arg.checkColorIndex()
            }

            virtualFrameBuffer.clear(color)

            return NIL
        }
    }

    @TinyFunction("Set the color index at the coordinate (x,y).", example = GFX_PSET_EXAMPLE)
    internal inner class pset : ThreeArgFunction() {
        @TinyCall("set the color index at the coordinate (x,y).")
        override fun call(
            @TinyArg("x", type = LuaType.NUMBER) arg1: LuaValue,
            @TinyArg("y", type = LuaType.NUMBER) arg2: LuaValue,
            @TinyArg("color", type = LuaType.NUMBER) arg3: LuaValue,
        ): LuaValue {
            virtualFrameBuffer.drawPoint(
                arg1.toint(),
                arg2.toint(),
                arg3.checkColorIndex(),
            )
            return NIL
        }
    }

    @TinyFunction("Get the color index at the coordinate (x,y).", example = GFX_PGET_EXAMPLE)
    internal inner class pget : TwoArgFunction() {
        @TinyCall("get the color index at the coordinate (x,y).")
        override fun call(
            @TinyArg("x", type = LuaType.NUMBER) arg1: LuaValue,
            @TinyArg("y", type = LuaType.NUMBER) arg2: LuaValue,
        ): LuaValue {
            val x = min(max(0, arg1.checkint()), gameOptions.width - 1)
            val y = min(max(0, arg2.checkint()), gameOptions.height - 1)

            val frame = virtualFrameBuffer.readFrameBuffer()

            val index = frame.getPixel(x, y)
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
            @TinyArg("sheet", type = LuaType.ANY) a: LuaValue,
        ): LuaValue {
            val (index, name) = getIndexAndName(a)

            val sprite = SpriteSheet(
                0,
                index,
                name,
                ResourceType.GAME_SPRITESHEET,
                PixelArray(gameOptions.width, gameOptions.height),
                gameOptions.width,
                gameOptions.height,
            )

            val frameBuffer = virtualFrameBuffer.readFrameBuffer()

            frameBuffer.copyInto(sprite.pixels)

            resourceAccess.saveSpritesheet(sprite)

            return valueOf(index)
        }

        private fun getIndexAndName(arg: LuaValue): Pair<Int, String> {
            return if (arg.isstring()) {
                val name = arg.tojstring()
                val existing = resourceAccess.findSpritesheet(name)
                val index = existing?.index ?: resourceAccess.newSpritesheetIndex()
                index to name
            } else {
                val index = arg.toint()
                val spriteSheet = resourceAccess.findSpritesheet(index)
                index to (spriteSheet?.name ?: "frame_buffer_$index")
            }
        }
    }

    @TinyFunction(
        "Change a color from the palette to another color.",
        example = GFX_PAL_EXAMPLE,
    )
    inner class pal : LibFunction() {
        @TinyCall("Reset all previous color changes.")
        override fun call(): LuaValue {
            virtualFrameBuffer.resetPalette()
            return NONE
        }

        @TinyCall("Replace the color a for the color b.")
        override fun call(
            a: LuaValue,
            b: LuaValue,
        ): LuaValue {
            virtualFrameBuffer.swapPalette(a.checkint(), b.checkint())
            return NONE
        }
    }

    @TinyFunction("Move the game camera.", example = GFX_CAMERA_EXAMPLE)
    inner class camera : TwoArgFunction() {
        @TinyCall("Reset the game camera to it's default position (0,0).")
        override fun call(): LuaValue {
            val previous = coordinates()
            virtualFrameBuffer.resetCamera()
            return previous
        }

        @TinyCall("Set game camera to the position x, y.")
        override fun call(
            @TinyArg("x", type = LuaType.NUMBER) arg1: LuaValue,
            @TinyArg("y", type = LuaType.NUMBER) arg2: LuaValue,
        ): LuaValue {
            val previous = coordinates()
            virtualFrameBuffer.setCamera(arg1.toint(), arg2.toint())
            return previous
        }

        private fun coordinates(): LuaTable {
            val (x, y) = virtualFrameBuffer.getCamera()
            return LuaTable().apply {
                set("x", x)
                set("y", y)
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
            return valueOf(virtualFrameBuffer.dithering(0xFFFF))
        }

        @TinyCall("Apply dithering pattern. The previous dithering pattern is returned.")
        override fun call(
            @TinyArg("pattern", "Dither pattern. For example: 0xA5A5 or 0x3030", type = LuaType.NUMBER) a: LuaValue,
        ): LuaValue {
            val actual = virtualFrameBuffer.dithering(a.checkint())
            return valueOf(actual)
        }
    }

    @TinyFunction(
        "Clip the draw surface (ie: limit the drawing area).",
        example = GFX_CLIP_EXAMPLE,
    )
    inner class clip : LibFunction() {
        @TinyCall("Reset the clip and draw on the fullscreen.")
        override fun call(): LuaValue {
            virtualFrameBuffer.resetClip()
            return NONE
        }

        @TinyCall("Clip and limit the drawing area.")
        override fun call(
            @TinyArg("x", type = LuaType.NUMBER) a: LuaValue,
            @TinyArg("y", type = LuaType.NUMBER) b: LuaValue,
            @TinyArg("width", type = LuaType.NUMBER) c: LuaValue,
            @TinyArg("height", type = LuaType.NUMBER) d: LuaValue,
        ): LuaValue {
            virtualFrameBuffer.setClip(a.checkint(), b.checkint(), c.checkint(), d.checkint())
            return NONE
        }
    }

    private fun LuaValue.checkColorIndex(): Int {
        return if (this.isnumber()) {
            this.checkint()
        } else {
            return gameOptions.colors().getColorIndex(this.checkjstring()!!)
        }
    }
}
