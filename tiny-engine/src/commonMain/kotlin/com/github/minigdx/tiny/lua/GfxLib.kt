package com.github.minigdx.tiny.lua

import com.github.mingdx.tiny.doc.TinyArg
import com.github.mingdx.tiny.doc.TinyCall
import com.github.mingdx.tiny.doc.TinyFunction
import com.github.mingdx.tiny.doc.TinyLib
import com.github.minigdx.tiny.engine.GameResourceAccess
import com.github.minigdx.tiny.graphic.PixelArray
import com.github.minigdx.tiny.resources.ResourceType
import com.github.minigdx.tiny.resources.SpriteSheet
import org.luaj.vm2.LuaTable
import org.luaj.vm2.LuaValue
import org.luaj.vm2.lib.LibFunction
import org.luaj.vm2.lib.OneArgFunction
import org.luaj.vm2.lib.ThreeArgFunction
import org.luaj.vm2.lib.TwoArgFunction

@TinyLib(
    "gfx",
    "Access to graphical API like updating the color palette or applying a dithering pattern.",
)
class GfxLib(private val resourceAccess: GameResourceAccess) : TwoArgFunction() {
    override fun call(arg1: LuaValue, arg2: LuaValue): LuaValue {
        val func = LuaTable()
        func["clip"] = clip()
        func["dither"] = dither()
        func["pal"] = pal()
        func["camera"] = camera()
        func["to_sheet"] = toSheet()
        func["pset"] = pset()
        func["pget"] = pget()
        func["cls"] = cls()

        arg2["gfx"] = func
        arg2["package"]["loaded"]["gfx"] = func
        return func
    }

    @TinyFunction("clear the screen")
    internal inner class cls : OneArgFunction() {
        @TinyCall("Clear the screen with a default color.")
        override fun call(): LuaValue = super.call()

        @TinyCall("Clear the screen with a color.")
        override fun call(@TinyArg("color") arg: LuaValue): LuaValue {
            val color = if (arg.isnil()) {
                valueOf("#000000").checkColorIndex()
            } else {
                arg.checkColorIndex()
            }
            resourceAccess.frameBuffer.clear(color)
            return NONE
        }
    }

    @TinyFunction("Set the color index at the coordinate (x,y).")
    internal inner class pset : ThreeArgFunction() {
        @TinyCall("set the color index at the coordinate (x,y).")
        override fun call(@TinyArg("x")arg1: LuaValue, @TinyArg("y")arg2: LuaValue, @TinyArg("color")arg3: LuaValue): LuaValue {
            resourceAccess.frameBuffer.pixel(arg1.checkint(), arg2.checkint(), arg3.checkint())
            return NONE
        }
    }

    @TinyFunction("Get the color index at the coordinate (x,y).")
    internal inner class pget : TwoArgFunction() {
        @TinyCall("get the color index at the coordinate (x,y).")
        override fun call(@TinyArg("x")arg1: LuaValue, @TinyArg("y")arg2: LuaValue): LuaValue {
            val index = resourceAccess.frameBuffer.pixel(arg1.checkint(), arg2.checkint())
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
    inner class toSheet : OneArgFunction() {

        @TinyCall("Copy the current frame buffer to an new or existing sheet index.")
        override fun call(@TinyArg("sheet") arg: LuaValue): LuaValue {
            val frameBuffer = resourceAccess.frameBuffer
            val copy = PixelArray(frameBuffer.width, frameBuffer.height).apply {
                copyFrom(frameBuffer.colorIndexBuffer) { index, _, _ -> index }
            }
            val sheet = SpriteSheet(
                0,
                arg.checkint(),
                "frame_buffer",
                ResourceType.GAME_SPRITESHEET,
                copy,
                copy.width,
                copy.height,
            )
            resourceAccess.spritesheet(sheet)
            return arg
        }
    }

    @TinyFunction(
        "Change a color from the palette to another color.",
        example = GFX_PAL_EXAMPLE,
    )
    inner class pal : LibFunction() {

        @TinyCall("Reset all previous color changes.")
        override fun call(): LuaValue {
            resourceAccess.frameBuffer.blender.pal()
            return NONE
        }

        @TinyCall("Replace the color a for the color b.")
        override fun call(a: LuaValue, b: LuaValue): LuaValue {
            resourceAccess.frameBuffer.blender.pal(a.checkint(), b.checkint())
            return NONE
        }
    }

    @TinyFunction("Move the game camera.")
    inner class camera : TwoArgFunction() {

        @TinyCall("Reset the game camera to it's default position (0,0).")
        override fun call(): LuaValue {
            resourceAccess.frameBuffer.camera.set(0, 0)
            return NONE
        }

        @TinyCall("Set game camera to the position x, y.")
        override fun call(@TinyArg("x") arg1: LuaValue, @TinyArg("y") arg2: LuaValue): LuaValue {
            resourceAccess.frameBuffer.camera.set(arg1.toint(), arg2.toint())
            return NONE
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
            return valueOf(resourceAccess.frameBuffer.blender.dither(0xFFFF))
        }

        @TinyCall("Apply dithering pattern. The previous dithering pattern is returned.")
        override fun call(@TinyArg("pattern") a: LuaValue): LuaValue {
            return valueOf(resourceAccess.frameBuffer.blender.dither(a.checkint()))
        }
    }

    @TinyFunction(
        "Clip the draw surface (ie: limit the drawing area).",
        example = GFX_CLIP_EXAMPLE,
    )
    inner class clip : LibFunction() {
        @TinyCall("Reset the clip and draw on the fullscreen.")
        override fun call(): LuaValue {
            resourceAccess.frameBuffer.clipper.reset()
            return NONE
        }

        @TinyCall("Clip and limit the drawing area.")
        override fun call(
            @TinyArg("x") a: LuaValue,
            @TinyArg("y") b: LuaValue,
            @TinyArg("width") c: LuaValue,
            @TinyArg("height") d: LuaValue,
        ): LuaValue {
            resourceAccess.frameBuffer.clipper.set(a.checkint(), b.checkint(), c.checkint(), d.checkint())
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
}
