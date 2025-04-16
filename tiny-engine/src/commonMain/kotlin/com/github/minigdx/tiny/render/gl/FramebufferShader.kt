package com.github.minigdx.tiny.render.gl

import com.danielgergely.kgl.GL_COLOR_BUFFER_BIT
import com.danielgergely.kgl.GL_DEPTH_BUFFER_BIT
import com.danielgergely.kgl.GL_TRIANGLES
import com.danielgergely.kgl.Kgl
import com.github.minigdx.tiny.engine.GameOptions
import com.github.minigdx.tiny.engine.RenderOperation
import com.github.minigdx.tiny.log.Logger
import com.github.minigdx.tiny.platform.WindowManager
import com.github.minigdx.tiny.render.NopRenderContext
import com.github.minigdx.tiny.render.RenderContext
import com.github.minigdx.tiny.render.WriteRender
import com.github.minigdx.tiny.render.shader.FragmentShader
import com.github.minigdx.tiny.render.shader.ShaderProgram
import com.github.minigdx.tiny.render.shader.VertexShader

class FramebufferShader(val gl: Kgl, val logger: Logger, val gameOptions: GameOptions) : WriteRender {
    private val uvsData =
        floatArrayOf(
            // right/bottom
            2f,
            -1f,
            // left/up
            0f,
            1f,
            // left/bottom
            0f,
            -1f,
        )

    private val vertexData =
        floatArrayOf(
            // bottom right
            3f,
            -1f,
            // top left
            -1f,
            3f,
            // bottom left
            -1f,
            -1f,
        )

    private val program = ShaderProgram(gl, VShader(), FShader())

    override fun init(windowManager: WindowManager): RenderContext {
        program.compileShader()
        return NopRenderContext
    }

    override fun render(
        context: RenderContext,
        ops: List<RenderOperation>,
    ) = Unit

    override fun drawOnScreen(context: RenderContext) {
        context as OpenGLRenderContext

        program.use()

        program.vertexShader.position.apply(vertexData)
        program.vertexShader.uvs.apply(uvsData)
        program.fragmentShader.frameBuffer.applyTexture(context.fboTexture)

        program.bind()

        program.clear(GL_COLOR_BUFFER_BIT or GL_DEPTH_BUFFER_BIT)
        program.clearColor(0f, 0f, 0f, 1.0f)

        program.drawArrays(GL_TRIANGLES, 0, 3)

        program.unbind()
    }

    class VShader : VertexShader(VERTEX_SHADER) {
        val position = attributeVec2("position")
        val uvs = attributeVec2("uvs")
        val viewport = varyingVec2("viewport")
    }

    class FShader : FragmentShader(FRAGMENT_SHADER) {
        val viewport = varyingVec2("viewport")
        val frameBuffer = uniformSample2D("frame_buffer", existingTexture = true)
    }

    companion object {
        //language=Glsl
        val VERTEX_SHADER =
            """
            void main() {
                gl_Position = vec4(position, 0.0, 1.0);
                viewport = uvs;
            }
            """.trimIndent()

        //language=Glsl
        val FRAGMENT_SHADER =
            """
            void main() {
                gl_FragColor = texture2D(frame_buffer, viewport);
            }
            """.trimIndent()
    }
}
