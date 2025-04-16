package com.github.minigdx.tiny.render

import com.github.minigdx.tiny.graphic.PixelArray

interface ReadRender {
    /**
     * Read the actual frame buffer as RenderFrame.
     * This render frame can be used to read pixels from it.
     */
    fun readRender(context: RenderContext): RenderFrame

    /**
     * Read the actual frame buffer as a pixel array.
     * This operation is <strong>COSTLY</strong>. Don't use it lightly.
     * This frame buffer can be used to generate sprite sheet.
     */
    fun readRenderAsFrameBuffer(context: RenderContext): PixelArray
}
