package com.github.minigdx.tiny.lua

import com.github.mingdx.tiny.doc.TinyArg
import com.github.mingdx.tiny.doc.TinyCall
import com.github.mingdx.tiny.doc.TinyFunction
import com.github.mingdx.tiny.doc.TinyLib
import com.github.minigdx.tiny.Pixel
import com.github.minigdx.tiny.engine.GameResourceAccess
import com.github.minigdx.tiny.render.operations.DrawSprite
import com.github.minigdx.tiny.resources.GameLevel2
import com.github.minigdx.tiny.resources.LdtkLevel
import com.github.minigdx.tiny.resources.ldtk.CustomField
import com.github.minigdx.tiny.resources.ldtk.Entity
import com.github.minigdx.tiny.resources.ldtk.EntityRef
import com.github.minigdx.tiny.resources.ldtk.GridPoint
import com.github.minigdx.tiny.resources.ldtk.Layer
import com.github.minigdx.tiny.resources.ldtk.Level
import com.github.minigdx.tiny.resources.ldtk.Tile
import com.github.minigdx.tiny.resources.ldtk.TilesetRect
import kotlinx.serialization.json.JsonElement
import org.luaj.vm2.LuaTable
import org.luaj.vm2.LuaValue
import org.luaj.vm2.lib.LibFunction
import org.luaj.vm2.lib.OneArgFunction
import org.luaj.vm2.lib.TwoArgFunction
import org.luaj.vm2.lib.ZeroArgFunction
import kotlin.math.floor

/**
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
) : TwoArgFunction() {
    private var currentWorld: Int = 0
    private var currentLevel: Int = 0

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
        map["cflag"] = cflag()
        map["flag"] = flag()
        map["from"] = from()
        map["to"] = to()
        map["level"] = level()

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
            val level = activeLevel()

            if (arg.isnil()) {
                val activeLevel = level ?: return NIL
                val layersAsLua =
                    activeLevel.layerInstances.mapIndexed { index, layer -> layer.toLua(index, activeLevel) }
                return LuaValue.listOf(layersAsLua.toTypedArray())
            }

            val layerIndex = layerIndex(arg) ?: return NIL
            return level?.layerInstances?.getOrNull(layerIndex)?.toLua(layerIndex, level) ?: NIL
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

            // FIXME: I believe I should instead use the grid size of the layer.
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

    @TinyFunction("Get the flag from a tile, using cell coordinates.")
    inner class cflag : LibFunction() {
        @TinyCall("Get the flag from the tile at the coordinate cx,cy.")
        override fun call(
            @TinyArg("cx") arg1: LuaValue,
            @TinyArg("cy") arg2: LuaValue,
        ): LuaValue {
            val level = activeLevel() ?: return NIL
            return getCell(level.layerInstances.asSequence(), arg1.checkint(), arg2.checkint())
        }

        @TinyCall("Get the flag from the tile at the coordinate cx,cy from a specific layer.")
        override fun call(
            @TinyArg("cx") arg1: LuaValue,
            @TinyArg("cy") arg2: LuaValue,
            @TinyArg("layer") arg3: LuaValue,
        ): LuaValue {
            val level = activeLevel() ?: return NIL
            val layer = layerIndex(arg3) ?: return NIL
            return getCell(sequenceOf(level.layerInstances.get(layer)), arg1.checkint(), arg2.checkint())
        }
    }

    @TinyFunction("Get the flag from a tile, using screen coordinates.")
    inner class flag : LibFunction() {
        @TinyCall("Get the flag from the tile at the coordinate x,y.")
        override fun call(
            @TinyArg("x") arg1: LuaValue,
            @TinyArg("y") arg2: LuaValue,
        ): LuaValue {
            val level = activeLevel() ?: return NIL

            return getCell(
                level.layerInstances.asSequence(),
                arg1.checkint(),
                arg2.checkint(),
            ) { layer, x, y ->
                val cx = x / layer.__gridSize
                val cy = y / layer.__gridSize
                cx + cy * layer.__cWid
            }
        }

        @TinyCall("Get the flag from the tile at the coordinate x,y from a specific layer.")
        override fun call(
            @TinyArg("x") arg1: LuaValue,
            @TinyArg("y") arg2: LuaValue,
            @TinyArg("layer") arg3: LuaValue,
        ): LuaValue {
            val level = activeLevel() ?: return NIL
            val layer = layerIndex(arg3) ?: return NIL
            return getCell(
                sequenceOf(level.layerInstances.get(layer)),
                arg1.checkint(),
                arg2.checkint(),
            ) { layer, x, y ->
                val cx = x / layer.__gridSize
                val cy = y / layer.__gridSize
                cx + cy * layer.__cWid
            }
        }
    }

    @TinyFunction(
        """Table with all entities by type (ie: `map.entities["player"]`).
            
```
local entities = map.entities()
local players = entities["Player"]
for entity in all(players) do 
    shape.rectf(entity.x, entity.y, entity.width, entity.height, 8) -- display an entity using a rectangle
end
[...]
entity.fields -- access custom field of the entity
```
        """,
    )
    inner class entities : LibFunction() {
        private var currentWorldVersion = -1
        private var currentWorldIndex = -1
        private var currentLevelIndex = -1

        // Layer name to cache
        private var cachedEntities: MutableMap<String?, LuaValue> = mutableMapOf()

        private fun cacheMe(
            name: String? = null,
            factory: () -> LuaValue,
        ): LuaValue {
            val cache = cachedEntities[name]
            return if (
                // Entities aren't cached yet
                cache == null ||
                // Any change occurs on the current level used.
                currentWorldIndex != currentWorld ||
                currentWorldVersion != resourceAccess.level(currentWorld)?.version ||
                currentLevelIndex != currentLevel
            ) {
                currentWorldIndex = currentWorld
                currentWorldVersion = resourceAccess.level(currentWorld)?.version ?: -1
                currentLevelIndex = currentLevel
                // Create the entities and cache it.
                factory().also { cachedEntities.put(name, it) }
            } else {
                cache
            }
        }

        @TinyCall("Get all entities from all entities layer as a table, with an entry per type.")
        override fun call(): LuaValue {
            val level = activeLevel() ?: return NONE
            return cacheMe {
                getEntities(level.layerInstances)
            }
        }

        @TinyCall("Get all entities from the specific layer as a table, with an entry per type.")
        override fun call(arg: LuaValue): LuaValue {
            val level = activeLevel() ?: return NONE
            val index = layerIndex(arg) ?: return NONE
            val layer = level.layerInstances[index]
            return cacheMe(layer.__identifier) {
                getEntities(listOf(layer))
            }
        }

        private fun getEntities(layers: List<Layer>): LuaValue {
            val entities =
                layers.filter { layer -> layer.entityInstances != null }
                    // Get layers with entities
                    .flatMap { layer -> layer.entityInstances ?: emptyList() }
                    // Group entities per type (__identifier)
                    .groupBy { entity -> entity.__identifier }
                    .flatMap { (name, values) ->
                        kotlin.collections.listOf(
                            LuaValue.valueOf(name),
                            listOf(values.map { entity -> toLua(entity) }.toTypedArray()),
                        )
                    }
                    .toTypedArray()

            return tableOf(entities)
        }

        private fun toLua(entity: Entity): LuaTable {
            val table = LuaTable()
            val (x, y) = entity.px
            table["x"] = valueOf(x)
            table["y"] = valueOf(y)
            val (cx, cy) = entity.__grid
            table["cx"] = valueOf(cx)
            table["cy"] = valueOf(cy)
            entity.__worldX?.let { table["world_x"] = valueOf(it) }
            entity.__worldY?.let { table["world_y"] = valueOf(it) }
            table["iid"] = valueOf(entity.iid)
            table["width"] = valueOf(entity.width)
            table["height"] = valueOf(entity.height)
            // Convert custom fields
            table["fields"] = LuaValue.tableOf(entity.fieldInstances.flatMap { field -> toLua(field) }.toTypedArray())
            return table
        }

        private fun toLua(field: CustomField): List<LuaValue> {
            fun toLua(value: Any?): LuaValue {
                return when (value) {
                    is Int -> valueOf(value)
                    is Float -> valueOf(value.toDouble())
                    is String -> valueOf(value)
                    is Boolean -> valueOf(value)
                    is EntityRef -> value.toLua()
                    is GridPoint -> value.toLua()
                    is TilesetRect -> value.toLua()
                    is List<*> -> LuaValue.listOf(value.map { toLua(it) }.toTypedArray())
                    null -> NIL
                    else -> TODO()
                }
            }
            return listOf(
                LuaValue.valueOf(field.__identifier),
                toLua(field.__value),
            )
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

    // Return the index of the layer or null if not found (invalid data, layer names, ...).
    private fun layerIndex(arg: LuaValue): Int? {
        val level = activeLevel() ?: return null
        return if (arg.isint()) {
            arg.checkint().takeIf { level.layerInstances.getOrNull(it) != null }
        } else {
            val id = arg.checkjstring()
            level.layerInstances.indexOfFirst { it.__identifier == id }.takeIf { it != -1 }
        }
    }

    @TinyFunction("Draw map tiles on the screen.")
    inner class draw : LibFunction() {
        @TinyCall(
            description = "Draw all active layers on the screen.",
        )
        override fun call(): LuaValue {
            val world = resourceAccess.level(currentWorld) ?: return NIL
            val level = activeLevel() ?: return NIL

            val layers =
                level.layerInstances
                    // Select only actives layers
                    .filterIndexed { index, layer -> isActiveLayer(index) && layer.__tilesetRelPath != null }
                    // Layers will be drawn in the reverse order (from the back to the front)
                    .asReversed()
                    .asSequence()

            layers.flatMap { layer -> toDrawSprite(world, layer) }
                .forEach { opcode -> resourceAccess.addOp(opcode) }

            return NONE
        }

        @TinyCall(
            description = "Draw the layer with the name or the index on the screen.",
        )
        override fun call(
            @TinyArg("index") a: LuaValue,
        ): LuaValue {
            val layerIndex = layerIndex(a) ?: return NIL
            if (!isActiveLayer(layerIndex)) {
                return NIL
            }
            val layer = activeLevel()?.layerInstances[layerIndex] ?: return NIL
            // Not a drawable layer.
            if (layer.__tilesetRelPath == null) {
                return NIL
            }

            val world = resourceAccess.level(currentWorld) ?: return NIL
            toDrawSprite(world, layer).forEach {
                resourceAccess.addOp(it)
            }
            return NONE
        }

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

        fun toDrawSprite(
            world: GameLevel2,
            layer: Layer,
        ): List<DrawSprite> {
            val tileset = world.tilesset[layer.__tilesetRelPath!!]!!

            val attributesGrid = layer.gridTiles?.map { tile -> toAttribute(layer.__gridSize, tile) } ?: emptyList()
            val attributesAutoLayer =
                layer.autoLayer?.map { tile -> toAttribute(layer.__gridSize, tile) } ?: emptyList()
            val attributes = attributesGrid + attributesAutoLayer

            return DrawSprite.from(resourceAccess, layer.__identifier, tileset, attributes)
        }
    }

    private fun EntityRef.toLua(): LuaTable {
        val result = LuaTable()
        result["entityIid"] = valueOf(entityIid)
        result["layerIid"] = valueOf(layerIid)
        result["levelIid"] = valueOf(levelIid)
        result["worldIid"] = valueOf(worldIid)
        return result
    }

    private fun GridPoint.toLua(): LuaTable {
        val result = LuaTable()
        result["cx"] = valueOf(cx)
        result["cy"] = valueOf(cy)
        return result
    }

    private fun TilesetRect.toLua(): LuaTable {
        val result = LuaTable()
        result["x"] = valueOf(x)
        result["y"] = valueOf(y)
        result["w"] = valueOf(w)
        result["h"] = valueOf(h)
        result["tilesetUid"] = valueOf(tilesetUid)
        return result
    }

    private fun getCell(
        layers: Sequence<Layer>,
        cx: Int,
        cy: Int,
        index: (Layer, Int, Int) -> Int = { layer, a, b -> a + b * layer.__cWid },
    ): LuaValue {
        val cell: Int =
            layers.filter { layer -> layer.intGridCsv != null }
                // Get the first cell != 0 in all IntLayers
                .map { layer ->
                    layer.intGridCsv!!.getOrElse(index(layer, cx, cy)) { 0 }
                }.firstOrNull { cell -> cell != 0 }
                ?: 0

        if (cell == 0) return NIL
        return LuaValue.valueOf(cell)
    }
}
