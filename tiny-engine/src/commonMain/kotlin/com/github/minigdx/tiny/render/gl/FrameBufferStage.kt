package com.github.minigdx.tiny.render.gl

import com.danielgergely.kgl.GL_BLEND
import com.danielgergely.kgl.GL_COLOR_BUFFER_BIT
import com.danielgergely.kgl.GL_DEPTH_BUFFER_BIT
import com.danielgergely.kgl.GL_FRAMEBUFFER
import com.danielgergely.kgl.GL_TRIANGLES
import com.danielgergely.kgl.Kgl
import com.github.minigdx.tiny.engine.GameOptions
import com.github.minigdx.tiny.platform.WindowManager
import com.github.minigdx.tiny.platform.performance.PerformanceMonitor
import com.github.minigdx.tiny.render.shader.FragmentShader
import com.github.minigdx.tiny.render.shader.ShaderProgram
import com.github.minigdx.tiny.render.shader.VertexShader

class FrameBufferStage(
    gl: Kgl,
    private val gameOptions: GameOptions,
    private val performanceMonitor: PerformanceMonitor,
) {
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

    private lateinit var windowManager: WindowManager

    fun init(windowManager: WindowManager) {
        program.compileShader()

        program.enable(GL_BLEND)

        this.windowManager = windowManager
    }

    fun execute(stage: SpriteBatchStage) {
        program.use()
        program.bindFramebuffer(GL_FRAMEBUFFER, null)

        program.viewport(
            gameOptions.gutter.first * gameOptions.zoom * windowManager.ratioWidth,
            gameOptions.gutter.second * gameOptions.zoom * windowManager.ratioHeight,
            gameOptions.width * gameOptions.zoom * windowManager.ratioWidth,
            gameOptions.height * gameOptions.zoom * windowManager.ratioHeight,
        )

        program.clearColor(1f, 0f, 0f, 1.0f)
        program.clear(GL_COLOR_BUFFER_BIT or GL_DEPTH_BUFFER_BIT)

        program.setup { vertexShader, fragmentShader ->
            vertexShader.position.apply(vertexData)
            vertexShader.uvs.apply(uvsData)
            fragmentShader.frameBuffer.applyTexture(stage.frameBufferContext.frameBufferTexture)
        }

        program.bind()

        val nbVertex = 3
        program.drawArrays(GL_TRIANGLES, 0, nbVertex)
        performanceMonitor.drawCall(nbVertex)
        performanceMonitor.drawOnScreen()

        program.unbind()
    }

    class VShader : VertexShader(VERTEX_SHADER) {
        val position = inVec2("position")
        val uvs = inVec2("uvs")
        val viewport = outVec2("viewport")
    }

    class FShader : FragmentShader(FRAGMENT_SHADER) {
        val viewport = inVec2("viewport")
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
                fragColor = texture(frame_buffer, viewport);
            }
            """.trimIndent()
    }
}
