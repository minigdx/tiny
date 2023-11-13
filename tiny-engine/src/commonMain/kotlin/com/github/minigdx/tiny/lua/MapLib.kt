package com.github.minigdx.tiny.lua

import com.github.mingdx.tiny.doc.TinyArg
import com.github.mingdx.tiny.doc.TinyArgs
import com.github.mingdx.tiny.doc.TinyCall
import com.github.mingdx.tiny.doc.TinyFunction
import com.github.mingdx.tiny.doc.TinyLib
import com.github.minigdx.tiny.engine.GameResourceAccess
import com.github.minigdx.tiny.resources.LdtkEntity
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.doubleOrNull
import kotlinx.serialization.json.intOrNull
import org.luaj.vm2.LuaTable
import org.luaj.vm2.LuaValue
import org.luaj.vm2.Varargs
import org.luaj.vm2.lib.LibFunction
import org.luaj.vm2.lib.OneArgFunction
import org.luaj.vm2.lib.TwoArgFunction
import kotlin.math.max
import kotlin.math.min

@TinyLib(
    "map",
    "Map API to accessing maps data configured in a game. " +
        "Map can be created using LDTk ( https://ldtk.io/ ). \n\n" +
        "WARNING: Projects need to be exported using " +
        "https://ldtk.io/docs/game-dev/super-simple-export/['Super simple export']",
)
class MapLib(private val resourceAccess: GameResourceAccess) : TwoArgFunction() {

    private var currentLevel: Int = 0

    private var currentLayer: Int = 0

    override fun call(arg1: LuaValue, arg2: LuaValue): LuaValue {
        val map = LuaTable()
        map["draw"] = draw()
        map["layer"] = layer()
        map["entities"] = entities()
        map["flag"] = flag()
        map["from"] = from()
        arg2["map"] = map
        arg2["package"]["loaded"]["map"] = map
        return map
    }

    @TinyFunction("Set the current layer to draw.")
    inner class layer : OneArgFunction() {
        @TinyCall("Set the current index layer to draw. Return the previous layer index.")
        override fun call(@TinyArg("layer_index") arg: LuaValue): LuaValue {
            val prec = currentLayer
            currentLayer = if (arg.isnil()) {
                0
            } else {
                val nbLayers = resourceAccess.level(currentLevel)?.numberOfLayers ?: 1
                min(max(0, arg.checkint()), nbLayers - 1)
            }

            return valueOf(prec)
        }

        @TinyCall("Reset the current layer to draw to the first available layer (index 0).")
        override fun call(): LuaValue = super.call()
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

    @TinyFunction("Table with all entities by type (ie: `map.entities[\"player\"]`).")
    inner class entities : LuaTable() {

        private val cachedEntities: MutableMap<Int, LuaValue> = mutableMapOf()

        private val entities: LuaValue
            get() {
                return cachedEntities[currentLevel] ?: cacheMe(resourceAccess.level(currentLevel)?.entities)
            }

        private fun cacheMe(entities: Map<String, List<LdtkEntity>>?): LuaValue {
            // Transform the list of entities into a table in Lua.
            val toCache = LuaTable()
            entities?.forEach { (key, v) ->
                val entitiesOfType = LuaTable()
                v.forEach {
                    entitiesOfType[it.iid] = it.toLuaTable()
                }
                toCache[key] = entitiesOfType
            }

            cachedEntities[currentLevel] = toCache
            return toCache
        }

        override fun get(key: LuaValue): LuaValue {
            if (key.isnil()) {
                return entities
            }
            val strKey = key.checkjstring() ?: return NIL

            return this.entities[strKey]
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
            table["customFields"] = customFields.toLua()
            return table
        }

        private fun JsonElement.toLua(): LuaValue {
            return when (this) {
                is JsonArray -> this.toLua()
                is JsonObject -> this.toLua()
                is JsonPrimitive -> this.toLua()
                is JsonNull -> this.toLua()
            }
        }

        private fun JsonNull.toLua(): LuaValue {
            return NIL
        }

        private fun JsonObject.toLua(): LuaTable {
            val result = LuaTable()
            this.forEach { (key, value) ->
                result[key] = value.toLua()
            }
            return result
        }

        private fun JsonPrimitive.toLua(): LuaValue {
            return if (this.isString) {
                return valueOf(this.content)
            } else {
                this.intOrNull?.let { valueOf(it) } ?: this.doubleOrNull?.let { valueOf(it) }
                    ?: this.booleanOrNull?.let { valueOf(it) } ?: valueOf(this.content)
            }
        }

        private fun JsonArray.toLua(): LuaTable {
            val result = LuaTable()
            this.forEach {
                result.insert(0, it.toLua())
            }
            return result
        }
    }

    @TinyFunction("Draw map tiles on the screen.")
    inner class draw : LibFunction() {

        @TinyCall(
            description = "Draw the default layer on the screen.",
        )
        override fun call(): LuaValue {
            val layer = resourceAccess.level(currentLevel)?.imageLayers?.get(currentLayer)
            if (layer != null) {
                resourceAccess.frameBuffer.copyFrom(
                    source = layer.pixels,
                    dstX = 0,
                    dstY = 0,
                    sourceX = 0,
                    sourceY = 0,
                    width = layer.width,
                    height = layer.height,
                )
            }
            return NONE
        }

        @TinyCall(
            description = "Draw the default layer on the screen at the x/y coordinates.",
        )
        override fun call(@TinyArg("x") a: LuaValue, @TinyArg("y") b: LuaValue): LuaValue {
            val layer = resourceAccess.level(currentLevel)?.imageLayers?.get(currentLayer)
            if (layer != null) {
                resourceAccess.frameBuffer.copyFrom(
                    source = layer.pixels,
                    dstX = a.checkint(),
                    dstY = b.checkint(),
                    sourceX = 0,
                    sourceY = 0,
                    width = layer.width,
                    height = layer.height,
                )
            }
            return NONE
        }

        @TinyCall(
            description = "Draw the default layer on the screen at the x/y coordinates.",
        )
        override fun call(
            @TinyArg("x", "x screen coordinate") a: LuaValue,
            @TinyArg("y", "y screen coordinate") b: LuaValue,
            @TinyArg("sx", "x map coordinate") c: LuaValue,
            @TinyArg("sy", "y map coordinate") d: LuaValue,
        ): LuaValue {
            val layer = resourceAccess.level(currentLevel)?.imageLayers?.get(currentLayer)
            if (layer != null) {
                resourceAccess.frameBuffer.copyFrom(
                    source = layer.pixels,
                    dstX = a.checkint(),
                    dstY = b.checkint(),
                    sourceX = c.checkint(),
                    sourceY = d.checkint(),
                    width = layer.width,
                    height = layer.height,
                )
            }
            return NONE
        }

        override fun invoke(
            @TinyArgs(
                names = ["x", "y", "sx", "sy", "width", "height"],
            ) args: Varargs,
        ): Varargs {
            if (args.narg() < 6) return super.invoke(args)
            val x = args.arg(1).checkint()
            val y = args.arg(2).checkint()
            val sx = args.arg(3).checkint()
            val sy = args.arg(4).checkint()
            val width = args.arg(5).checkint()
            val height = args.arg(6).checkint()

            val layer = resourceAccess.level(currentLevel)?.imageLayers?.get(currentLayer)
            if (layer != null) {
                resourceAccess.frameBuffer.copyFrom(
                    source = layer.pixels,
                    dstX = x,
                    dstY = y,
                    sourceX = sx,
                    sourceY = sy,
                    width = width,
                    height = height,
                )
            }
            return NONE
        }

        @TinyCall(
            description = "Draw the layer on the screen by it's index.",
        )
        override fun call(
            @TinyArg("layer", "index of the layer") a: LuaValue,
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
                    height = layer.height,
                )
            }
            return NONE
        }
    }
}
