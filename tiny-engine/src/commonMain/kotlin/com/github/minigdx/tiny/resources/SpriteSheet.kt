package com.github.minigdx.tiny.resources

import com.github.minigdx.tiny.Pixel
import com.github.minigdx.tiny.graphic.PixelArray

/**
 * Represents a sprite sheet resource containing pixel data organized as a 2D grid.
 *
 * A sprite sheet is a collection of sprite images packed into a single texture,
 * allowing efficient rendering by reducing texture switches. Individual sprites
 * can be extracted from the sheet using coordinates and dimensions.
 *
 * @param version Resource version for cache invalidation
 * @param index Unique resource index within the game
 * @param name Human-readable name identifier
 * @param type Resource type classification
 * @param pixels 2D pixel array containing the sprite sheet image data
 * @param width Width of the sprite sheet in pixels
 * @param height Height of the sprite sheet in pixels
 * @param reload Flag indicating if the resource needs reloading
 */
class SpriteSheet(
    override val version: Int,
    override val index: Int,
    override val name: String,
    override val type: ResourceType,
    var pixels: PixelArray,
    var width: Pixel,
    var height: Pixel,
    /**
     * Texture unit assigned by the [com.github.minigdx.tiny.engine.GameResourceProcessor].
     */
    var textureUnit: Int? = null,

    override var reload: Boolean = false,
) : GameResource {

    data class SpriteSheetKey(val index: Int, val type: ResourceType)

    val key = SpriteSheetKey(index, type)

    /**
     * Indicates whether this sprite sheet contains primitive shapes and basic drawing elements.
     *
     * Primitive sprite sheets typically contain basic geometric shapes, lines, and fundamental
     * drawing elements used by the engine's shape drawing APIs. These are usually loaded
     * as system resources and provide fallback rendering capabilities.
     *
     * @return false for user sprite sheets, true for engine primitive sprite sheets
     */
    val isPrimitives
        get() = type == ResourceType.PRIMITIVE_SPRITESHEET

    /**
     * Indicates whether this sprite sheet is a system-managed resource.
     *
     * System sprite sheets are internal engine resources that contain essential
     * graphics elements like fonts.
     * These are typically loaded automatically by the engine and should not be
     * modified by user code.
     *
     * @return false for user sprite sheets, true for engine system sprite sheets
     */
    val isSystem
        get() = type == ResourceType.BOOT_SPRITESHEET
}
