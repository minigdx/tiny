package com.github.minigdx.tiny.lua

import com.github.mingdx.tiny.doc.TinyArg
import com.github.mingdx.tiny.doc.TinyArgs
import com.github.mingdx.tiny.doc.TinyCall
import com.github.mingdx.tiny.doc.TinyFunction
import com.github.mingdx.tiny.doc.TinyLib
import com.github.minigdx.tiny.engine.GameOptions
import com.github.minigdx.tiny.engine.GameResourceAccess
import com.github.minigdx.tiny.graphic.PixelArray
import org.luaj.vm2.LuaTable
import org.luaj.vm2.LuaValue
import org.luaj.vm2.Varargs
import org.luaj.vm2.lib.LibFunction
import org.luaj.vm2.lib.OneArgFunction
import org.luaj.vm2.lib.ThreeArgFunction
import org.luaj.vm2.lib.TwoArgFunction

@TinyLib("spr", "Sprite API to draw or update sprites.")
class SprLib(val gameOptions: GameOptions, val resourceAccess: GameResourceAccess) : TwoArgFunction() {

    private var currentSpritesheet: Int = 0

    override fun call(arg1: LuaValue, arg2: LuaValue): LuaValue {
        val sprTable = LuaTable()
        sprTable["sdraw"] = sdraw()
        sprTable["draw"] = draw()
        sprTable["pset"] = pset()
        sprTable["pget"] = pget()
        sprTable["sheet"] = sheet()
        arg2.set("spr", sprTable)
        arg2.get("package").get("loaded").set("spr", sprTable)
        return sprTable
    }

    @TinyFunction(
        "Get the color index at the coordinate (x,y) from the current spritesheet.",
        example = SPR_PGET_EXAMPLE,
        spritePath = "resources/tiny-town.png",
    )
    private inner class pget : TwoArgFunction() {
        @TinyCall("get the color index at the coordinate (x,y) from the current spritesheet.")
        override fun call(@TinyArg("x") arg1: LuaValue, @TinyArg("y") arg2: LuaValue): LuaValue {
            val pixelArray = resourceAccess.spritesheet(currentSpritesheet)?.pixels ?: return NIL

            val x = arg1.checkint()
            val y = arg2.checkint()

            if (isInPixelArray(pixelArray, x, y)) {
                val index = pixelArray.get(x, y)
                val colorIndex = index.get(0)
                return valueOf(colorIndex.toInt())
            } else {
                return NIL
            }
        }
    }

    private fun isInPixelArray(pixelArray: PixelArray, x: Int, y: Int): Boolean {
        return x in 0 until pixelArray.width && y in 0 until pixelArray.height
    }

    @TinyFunction(
        "Set the color index at the coordinate (x,y) in the current spritesheet.",
        example = SPR_PSET_EXAMPLE,
        spritePath = "resources/tiny-dungeon.png",
    )
    private inner class pset : ThreeArgFunction() {
        @TinyCall("Set the color index at the coordinate (x,y) in the current spritesheet.")
        override fun call(
            @TinyArg("x") arg1: LuaValue,
            @TinyArg("y") arg2: LuaValue,
            @TinyArg("color") arg3: LuaValue,
        ): LuaValue {
            val x = arg1.checkint()
            val y = arg2.checkint()
            val pixels = resourceAccess.spritesheet(currentSpritesheet)?.pixels ?: return NIL
            return if (isInPixelArray(pixels, x, y)) {
                pixels.set(x, y, arg3.checkint())
                arg3
            } else {
                NIL
            }
        }
    }

    @TinyFunction(
        "Switch to another spritesheet. " +
            "The index of the spritesheet is given by it's position in the spritesheets field from the `_tiny.json` file." +
            "The first spritesheet is at the index 0. It retuns the previous spritesheet.",
    )
    internal inner class sheet : OneArgFunction() {

        @TinyCall("Switch to the first spritesheet")
        override fun call(): LuaValue = super.call()

        @TinyCall("Switch to the N spritesheet")
        override fun call(@TinyArg("spritesheetN") arg: LuaValue): LuaValue {
            val previousSpriteSheet = currentSpritesheet
            currentSpritesheet = if (arg.isnil()) {
                0
            } else {
                arg.checkint()
            }
            return valueOf(previousSpriteSheet)
        }
    }

    @TinyFunction(
        "S(uper) Draw a fragment from the spritesheet.",
        example = SPR_PGET_EXAMPLE,
        spritePath = "resources/tiny-town.png",
    )
    internal inner class sdraw : LibFunction() {
        @TinyCall("Draw the full spritesheet at default coordinate (0, 0)")
        override fun call(): LuaValue {
            return invoke(varargsOf(arrayOf(NIL, NIL, NIL, NIL, NIL, NIL, NIL, NIL))).arg1()
        }

        @TinyCall("Draw the full spritesheet at coordinate (x, y)")
        override fun call(@TinyArg("x") a: LuaValue, @TinyArg("y") b: LuaValue): LuaValue {
            return invoke(varargsOf(arrayOf(a, b, NIL, NIL, NIL, NIL, NIL, NIL))).arg1()
        }

        @TinyCall("Draw the full spritesheet at coordinate (x, y) from the sprite (sprX, sprY)")
        override fun call(
            @TinyArg("x") a: LuaValue,
            @TinyArg("y") b: LuaValue,
            @TinyArg("sprX") c: LuaValue,
            @TinyArg("sprY") d: LuaValue,
        ): LuaValue {
            return invoke(varargsOf(arrayOf(a, b, c, d, NIL, NIL, NIL, NIL))).arg1()
        }

        @TinyCall("Draw a fragment from the spritesheet at the coordinate (x, y) from the sprite (sprX, sprY) with the width and height.")
        override fun invoke(
            @TinyArgs(
                arrayOf(
                    "x",
                    "y",
                    "sprX",
                    "sprY",
                    "width",
                    "height",
                    "flipX",
                    "flipY",
                ),
                documentations = arrayOf(
                    "screen x coordinate to draw the sprite (default 0)",
                    "screen y coordinate to draw the sprite (default 0)",
                    "x coordinate from the spritesheet (default 0)",
                    "y coordinate from the spritesheet (default 0)",
                    "width of the spritesheet to copy (default width of the spritesheet)",
                    "height of the spritesheet to copy (default height of the spritesheet)",
                    "flip on the x axis (default: false)",
                    "flip on the y axis (default: false)",
                ),
            ) args: Varargs,
        ): Varargs {
            val spritesheet = resourceAccess.spritesheet(currentSpritesheet) ?: return NONE

            val x = args.arg(1).optint(0)
            val y = args.arg(2).optint(0)
            val sprX = args.arg(3).optint(0)
            val sprY = args.arg(4).optint(0)
            val sprWidth = args.arg(5).optint(spritesheet.width)
            val sprHeight = args.arg(6).optint(spritesheet.height)
            val flipX = args.arg(7).optboolean(false)
            val flipY = args.arg(8).optboolean(false)

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

    @TinyFunction("Draw a sprite.", example = SPR_DRAW_EXAMPLE, spritePath = "resources/tiny-town.png")
    internal inner class draw : LibFunction() {

        @TinyCall("Draw a sprite at the default coordinate (0, 0).")
        override fun call(
            @TinyArg("sprN") a: LuaValue,
        ): LuaValue = super.call(a)

        @TinyCall("Draw a sprite.")
        override fun call(
            @TinyArg("sprN") a: LuaValue,
            @TinyArg("x") b: LuaValue,
            @TinyArg("y") c: LuaValue,
        ): LuaValue = super.call(a, b, c)

        @TinyCall("Draw a sprite and allow flip on x or y axis.")
        override fun invoke(@TinyArgs(["sprN", "x", "y", "flipX", "flipY"]) args: Varargs): Varargs {
            if (args.narg() < 1) return NIL
            val sprN = args.arg(1).checkint()
            val x = args.arg(2).optint(0)
            val y = args.arg(3).optint(0)
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
                reverseY = flipY,
            )

            return NIL
        }
    }
}
