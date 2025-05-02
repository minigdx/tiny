package com.github.minigdx.tiny.resources.ldtk

import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.descriptors.buildClassSerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

/**
 * Unique Instance Identifier (IID)
 *
 * @see: https://ldtk.io/docs/game-dev/json-overview/unique-identifiers/
 */
typealias StrIID = String

/**
 * Coordinate with grid unit.
 */
typealias GridInt = Int

/**
 * Coordinate with pixel unit.
 */
typealias PixelInt = Int

/**
 * [x,y] format
 */
typealias PixelCoord = List<PixelInt>

/**
 * Grid-based coordinates [x,y] format
 */
typealias GridCoord = List<GridInt>

/**
 * Tile ID, used in tileset.
 */
typealias TileID = Int

/**
 * Describes the layout of levels within a game world.
 *
 * <p>There are three primary layout types:</p>
 *
 * <ul>
 * <li><b>Linear Horizontal or Linear Vertical:</b>
 * The order of the levels in the {@code levels} array directly corresponds
 * to their arrangement in the world. For horizontal layouts, levels are
 * placed sequentially along the X-axis. For vertical layouts, they are
 * placed sequentially along the Y-axis.</li>
 *
 * <li><b>Free:</b>
 * Each level is positioned in the world using its {@code worldX} and
 * {@code worldY} coordinates, allowing for arbitrary placement.</li>
 *
 * <li><b>GridVania:</b>
 * The {@code worldGridWidth} and {@code worldGridHeight} values defined in
 * the root of the JSON structure specify the world grid dimensions. Each
 * level's {@code worldX} and {@code worldY} coordinates are snapped to this grid,
 * ensuring alignment with the grid structure.</li>
 * </ul>
 *
 * See: https://ldtk.io/docs/game-dev/json-overview/world-layout/
 */
enum class WorldLayout {
    LinearHorizontal,
    LinearVertical,
    Free,
    GridVania,
}

@Serializable
data class Ldtk(
    val iid: StrIID,
    val worldLayout: WorldLayout,
    val levels: List<Level>,
) {
    companion object {
        fun read(content: String): Ldtk {
            val json = Json {
                allowStructuredMapKeys = true
                ignoreUnknownKeys = true
                classDiscriminator = "__type"
            }
            return json.decodeFromString(serializer(), content)
        }
    }
}

@Serializable
data class Level(
    val identifier: String,
    val iid: StrIID,
    val worldX: Int,
    val worldY: Int,
    val layerInstances: List<Layer>,
    /**
     * 	An array of all custom fields and their values.
     */
    val fieldInstances: List<CustomField>,
)

@Serializable
sealed interface Layer {
    val __identifier: String
    val __cWid: GridInt
    val __cHei: GridInt
    val __gridSize: GridInt

    // optional offset that could happen when resizing levels
    val pxOffsetX: Int

    // optional offset that could happen when resizing levels
    val pxOffsetY: Int

    val seed: Long

    val __tilesetRelPath: String?

    val overrideTilesetUid: Int?

    val intGridCsv: List<Int>?

    val entityInstances: List<Entity>?

    val autoLayerTiles: List<Tile>?

    val gridTiles: List<Tile>?

    @SerialName("IntGrid")
    @Serializable
    data class IntGrid(
        override val __identifier: String,
        override val __cWid: GridInt,
        override val __cHei: GridInt,
        override val __gridSize: GridInt,
        override val pxOffsetX: Int,
        override val pxOffsetY: Int,
        override val seed: Long,
        /**
         * A list of all values in the IntGrid layer, stored in CSV format (Comma Separated Values).
         * Order is from left to right, and top to bottom (ie. first row from left to right, followed by second row, etc).
         * 0 means "empty cell" and IntGrid values start at 1.
         * The array size is __cWid x __cHei cells.
         */
        override val intGridCsv: List<Int>,
        /**
         * Always null
         */
        override val overrideTilesetUid: Int? = null,
        /**
         * Always null
         */
        override val entityInstances: List<Entity>? = null,
        /**
         * Always null
         */
        override val __tilesetRelPath: String? = null,
        /**
         * Always null
         */
        override val autoLayerTiles: List<Tile>? = null,
        /**
         * Always null
         */
        override val gridTiles: List<Tile>? = null,
    ) : Layer

    @SerialName("AutoLayer")
    @Serializable
    data class AutoLayer(
        override val __identifier: String,
        override val __cWid: GridInt,
        override val __cHei: GridInt,
        override val __gridSize: GridInt,
        override val pxOffsetX: Int,
        override val pxOffsetY: Int,
        override val seed: Long,
        override val autoLayerTiles: List<Tile>,
        /**
         * The relative path to corresponding Tileset, if any.
         */
        override val __tilesetRelPath: String,
        /**
         * Always null
         */
        override val overrideTilesetUid: Int? = null,
        /**
         * Always null
         */
        override val intGridCsv: List<Int>? = null,
        /**
         * Always null
         */
        override val entityInstances: List<Entity>? = null,
        /**
         * Always null
         */
        override val gridTiles: List<Tile>? = null,
    ) : Layer

    @SerialName("Tiles")
    @Serializable
    data class TilesLayer(
        override val __identifier: String,
        override val __cWid: GridInt,
        override val __cHei: GridInt,
        override val __gridSize: GridInt,
        override val pxOffsetX: Int,
        override val pxOffsetY: Int,
        override val seed: Long,
        override val gridTiles: List<Tile>,
        /**
         * The relative path to corresponding Tileset, if any.
         */
        override val __tilesetRelPath: String,
        /**
         * This layer can use another tileset by overriding the tileset UID here.
         */
        override val overrideTilesetUid: Int? = null,
        /**
         * Always null
         */
        override val intGridCsv: List<Int>? = null,
        /**
         * Always null
         */
        override val entityInstances: List<Entity>? = null,
        /**
         * Always null
         */
        override val autoLayerTiles: List<Tile>? = null,
    ) : Layer

    @SerialName("Entities")
    @Serializable
    data class EntitiesLayer(
        override val __identifier: String,
        override val __cWid: GridInt,
        override val __cHei: GridInt,
        override val __gridSize: GridInt,
        override val pxOffsetX: Int,
        override val pxOffsetY: Int,
        override val seed: Long,
        override val entityInstances: List<Entity>,
        /**
         * Always null
         */
        override val intGridCsv: List<Int>? = null,
        /**
         * Always null
         */
        override val __tilesetRelPath: String? = null,
        /**
         * Always null
         */
        override val overrideTilesetUid: Int? = null,
        /**
         * Always null
         */
        override val autoLayerTiles: List<Tile>? = null,
        /**
         * Always null
         */
        override val gridTiles: List<Tile>? = null,
    ) : Layer
}

/**
 * @See: https://ldtk.io/json/#ldtk-Tile
 */
@Serializable
data class Tile(
    /**
     * Alpha/opacity of the tile (0-1, defaults to 1)
     */
    val a: Float,
    /**
     * "Flip bits", a 2-bits integer to represent the mirror transformations of the tile.
     * - Bit 0 = X flip
     * - Bit 1 = Y flip
     * Examples: f=0 (no flip), f=1 (X flip only), f=2 (Y flip only), f=3 (both flips)
     */
    val f: Int,
    /**
     * Pixel coordinates of the tile in the layer ([x,y] format). Don't forget optional layer offsets, if they exist!
     */
    val px: PixelCoord,
    /**
     * Pixel coordinates of the tile in the tileset ([x,y] format)
     */
    val src: PixelCoord,
    /**
     * The Tile ID in the corresponding tileset.
     */
    val t: TileID,
)

@Serializable
data class Entity(
    /**
     * Grid-based coordinates ([x,y] format)
     */
    val __grid: GridCoord,
    /**
     * Entity definition identifier (The type of the entity)
     */
    val __identifier: String,
    /**
     * Pivot coordinates ([x,y] format, values are from 0 to 1) of the Entity
     */
    val __pivot: List<Float>,
    /**
     * X world coordinate in pixels. Only available in GridVania or Free world layouts.
     */
    val __worldX: PixelInt? = null,
    /**
     * Y world coordinate in pixels Only available in GridVania or Free world layouts.
     */
    val __worldY: PixelInt? = null,
    /**
     * 	An array of all custom fields and their values.
     */
    val fieldInstances: List<CustomField>,
    /**
     * Entity height in pixels. For non-resizable entities, it will be the same as Entity definition.
     */
    val height: PixelInt,
    /**
     * Entity width in pixels. For non-resizable entities, it will be the same as Entity definition.
     */
    val width: PixelInt,
    val iid: StrIID,
    /**
     * Pixel coordinates ([x,y] format) in current level coordinate space. Don't forget optional layer offsets, if they exist!
     */
    val px: PixelCoord,
)

@Serializable(with = CustomFieldSerializer::class)
data class CustomField(
    /**
     * Field definition identifier
     */
    val __identifier: String,
    /**
     * Type of the field, such as Int, Float, String, Enum(my_enum_name), Bool, etc.
     * NOTE: if you enable the advanced option Use Multilines type, you will have "Multilines" instead of "String" when relevant.
     */
    val __type: String,
    /**
     * Actual value of the field instance. The value type varies, depending on __type:
     * - For classic types (ie. Integer, Float, Boolean, String, Text and FilePath), you just get the actual value with the expected type.
     * - For Color, the value is an hexadecimal string using "#rrggbb" format.
     * - For Enum, the value is a String representing the selected enum value.
     * - For Point, the value is a GridPoint object.
     * - For Tile, the value is a TilesetRect object.
     * - For EntityRef, the value is an EntityReferenceInfos object.
     *
     * If the field is an array, then this __value will also be a JSON array.
     */
    val __value: Any?,
)

@Serializable
data class EntityRef(
    /**
     * IID of the refered EntityInstance
     */
    val entityIid: StrIID,
    /**
     * 	IID of the LayerInstance containing the refered EntityInstance
     */
    val layerIid: StrIID,
    /**
     * IID of the Level containing the refered EntityInstance
     */
    val levelIid: StrIID,
    /**
     * IID of the World containing the refered EntityInstance
     */
    val worldIid: StrIID,
)

/**
 * This object represents a custom sub rectangle in a Tileset image.
 * @see https://ldtk.io/json/#ldtk-TilesetRect
 */
@Serializable
data class TilesetRect(
    /**
     * Height in pixels
     */
    val h: PixelInt,
    /**
     * 	UID of the tileset
     */
    val tilesetUid: Int,
    /**
     * Width in pixels
     */
    val w: PixelInt,
    /**
     * 	X pixels coordinate of the top-left corner in the Tileset image
     */
    val x: PixelInt,
    /**
     * Y pixels coordinate of the top-left corner in the Tileset image
     */
    val y: PixelInt,
)

/**
 * This object is just a grid-based coordinate used in Field values.
 * @see: https://ldtk.io/json/#ldtk-GridPoint
 */
@Serializable
data class GridPoint(
    /**
     * X grid-based coordinate
     */
    val cx: GridInt,
    /**
     * 	Y grid-based coordinate
     */
    val cy: GridInt,
)

object CustomFieldSerializer : KSerializer<CustomField> {
    override val descriptor: SerialDescriptor =
        buildClassSerialDescriptor("CustomField") {
            element("__type", PrimitiveSerialDescriptor("__type", PrimitiveKind.STRING))
            element("__value", JsonElement.serializer().descriptor) // On manipule un JsonElement en interne
        }

    override fun serialize(
        encoder: Encoder,
        value: CustomField,
    ) = throw UnsupportedOperationException(
        "LdTk file is not supposed to be serialized. " +
            "If you need to update it, Use LdTk instead: https://ldtk.io/",
    )

    override fun deserialize(decoder: Decoder): CustomField {
        val jsonElement = JsonElement.serializer().deserialize(decoder)
        val jsonObject = jsonElement.jsonObject
        val identifier = jsonObject["__identifier"]?.jsonPrimitive?.content!!
        val type = jsonObject["__type"]?.jsonPrimitive?.content!!
        val valueElement = jsonObject["__value"]

        return CustomField(
            identifier,
            type,
            deserialize(type, valueElement),
        )
    }

    private fun deserialize(
        type: String,
        valueElement: JsonElement?,
    ): Any? {
        if (valueElement is JsonNull) {
            return null
        }
        return when (type) {
            "Int" -> valueElement?.jsonPrimitive?.content?.toIntOrNull()
            "Float" -> valueElement?.jsonPrimitive?.content?.toFloatOrNull()
            "String", "Multilines", "Text", "FilePath", "Color" -> valueElement?.jsonPrimitive?.content
            "Bool" -> valueElement?.jsonPrimitive?.content?.toBooleanStrictOrNull()
            "Point" -> valueElement?.let { Json.decodeFromJsonElement(GridPoint.serializer(), it) }
            "Tile" -> valueElement?.let { Json.decodeFromJsonElement(TilesetRect.serializer(), it) }
            "EntityRef" -> valueElement?.let { Json.decodeFromJsonElement(EntityRef.serializer(), it) }
            else ->
                if (type.startsWith("LocalEnum.")) {
                    valueElement?.jsonPrimitive?.content
                } else if (type.startsWith("Array<")) {
                    val nestedType = type.removePrefix("Array<").removeSuffix(">")
                    valueElement?.jsonArray?.map { nestedElement -> deserialize(nestedType, nestedElement) }
                } else {
                    throw IllegalArgumentException(
                        "$type is not supported. " +
                            "Is the type describe exist in LdTK ? (https://ldtk.io/json/#ldtk-FieldInstanceJson) " +
                            "If yes, please fill an issue to support it in Tiny (https://github.com/minigdx/tiny/issues).",
                    )
                }
        }
    }
}
