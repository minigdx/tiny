package com.github.minigdx.tiny.render

import com.github.minigdx.tiny.ColorIndex
import com.github.minigdx.tiny.Pixel
import com.github.minigdx.tiny.platform.WindowManager
import com.github.minigdx.tiny.resources.SpriteSheet

interface VirtualFrameBuffer {
    /**
     * Draw a sprite into the virtual frame buffer
     */
    fun draw(
        source: SpriteSheet,
        sourceX: Pixel,
        sourceY: Pixel,
        sourceWidth: Pixel,
        sourceHeight: Pixel,
        destinationX: Pixel,
        destinationY: Pixel,
        flipX: Boolean = false,
        flipY: Boolean = false,
    )

    /**
     * Draw a sprite using one solid color
     */
    fun drawMonocolor(
        source: SpriteSheet,
        color: ColorIndex,
        sourceX: Pixel,
        sourceY: Pixel,
        sourceWidth: Pixel,
        sourceHeight: Pixel,
        destinationX: Pixel,
        destinationY: Pixel,
        flipX: Boolean = false,
        flipY: Boolean = false,
    )

    /**
     * Draw the virtual frame buffer on the screen.
     */
    fun draw()

    /**
     * Bind sprite sheet textures for rendering.
     */
    fun bindTextures(spritesheetToBind: List<SpriteSheet>)

    /**
     * Read the current frame buffer contents.
     */
    fun readFrameBuffer(): RenderFrame

    /**
     * Draw a rectangle primitive.
     */
    fun drawRect(
        x: Pixel,
        y: Pixel,
        width: Pixel,
        height: Pixel,
        colorIndex: ColorIndex,
        filled: Boolean,
    )

    /**
     * Draw a line primitive.
     */
    fun drawLine(
        x1: Pixel,
        y1: Pixel,
        x2: Pixel,
        y2: Pixel,
        colorIndex: ColorIndex,
    )

    /**
     * Draw a circle primitive.
     */
    fun drawCircle(
        centerX: Pixel,
        centerY: Pixel,
        radius: Pixel,
        color: ColorIndex,
        filled: Boolean,
    )

    /**
     * Draw a point primitive.
     */
    fun drawPoint(
        x: Pixel,
        y: Pixel,
        color: ColorIndex,
    )

    /**
     * Draw a triangle primitive.
     */
    fun drawTriangle(
        x1: Pixel,
        y1: Pixel,
        x2: Pixel,
        y2: Pixel,
        x3: Pixel,
        y3: Pixel,
        color: ColorIndex,
        filled: Boolean,
    )

    /**
     * Configure dithering settings.
     */
    fun dithering(dither: Int): Int

    /**
     * Clear the virtual framebuffer.
     */
    fun clear(color: ColorIndex)

    /**
     * Initialize the virtual frame buffer with window manager.
     */
    fun init(windowManager: WindowManager)
}
