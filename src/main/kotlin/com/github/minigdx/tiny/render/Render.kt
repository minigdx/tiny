package com.github.minigdx.tiny.render

import com.github.minigdx.tiny.Pixel
import com.github.minigdx.tiny.platform.RenderContext

interface Render {
    fun init(): RenderContext

    fun draw(context: RenderContext, image: ByteArray, width: Pixel, height: Pixel)
}
