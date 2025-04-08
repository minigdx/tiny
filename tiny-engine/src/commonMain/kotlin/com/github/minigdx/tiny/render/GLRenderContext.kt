package com.github.minigdx.tiny.render

import com.danielgergely.kgl.ByteBuffer
import com.danielgergely.kgl.Framebuffer
import com.danielgergely.kgl.Program
import com.danielgergely.kgl.Texture
import com.github.minigdx.tiny.platform.RenderContext
import com.github.minigdx.tiny.platform.WindowManager

class GLRenderContext(
    val program: Program,
    val gameTexture: Texture,
    val colorPalette: Texture,
    val fbo: Framebuffer,
    val fboBuffer: ByteBuffer,
    val windowManager: WindowManager,
) : RenderContext
