package com.github.minigdx.tiny.render

import com.danielgergely.kgl.Framebuffer
import com.danielgergely.kgl.GL_COMPILE_STATUS
import com.danielgergely.kgl.GL_FALSE
import com.danielgergely.kgl.GL_FRAMEBUFFER
import com.danielgergely.kgl.GL_TEXTURE_2D
import com.danielgergely.kgl.Kgl
import com.danielgergely.kgl.Shader
import com.danielgergely.kgl.Texture
import com.github.minigdx.tiny.Pixel
import com.github.minigdx.tiny.engine.GameOptions
import com.github.minigdx.tiny.engine.RenderOperation
import com.github.minigdx.tiny.engine.RenderUnit
import com.github.minigdx.tiny.log.Logger
import com.github.minigdx.tiny.platform.WindowManager

abstract class RendererUnit<CONTEXT : RenderContext>(
    protected val gl: Kgl,
    protected val logger: Logger,
    protected val gameOptions: GameOptions,
) {
    private fun invalidRendererUnit(renderUnit: RenderUnit): Nothing =
        throw IllegalStateException(
            "RendererUnit doesn't support $renderUnit rendering",
        )

    protected fun createShader(
        shader: String,
        shaderType: Int,
    ): Shader {
        fun addLineNumbers(text: String): String {
            val lines = text.lines()
            val lineNumberWidth = lines.size.toString().length
            return lines.mapIndexed { index, line ->
                val lineNumber = (index + 1).toString().padStart(lineNumberWidth, ' ')
                "$lineNumber: $line"
            }.joinToString("\n")
        }

        val vertexShaderId = gl.createShader(shaderType)!!
        gl.shaderSource(vertexShaderId, shader)
        gl.compileShader(vertexShaderId)
        if (gl.getShaderParameter(vertexShaderId, GL_COMPILE_STATUS) == GL_FALSE) {
            val log = gl.getShaderInfoLog(vertexShaderId)
            gl.deleteShader(vertexShaderId)
            throw RuntimeException(
                "Shader compilation error: $log \n" +
                    "---------- \n" +
                    "Shader code in error: \n" +
                    addLineNumbers(shader),
            )
        }
        return vertexShaderId
    }

    abstract fun init(windowManager: WindowManager): CONTEXT

    open fun drawGPU(
        context: CONTEXT,
        ops: List<RenderOperation>,
    ): Unit = invalidRendererUnit(RenderUnit.GPU)

    open fun drawCPU(
        context: CONTEXT,
        image: ByteArray,
        width: Pixel,
        height: Pixel,
    ): Unit = invalidRendererUnit(RenderUnit.CPU)

}
