package com.github.minigdx.tiny.resources.ldtk

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

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
            return json.decodeFromString(Ldtk.serializer(), content)
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
        val intGridCsv: List<Int>,
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
        val autoLayer: List<Tile>,
        /**
         * The relative path to corresponding Tileset, if any.
         */
        val __tilesetRelPath: String,
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
        val gridTiles: List<Tile>,
        /**
         * The relative path to corresponding Tileset, if any.
         */
        val __tilesetRelPath: String,
        /**
         * This layer can use another tileset by overriding the tileset UID here.
         */
        val overrideTilesetUid: String? = null,
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
        val entityInstances: List<Entity>,
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
    val __identifier: String,
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

@Serializable
data class CustomField(
    val __identifier: String,
    /**
     * Type of the field, such as Int, Float, String, Enum(my_enum_name), Bool, etc.
     * NOTE: if you enable the advanced option Use Multilines type, you will have "Multilines" instead of "String" when relevant.
     */
    val __type: String,
)
