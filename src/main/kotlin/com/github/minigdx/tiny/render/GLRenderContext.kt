package com.github.minigdx.tiny.render

import com.danielgergely.kgl.Program
import com.danielgergely.kgl.Texture
import com.github.minigdx.tiny.platform.RenderContext

class GLRenderContext(
    val program: Program,
    val texture: Texture,
) : RenderContext
