package com.github.minigdx.tiny.render

import com.github.minigdx.tiny.platform.WindowManager
import com.github.minigdx.tiny.render.shader.ShaderProgram

class OpenGLCPURenderContext(
    val windowManager: WindowManager,
    val shaderProgram: ShaderProgram<OpenGLCPURenderUnit.VShader, OpenGLCPURenderUnit.FShader>,
) : CPURenderContext
