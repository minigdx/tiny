package com.github.minigdx.tiny.render

import com.github.minigdx.tiny.ColorIndex
import com.github.minigdx.tiny.Pixel
import com.github.minigdx.tiny.graphic.FrameBuffer
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
     * Draw a primitive (circle, line, ...) into the [FrameBuffer]
     */
    fun drawPrimitive(block: (FrameBuffer) -> Unit)

    /**
     * Draw the virtual frame buffer on the screen.
     */
    fun draw()

    fun bindTextures(spritesheetToBind: List<SpriteSheet>)

    fun readFrameBuffer(): RenderFrame

    fun drawRect(
        x: Pixel,
        y: Pixel,
        width: Pixel,
        height: Pixel,
        colorIndex: ColorIndex,
        filled: Boolean,
    )

    fun drawLine(
        x1: Pixel,
        y1: Pixel,
        x2: Pixel,
        y2: Pixel,
        colorIndex: ColorIndex,
    )

    fun drawCircle(
        centerX: Pixel,
        centerY: Pixel,
        radius: Pixel,
        color: ColorIndex,
        filled: Boolean,
    )

    /**
     * Clear the virtual framebuffer.
     */
    fun clear(color: ColorIndex)

    fun init(windowManager: WindowManager)
}
