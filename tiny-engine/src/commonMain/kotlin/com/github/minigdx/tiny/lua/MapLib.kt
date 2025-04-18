package com.github.minigdx.tiny.lua

import com.github.mingdx.tiny.doc.TinyArg
import com.github.mingdx.tiny.doc.TinyArgs
import com.github.mingdx.tiny.doc.TinyCall
import com.github.mingdx.tiny.doc.TinyFunction
import com.github.mingdx.tiny.doc.TinyLib
import com.github.minigdx.tiny.Pixel
import com.github.minigdx.tiny.engine.DrawSprite
import com.github.minigdx.tiny.engine.GameResourceAccess
import com.github.minigdx.tiny.graphic.ColorPalette
import com.github.minigdx.tiny.resources.GameLevel
import com.github.minigdx.tiny.resources.LdtkEntity
import com.github.minigdx.tiny.resources.LdtkLevel
import com.github.minigdx.tiny.resources.ldtk.Layer
import com.github.minigdx.tiny.resources.ldtk.Level
import com.github.minigdx.tiny.resources.ldtk.Tile
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
import org.luaj.vm2.lib.ZeroArgFunction
import kotlin.math.floor

/**
 * map.draw()
 * map.draw("Wall") // draw layer Wall.
 * map.draw("Entities") // NOP as Entities is not a drawalble layer
 *
 * map.sdraw(x, y, srcX, srcY, with, height)
 * map.sdraw(x, y, srcX, srcY, with, height, "Layer")
 *
 * map.world("Another.ldtk") // load another world as current world
 * map.world() // return the actual world identifier (index)
 * map.world(2)
 * map.level("AnotherLevel") // load another level from the current world
 * map.level() // return the actual level identifier (index)
 * map.level(3)
 *
 * map.entities()["Door"] // List all Door entities from all entities layer
 * map.entities("NiceDoor")["Door"] // List all Door entities from the layer "NiceDoor"
 *
 * map.cflag(cx, cy) // get the flag to the FIRST IntLayer (by cell units)
 * map.flag(x, y) // same, by screen coordinate
 *
 * map.cflag(cx, cy, "Layer") // get the flag on this specific layer
 *
 *
 * map.properties -> {
 *      worldIdentifier: ...
 *      worldLayer: ...
 *      levelIdentifier: ...
 *      x, y: ...
 *
 * }
 *
 */
@TinyLib(
    "map",
    "Access map created with LDTk ( https://ldtk.io/ ).",
)
class MapLib(
    private val resourceAccess: GameResourceAccess,
    private val spriteSize: Pair<Pixel, Pixel>,
    private val colors: ColorPalette,
) : TwoArgFunction() {
    private var currentWorld: Int = 0
    private var currentLevel: Int = 0

    private var currentLayer: Int = 0

    // Empty array means that all layers are active.
    // Otherwise, the state can be accessed using the index of the layer.
    private var layersState: Array<Boolean> = emptyArray()

    override fun call(
        arg1: LuaValue,
        arg2: LuaValue,
    ): LuaValue {
        val map = LuaTable()
        map["draw"] = draw()
        map["layer"] = layer()
        map["entities"] = entities()
        map["flag"] = flag()
        map["from"] = from()
        map["to"] = to()
        map["level"] = level()
        map["x"] = property(int = { it.x })
        map["y"] = property(int = { it.y })
        map["identifier"] = property(str = { it.identifier })
        map["unique_identifier"] = property(str = { it.uniqueIdentifer })
        map["width"] = property(int = { it.width })
        map["height"] = property(int = { it.height })
        map["bgColors"] = property(int = { colors.getColorIndex(it.bgColor) })
        map["customFields"] = property(json = { it.customFields })
        arg2["map"] = map
        arg2["package"]["loaded"]["map"] = map
        return map
    }

    inner class property(
        val int: ((LdtkLevel) -> Int)? = null,
        val str: ((LdtkLevel) -> String)? = null,
        val json: ((LdtkLevel) -> JsonElement)? = null,
    ) : ZeroArgFunction() {
        override fun call(): LuaValue {
            // FIXME: rework that.
            val value = NIL
            return value
        }
    }

    @TinyFunction("Set the current level to use.")
    inner class level : OneArgFunction() {
        @TinyCall("Return the index of the current level.")
        override fun call(): LuaValue {
            return super.call()
        }

        @TinyCall(
            "Set the current level to use. " +
                "The level can be an index or the id defined by LDTK. " +
                "Return the previous index level.",
        )
        override fun call(
            @TinyArg("level") arg: LuaValue,
        ): LuaValue {
            // FIXME: rework
            return NIL
        }
    }

    @TinyFunction("Get the list of layers from the actual level.")
    inner class layer : OneArgFunction() {
        @TinyCall(
            "Get the layer at the specified index or name from the actual level. " +
                "The layer in the front is 0.",
        )
        override fun call(
            @TinyArg("layer_index") arg: LuaValue,
        ): LuaValue {
            val level = activeLevel() ?: return NIL

            if (arg.isnil()) {
                val activeLevel = activeLevel() ?: return NIL
                val layersAsLua = activeLevel.layerInstances.mapIndexed { index, layer -> layer.toLua(index, activeLevel) }
                return LuaValue.listOf(layersAsLua.toTypedArray())
            }

            val layerIndex =
                if (!arg.isint()) {
                    val id = arg.checkjstring()
                    level.layerInstances.indexOfFirst { it.__identifier == id }
                } else {
                    arg.checkint()
                }

            return level.layerInstances.getOrNull(layerIndex)?.toLua(layerIndex, level) ?: NIL
        }

        @TinyCall("Get the list of layers from the actual level.")
        override fun call(): LuaValue = super.call()

        private fun Layer.toLua(
            layerIndex: Int,
            level: Level,
        ): LuaValue {
            val result = LuaTable()
            result["toggle"] =
                object : ZeroArgFunction() {
                    override fun call(): LuaValue {
                        if (layersState.isEmpty()) {
                            // All layers are active by default
                            layersState = Array(level.layerInstances.size) { true }
                        }
                        val current = layersState[layerIndex]
                        layersState[layerIndex] = current.not()
                        return valueOf(current)
                    }
                }
            return result
        }
    }

    @TinyFunction("Convert cell coordinates cx, cy into map screen coordinates x, y.")
    inner class from : TwoArgFunction() {
        @TinyCall("Convert the cell coordinates into coordinates as a table [x,y].")
        override fun call(
            arg1: LuaValue,
            arg2: LuaValue,
        ): LuaValue {
            val (cx, cy) =
                if (arg1.istable()) {
                    arg1["cx"].toint() to arg1["cy"].toint()
                } else {
                    arg1.checkint() to arg2.checkint()
                }

            return LuaTable(2, 2).apply {
                set("x", valueOf(cx * spriteSize.first.toDouble()))
                set("y", valueOf(cy * spriteSize.second.toDouble()))
            }
        }

        @TinyCall("Convert the cell coordinates from a table [cx,cy] into screen coordinates as a table [x,y].")
        override fun call(
            @TinyArg("cell") arg: LuaValue,
        ): LuaValue = super.call(arg)
    }

    @TinyFunction(
        "Convert screen coordinates x, y into map cell coordinates cx, cy.\n" +
            "For example, coordinates of the player can be converted to cell coordinates to access the flag " +
            "of the tile matching the player coordinates.",
    )
    inner class to : TwoArgFunction() {
        @TinyCall("Convert the coordinates into cell coordinates as a table [cx,cy].")
        override fun call(
            @TinyArg("x") arg1: LuaValue,
            @TinyArg("y") arg2: LuaValue,
        ): LuaValue {
            val (x, y) =
                if (arg1.istable()) {
                    arg1["x"].toint() to arg1["y"].toint()
                } else {
                    arg1.checkint() to arg2.checkint()
                }

            return LuaTable(2, 2).apply {
                set("cx", valueOf(floor(x / spriteSize.first.toDouble())))
                set("cy", valueOf(floor(y / spriteSize.second.toDouble())))
            }
        }

        @TinyCall("Convert the coordinates from a table [x,y] into cell coordinates as a table [cx,cy].")
        override fun call(
            @TinyArg("coordinates") arg: LuaValue,
        ) = super.call(arg)
    }

    @TinyFunction("Get the flag from a tile.")
    inner class flag : TwoArgFunction() {
        @TinyCall("Get the flag from the tile at the coordinate cx,cy.")
        override fun call(
            @TinyArg("cx") arg1: LuaValue,
            @TinyArg("cy") arg2: LuaValue,
        ): LuaValue {
            // FIXME: rework
            return NIL
        }

        @TinyCall("Get the flag from the tile at the coordinate table [cx,cy].")
        override fun call(
            @TinyArg("cell") arg: LuaValue,
        ): LuaValue = super.call(arg)
    }

    @TinyFunction(
        """Table with all entities by type (ie: `map.entities["player"]`).
            
```
local players = map.entities["player"]
local entity = players[1] -- get the first player
shape.rectf(entity.x, entity.y, entity.width, entity.height, 8) -- display an entity using a rectangle
[...]
entity.customFields -- access custom field of the entity
```
        """,
    )
    inner class entities : LuaTable() {
        private val cachedEntities: MutableMap<Int, LuaValue> = mutableMapOf()

        private var currentLevelVersion = currentWorld to -1

        private val entities: LuaValue
            get() {
                // FIXME: rework
                return NIL
            }

        private fun cacheMe(level: GameLevel?): LuaValue {
            // Transform the list of entities into a table in Lua.
            val toCache = LuaTable()
            level?.entities?.forEach { (key, v) ->
                val entitiesOfType = LuaTable()
                v.forEach {
                    entitiesOfType[it.iid] = it.toLuaTable()
                }
                toCache[key] = entitiesOfType
            }

            cachedEntities[currentWorld] = toCache
            currentLevelVersion = currentWorld to (level?.version ?: -1)
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
    }

    private fun isActiveLayer(index: Int): Boolean {
        if (layersState.isEmpty()) return true
        return layersState.getOrElse(index) { return true }
    }

    private fun activeLevel(): Level? {
        val world = resourceAccess.level(currentWorld)
        return world
            ?.ldtk
            ?.levels
            ?.getOrNull(currentLevel)
    }

    @TinyFunction("Draw map tiles on the screen.")
    inner class draw : LibFunction() {
        @TinyCall(
            description = "Draw the default layer on the screen.",
        )
        override fun call(): LuaValue {
            val world = resourceAccess.level(currentWorld) ?: return NONE
            val level = activeLevel() ?: return NONE

            val layers =
                level.layerInstances
                    // Select only actives layers
                    .filterIndexed { index, layer -> isActiveLayer(index) && layer.__tilesetRelPath != null }
                    // Layers will be drawn in the reverse order (from the back to the front)
                    .asReversed()
                    .asSequence()

            fun toAttribute(
                size: Int,
                tile: Tile,
            ): DrawSprite.DrawSpriteAttribute {
                fun Int.toFlip(): Pair<Boolean, Boolean> {
                    return ((this and 0x01) == 0x01) to ((this and 0x02) == 0x02)
                }
                val (srcX, srcY) = tile.src
                val (destX, destY) = tile.px
                val (flipX, flipY) = tile.f.toFlip()

                return DrawSprite.DrawSpriteAttribute(
                    srcX,
                    srcY,
                    size,
                    size,
                    destX,
                    destY,
                    flipX,
                    flipY,
                )
            }

            layers.flatMap { layer ->
                val tileset = world.tilesset[layer.__tilesetRelPath!!]!!

                val attributesGrid = layer.gridTiles?.map { tile -> toAttribute(layer.__gridSize, tile) } ?: emptyList()
                val attributesAutoLayer =
                    layer.autoLayer?.map { tile -> toAttribute(layer.__gridSize, tile) } ?: emptyList()
                val attributes = attributesGrid + attributesAutoLayer

                DrawSprite.from(layer.__identifier, tileset, attributes)
            }.forEach { opcode ->
                resourceAccess.addOp(opcode)
            }

            return NONE
        }

        @TinyCall(
            description = "Draw the layer with the name or the index on the screen.",
        )
        override fun call(
            @TinyArg("x") a: LuaValue,
            @TinyArg("y") b: LuaValue,
        ): LuaValue {
            // FIXME: reworkd
            return NONE
        }

        @TinyCall(
            description =
                "Draw the default layer on the screen at the x/y coordinates " +
                    "starting the mx/my coordinates from the map.",
        )
        override fun call(
            @TinyArg("x", "x screen coordinate") a: LuaValue,
            @TinyArg("y", "y screen coordinate") b: LuaValue,
            @TinyArg("mx", "x map coordinate") c: LuaValue,
            @TinyArg("my", "y map coordinate") d: LuaValue,
        ): LuaValue {
            // FIXME: reworkd
            return NONE
        }

        @TinyCall(
            description =
                "Draw the default layer on the screen at the x/y coordinates " +
                    "starting the mx/my coordinates from the map using the size width/height.",
        )
        override fun invoke(
            @TinyArgs(
                names = ["x", "y", "mx", "my", "width", "height"],
            ) args: Varargs,
        ): Varargs {
            if (args.narg() < 6) return super.invoke(args)
            val x = args.arg(1).checkint()
            val y = args.arg(2).checkint()
            val sx = args.arg(3).checkint()
            val sy = args.arg(4).checkint()
            val width = args.arg(5).checkint()
            val height = args.arg(6).checkint()

            // FIXME: rework
            return NONE
        }

        @TinyCall(
            description = "Draw the layer on the screen by it's index.",
        )
        override fun call(
            @TinyArg("layer", "index of the layer") a: LuaValue,
        ): LuaValue {
            // FIXME: rework
            return NONE
        }
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
