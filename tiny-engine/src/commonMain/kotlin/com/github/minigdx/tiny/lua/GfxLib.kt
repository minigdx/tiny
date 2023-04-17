package com.github.minigdx.tiny.lua

import com.github.mingdx.tiny.doc.TinyArg
import com.github.mingdx.tiny.doc.TinyCall
import com.github.mingdx.tiny.doc.TinyFunction
import com.github.mingdx.tiny.doc.TinyLib
import com.github.minigdx.tiny.engine.GameResourceAccess
import org.luaj.vm2.LuaTable
import org.luaj.vm2.LuaValue
import org.luaj.vm2.lib.LibFunction
import org.luaj.vm2.lib.TwoArgFunction

@TinyLib("gfx")
class GfxLib(private val resourceAccess: GameResourceAccess) : TwoArgFunction() {
    override fun call(arg1: LuaValue, arg2: LuaValue): LuaValue {
        val func = LuaTable()
        func.set("clip", clip())
        func.set("dither", dither())
        func.set("pal", pal())
        arg2.set("gfx", func)
        arg2.get("package").get("loaded").set("gfx", func)
        return func
    }

    @TinyFunction(
        "Change a color from the palette to another color.",
        example = GFX_PAL_EXAMPLE
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

    @TinyFunction(
        "Apply a dithering pattern on every new draw call. " +
            "The pattern is using the bits value of a 2 octet value. " +
            "The first bits is the one on the far left and represent " +
            "the pixel of the top left of a 4x4 matrix. " +
            "The last bit is the pixel from the bottom right of this matrix.",
        example = GFX_DITHER_EXAMPLE
    )
    inner class dither : LibFunction() {
        @TinyCall("Reset dithering pattern.")
        override fun call(): LuaValue {
            resourceAccess.frameBuffer.blender.dither(0xFFFF)
            return NONE
        }

        @TinyCall("Apply dithering pattern.")
        override fun call(@TinyArg("pattern") a: LuaValue): LuaValue {
            resourceAccess.frameBuffer.blender.dither(a.checkint())
            return NONE
        }
    }

    @TinyFunction(
        "Clip the draw surface (ie: limit the drawing area).",
        example = GFX_CLIP_EXAMPLE
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
            @TinyArg("height") d: LuaValue
        ): LuaValue {
            resourceAccess.frameBuffer.clipper.set(a.checkint(), b.checkint(), c.checkint(), d.checkint())
            return NONE
        }
    }
}
