package com.github.minigdx.tiny.lua

import com.github.mingdx.tiny.doc.TinyArg
import com.github.mingdx.tiny.doc.TinyArgs
import com.github.mingdx.tiny.doc.TinyCall
import com.github.mingdx.tiny.doc.TinyFunction
import com.github.mingdx.tiny.doc.TinyLib
import com.github.minigdx.tiny.engine.GameResourceAccess
import com.github.minigdx.tiny.resources.LdtkEntity
import org.luaj.vm2.LuaTable
import org.luaj.vm2.LuaValue
import org.luaj.vm2.Varargs
import org.luaj.vm2.lib.LibFunction
import org.luaj.vm2.lib.TwoArgFunction

@TinyLib(
    "map",
    "Map API to accessing maps data configured in a game."
)
class MapLib(private val resourceAccess: GameResourceAccess) : TwoArgFunction() {

    private var currentLevel: Int = 0

    override fun call(arg1: LuaValue, arg2: LuaValue): LuaValue {
        val map = LuaTable()
        map.set("draw", draw())
        map.set("entity", entity())
        map.set("flag", flag())
        map.set("from", from())
        arg2.set("map", map)
        arg2.get("package").get("loaded").set("map", map)
        return map
    }

    // convert screen coordinate into map coordinate
    inner class from : TwoArgFunction() {
        override fun call(arg1: LuaValue, arg2: LuaValue): LuaValue {
            TODO()
        }
    }

    @TinyFunction("Get the flag from a tile.")
    inner class flag : LibFunction() {

        @TinyCall("Get the flag from the tile at the coordinate x,y.")
        override fun call(@TinyArg("x") a: LuaValue, @TinyArg("y") b: LuaValue): LuaValue {
            val tileX = a.checkint()
            val tileY = b.checkint()

            val layer = resourceAccess.level(currentLevel)?.intLayers?.first { l -> l != null } ?: return NONE

            return if (tileX in 0 until layer.width && tileY in 0 until layer.height) {
                valueOf(layer.ints.getOne(tileX, tileY))
            } else {
                NONE
            }
        }
    }

    @TinyFunction("Get all entities from a type.")
    inner class entity : LuaTable() {

        override fun get(key: LuaValue): LuaValue {
            val strKey = key.checkjstring() ?: return NIL
            // TODO: mettre un système de cache pour ne pas iter à chaque fois sur la liste.
            val entities = resourceAccess.level(currentLevel)?.entities?.get(strKey) ?: return NIL
            val first = LuaTable()
            entities.forEach {
                first[it.iid] = it.toLuaTable()
            }

            return first
        }

        private fun LdtkEntity.toLuaTable(): LuaTable {
            val table = LuaTable()
            table["x"] = valueOf(this.x)
            table["y"] = valueOf(this.y)
            table["id"] = valueOf(id)
            table["iid"] = valueOf(iid)
            table["layer"] = valueOf(layer)
            table["width"] = valueOf(width)
            table["height"] = valueOf(height)
            table["color"] = valueOf(color)
            table["customFields"] = customFields.let {
                val fields = LuaTable()
                it.forEach { (key, value) ->
                    fields[key] = valueOf(value)
                }
                fields
            }
            return table
        }
    }

    @TinyFunction("Draw map tiles on the screen.")
    inner class draw : LibFunction() {

        @TinyCall(
            description = "Draw the default layer on the screen.",
        )
        override fun call(): LuaValue {
            val layer = resourceAccess.level(currentLevel)?.imageLayers?.get(0)
            if (layer != null) {
                resourceAccess.frameBuffer.copyFrom(
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

        @TinyCall(
            description = "Draw the default layer on the screen at the x/y coordinates.",
        )
        override fun call(@TinyArg("x") a: LuaValue, @TinyArg("y") b: LuaValue): LuaValue {
            val layer = resourceAccess.level(currentLevel)?.imageLayers?.get(0)
            if (layer != null) {
                resourceAccess.frameBuffer.copyFrom(
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

        @TinyCall(
            description = "Draw the default layer on the screen at the x/y coordinates.",
        )
        override fun call(
            @TinyArg("x") a: LuaValue,
            @TinyArg("y") b: LuaValue,
            @TinyArg("sx") c: LuaValue,
            @TinyArg("sy") d: LuaValue
        ): LuaValue {
            val layer = resourceAccess.level(currentLevel)?.imageLayers?.get(0)
            if (layer != null) {
                resourceAccess.frameBuffer.copyFrom(
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
            @TinyArgs(
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

            val layer = resourceAccess.level(currentLevel)?.imageLayers?.get(0)
            if (layer != null) {
                resourceAccess.frameBuffer.copyFrom(
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

        @TinyCall(
            description = "Draw the layer on the screen.",
            mainCall = true
        )
        override fun call(
            @TinyArg("layer", "index of the layer") a: LuaValue
        ): LuaValue {
            val layer = resourceAccess.level(currentLevel)?.imageLayers?.getOrNull(a.checkint())
            if (layer != null) {
                resourceAccess.frameBuffer.copyFrom(
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
