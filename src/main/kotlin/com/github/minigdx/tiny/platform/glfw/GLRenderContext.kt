package com.github.minigdx.tiny.platform.glfw

import com.danielgergely.kgl.Program
import com.danielgergely.kgl.Texture
import com.github.minigdx.tiny.platform.RenderContext

class GLRenderContext(
    val program: Program,
    val texture: Texture,
) : RenderContext
