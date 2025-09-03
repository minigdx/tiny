package com.github.minigdx.tiny.render.batch

import com.github.minigdx.tiny.ColorIndex
import com.github.minigdx.tiny.graphic.Camera
import com.github.minigdx.tiny.graphic.Clipper
import com.github.minigdx.tiny.resources.SpriteSheet

/**
 * Key used to group sprites that can be rendered together in a single batch.
 * Sprites with identical rendering state (dither pattern, color palette,
 * camera transform, and clipping region) can be batched together
 * to reduce GPU state changes and improve rendering performance.
 */
class BatchKey(
    var _spriteSheet: SpriteSheet? = null,
    var dither: Int = 0,
    var palette: Array<ColorIndex> = emptyArray(),
    var camera: Camera? = null,
    var clipper: Clipper? = null,
) {
    val spriteSheet: SpriteSheet
        get() = _spriteSheet!!

    fun set(
        spriteSheet: SpriteSheet,
        dither: Int,
        palette: Array<ColorIndex>,
        camera: Camera?,
        clipper: Clipper?,
    ): BatchKey {
        this._spriteSheet = spriteSheet
        this.dither = dither
        this.palette = palette
        this.camera = camera
        this.clipper = clipper

        return this
    }

    fun reset() {
        this._spriteSheet = null
        this.dither = 0
        this.palette = emptyArray()
        this.camera = null
        this.clipper = null
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is BatchKey) return false
        return _spriteSheet == other._spriteSheet &&
            dither == other.dither &&
            palette.contentEquals(other.palette) &&
            camera == other.camera &&
            clipper == other.clipper
    }

    override fun hashCode(): Int {
        var result = dither
        result = 31 * result + (_spriteSheet?.hashCode() ?: 0)
        result = 31 * result + palette.contentHashCode()
        result = 31 * result + (camera?.hashCode() ?: 0)
        result = 31 * result + (clipper?.hashCode() ?: 0)
        return result
    }
}
