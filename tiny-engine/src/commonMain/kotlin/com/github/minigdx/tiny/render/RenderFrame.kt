package com.github.minigdx.tiny.render

import com.github.minigdx.tiny.ColorIndex
import com.github.minigdx.tiny.Pixel
import com.github.minigdx.tiny.graphic.PixelArray

/**
 * Generated frame by the GPU.
 */
interface RenderFrame {
    /**
     * Copy the [RenderFrame] into a [pixelArray] for further usage.
     * As it will iterate on ALL pixels of the buffer,
     * don't use this method lightly
     */
    fun copyInto(pixelArray: PixelArray)

    /**
     * Get the color index being this specific pixel.
     */
    fun getPixel(
        x: Pixel,
        y: Pixel,
    ): ColorIndex
}
