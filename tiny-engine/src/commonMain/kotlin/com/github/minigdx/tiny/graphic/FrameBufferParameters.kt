package com.github.minigdx.tiny.graphic

import com.github.minigdx.tiny.ColorIndex
import com.github.minigdx.tiny.Pixel

class Blender(internal val gamePalette: ColorPalette) {
    private var switch: Array<ColorIndex> = Array(gamePalette.size) { index -> index }

    private var cachedPaletteReference: Array<ColorIndex>? = null

    internal var dithering: Int = 0xFFFF

    internal val hasDithering: Boolean
        get() {
            return dithering != 0xFFFF
        }

    fun palette(): Array<ColorIndex> {
        fun cache(): Array<ColorIndex> {
            val copyOf = switch.copyOf()
            cachedPaletteReference = copyOf
            return copyOf
        }
        return cachedPaletteReference ?: cache()
    }

    fun dither(pattern: Int): Int {
        val prec = dithering
        dithering = pattern and 0xFFFF
        return prec
    }

    fun pal() {
        cachedPaletteReference = null
        switch = Array(gamePalette.size) { index -> index }
    }

    fun pal(
        source: ColorIndex,
        target: ColorIndex,
    ) {
        cachedPaletteReference = null
        switch[gamePalette.check(source)] = gamePalette.check(target)
    }

    fun mix(
        colors: ByteArray,
        x: Pixel,
        y: Pixel,
        transparency: Array<Int>?,
    ): ByteArray? {
        fun dither(pattern: Int): Boolean {
            val a = x % 4
            val b = (y % 4) * 4

            return (pattern shr (15 - (a + b))) and 0x01 == 0x01
        }

        val color = gamePalette.check(colors[0].toInt())
        colors[0] = switch[color].toByte()
        // Return null if transparent
        if (transparency == null && gamePalette.isTransparent(colors[0].toInt())) return null
        return if (!dither(dithering)) {
            null
        } else {
            colors
        }
    }
}

class Camera() {
    var x = 0
        internal set
    var y = 0
        internal set

    fun set(
        x: Int,
        y: Int,
    ) {
        this.x = x
        this.y = y
    }

    fun cx(x: Int): Int {
        return x - this.x
    }

    fun cy(y: Int): Int {
        return y - this.y
    }
}

class FrameBufferParameters(
    val width: Pixel,
    val height: Pixel,
    val gamePalette: ColorPalette,
) {
    internal val clipper: Clipper = Clipper(width, height)

    internal val blender = Blender(gamePalette)

    internal val camera = Camera()

    private var currentClipper: Clipper? = null

    fun clipper(): Clipper {
        val currentInstance = currentClipper
        val result = if (clipper.updated || currentInstance == null) {
            clipper.updated = false
            val newInstance = Clipper(width, height).set(clipper.left, clipper.top, clipper.width, clipper.height)
            currentClipper = newInstance
            newInstance
        } else {
            currentInstance
        }
        return result
    }
}
