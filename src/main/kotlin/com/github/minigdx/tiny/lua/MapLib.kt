package com.github.minigdx.tiny.lua

import com.github.minigdx.tiny.resources.GameScript
import org.luaj.vm2.LuaTable
import org.luaj.vm2.LuaValue
import org.luaj.vm2.Varargs
import org.luaj.vm2.lib.LibFunction
import org.luaj.vm2.lib.TwoArgFunction

class MapLib(private val parent: GameScript) : TwoArgFunction() {
    override fun call(arg1: LuaValue, arg2: LuaValue): LuaValue {
        val map = LuaTable()
        map.set("draw", draw())
        arg2.set("map", map)
        arg2.get("package").get("loaded").set("map", map)
        return map
    }

    inner class draw() : LibFunction() {

        @DocCall(
            documentation = "Draw the default layer on the screen.",
            mainCall = true,
        )
        override fun call(): LuaValue {
            val layer = parent.level?.imageLayers?.get(0)
            if (layer != null) {
                parent.frameBuffer.colorIndexBuffer.copyFrom(
                    source = layer.pixels,
                    dstX = 0,
                    dstY = 0,
                    sourceX = 0,
                    sourceY = 0,
                    width = layer.width,
                    height = layer.height
                )
            }
            return NONE
        }

        @DocCall(
            documentation = "Draw the default layer on the screen at the x/y coordinates.",
        )
        override fun call(@DocArg("x") a: LuaValue, @DocArg("y") b: LuaValue): LuaValue {
            val layer = parent.level?.imageLayers?.get(0)
            if (layer != null) {
                parent.frameBuffer.colorIndexBuffer.copyFrom(
                    source = layer.pixels,
                    dstX = a.checkint(),
                    dstY = b.checkint(),
                    sourceX = 0,
                    sourceY = 0,
                    width = layer.width,
                    height = layer.height
                )
            }
            return NONE
        }

        @DocCall(
            documentation = "Draw the default layer on the screen at the x/y coordinates.",
        )
        override fun call(
            @DocArg("x") a: LuaValue, @DocArg("y") b: LuaValue,
            @DocArg("sx") c: LuaValue, @DocArg("sy") d: LuaValue
        ): LuaValue {
            val layer = parent.level?.imageLayers?.get(0)
            if (layer != null) {
                parent.frameBuffer.colorIndexBuffer.copyFrom(
                    source = layer.pixels,
                    dstX = a.checkint(),
                    dstY = b.checkint(),
                    sourceX = c.checkint(),
                    sourceY = d.checkint(),
                    width = layer.width,
                    height = layer.height
                )
            }
            return NONE
        }

        override fun invoke(
            @DocArgs(
                names = ["x", "y", "sx", "sy", "width", "height"]
            ) args: Varargs
        ): Varargs {
            if (args.narg() < 6) return super.invoke(args)
            val x = args.arg(1).checkint()
            val y = args.arg(2).checkint()
            val sx = args.arg(3).checkint()
            val sy = args.arg(4).checkint()
            val width = args.arg(5).checkint()
            val height = args.arg(6).checkint()

            val layer = parent.level?.imageLayers?.get(0)
            if (layer != null) {
                parent.frameBuffer.colorIndexBuffer.copyFrom(
                    source = layer.pixels,
                    dstX = x,
                    dstY = y,
                    sourceX = sx,
                    sourceY = sy,
                    width = width,
                    height = height
                )
            }
            return NONE
        }

        @DocCall(
            documentation = "Draw the layer on the screen.",
            mainCall = true
        )
        override fun call(
            @DocArg("layer", "index of the layer") a: LuaValue
        ): LuaValue {
            val layer = parent.level?.imageLayers?.getOrNull(a.checkint())
            if (layer != null) {
                parent.frameBuffer.colorIndexBuffer.copyFrom(
                    source = layer.pixels,
                    dstX = 0,
                    dstY = 0,
                    sourceX = 0,
                    sourceY = 0,
                    width = layer.width,
                    height = layer.height
                )
            }
            return NONE
        }
    }

}
