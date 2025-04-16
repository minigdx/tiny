package com.github.minigdx.tiny.render.gl

import com.danielgergely.kgl.ByteBuffer
import com.danielgergely.kgl.Framebuffer
import com.danielgergely.kgl.Texture
import com.github.minigdx.tiny.platform.WindowManager
import com.github.minigdx.tiny.render.RenderContext

data class OpenGLRenderContext(
    val fbo: Framebuffer,
    val fboBuffer: ByteBuffer,
    val windowManager: WindowManager,
    val fboTexture: Texture,
) : RenderContext
