package com.github.minigdx.tiny.render.gl

import com.danielgergely.kgl.ByteBuffer
import com.danielgergely.kgl.GL_BLEND
import com.danielgergely.kgl.GL_COLOR_ATTACHMENT0
import com.danielgergely.kgl.GL_DEPTH24_STENCIL8
import com.danielgergely.kgl.GL_FRAMEBUFFER
import com.danielgergely.kgl.GL_FRAMEBUFFER_COMPLETE
import com.danielgergely.kgl.GL_NEAREST
import com.danielgergely.kgl.GL_RENDERBUFFER
import com.danielgergely.kgl.GL_RGBA
import com.danielgergely.kgl.GL_SCISSOR_TEST
import com.danielgergely.kgl.GL_STENCIL_ATTACHMENT
import com.danielgergely.kgl.GL_TEXTURE_2D
import com.danielgergely.kgl.GL_TEXTURE_MAG_FILTER
import com.danielgergely.kgl.GL_TEXTURE_MIN_FILTER
import com.danielgergely.kgl.GL_TRIANGLES
import com.danielgergely.kgl.GL_UNSIGNED_BYTE
import com.danielgergely.kgl.Kgl
import com.github.minigdx.tiny.engine.GameOptions
import com.github.minigdx.tiny.graphic.Clipper
import com.github.minigdx.tiny.graphic.PixelFormat
import com.github.minigdx.tiny.platform.performance.PerformanceMonitor
import com.github.minigdx.tiny.render.RenderFrame
import com.github.minigdx.tiny.render.batch.SpriteBatch
import com.github.minigdx.tiny.render.shader.FragmentShader
import com.github.minigdx.tiny.render.shader.ShaderProgram
import com.github.minigdx.tiny.render.shader.VertexShader
import com.github.minigdx.tiny.resources.SpriteSheet

class SpriteBatchStage(
    gl: Kgl,
    private val gameOptions: GameOptions,
    private val performanceMonitor: PerformanceMonitor,
) {
    class SpriteBatchState private constructor(
        var dither: Int = 0xFFFF,
        val clipper: Clipper,
    ) {
        constructor(gameOptions: GameOptions) : this(0XFFFF, Clipper(gameOptions.width, gameOptions.height))
    }

    lateinit var frameBufferContext: FrameBufferContext

    private val program = ShaderProgram(gl, VShader(), FShader())

    private val spriteBatchState = SpriteBatchState(gameOptions)

    fun init() {
        program.compileShader()

        // Framebuffer of the size of the screen
        val frameBufferData = ByteBuffer(gameOptions.width * gameOptions.height * PixelFormat.RGBA)

        // Attach stencil buffer to the framebuffer.
        val stencilBuffer = program.createRenderbuffer()
        program.bindRenderbuffer(GL_RENDERBUFFER, stencilBuffer)
        program.renderbufferStorage(GL_RENDERBUFFER, GL_DEPTH24_STENCIL8, gameOptions.width, gameOptions.height)

        val frameBuffer = program.createFramebuffer()
        program.bindFramebuffer(GL_FRAMEBUFFER, frameBuffer)

        program.framebufferRenderbuffer(GL_FRAMEBUFFER, GL_STENCIL_ATTACHMENT, GL_RENDERBUFFER, stencilBuffer)

        // Prepare the texture used for the FBO
        val frameBufferTexture = program.createTexture()
        program.bindTexture(GL_TEXTURE_2D, frameBufferTexture)

        program.texImage2D(
            GL_TEXTURE_2D,
            0,
            GL_RGBA,
            gameOptions.width,
            gameOptions.height,
            0,
            GL_RGBA,
            GL_UNSIGNED_BYTE,
            frameBufferData,
        )
        program.texParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_NEAREST)
        program.texParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_NEAREST)
        program.framebufferTexture2D(GL_FRAMEBUFFER, GL_COLOR_ATTACHMENT0, GL_TEXTURE_2D, frameBufferTexture, 0)

        if (program.checkFramebufferStatus(GL_FRAMEBUFFER) != GL_FRAMEBUFFER_COMPLETE) {
            throw IllegalStateException("Framebuffer is NOT complete!")
        }

        frameBufferContext = FrameBufferContext(
            frameBufferTexture = frameBufferTexture,
            frameBuffer = frameBuffer,
            frameBufferData = frameBufferData,
        )

        program.enable(GL_BLEND)

        program.disable(GL_SCISSOR_TEST)
        program.bindTexture(GL_TEXTURE_2D, null)
        program.bindFramebuffer(GL_FRAMEBUFFER, null)
    }


    fun bindTextures(spritesheets: List<SpriteSheet>) {

    }

    fun startStage() {
        program.use()
        program.disable(GL_SCISSOR_TEST)
        program.bindFramebuffer(GL_FRAMEBUFFER, frameBufferContext.frameBuffer)
        // program.enable(GL_SCISSOR_TEST)

        program.viewport(0, 0, gameOptions.width, gameOptions.height)
    }

    fun endStage() {
        program.disable(GL_SCISSOR_TEST)
        program.bindFramebuffer(GL_FRAMEBUFFER, null)
    }

    fun execute(batch: SpriteBatch) {

        program.setup { vertexShader, fragmentShader ->
            vertexShader.aPos.apply(batch.vertex)
            vertexShader.uViewport.apply(
                gameOptions.width.toFloat(),
                // Flip the vertical
                gameOptions.height.toFloat() * -1,
            )
        }

        program.bind()
        program.drawArrays(GL_TRIANGLES, 0, batch.numberOfVertex)
        performanceMonitor.drawCall(batch.numberOfVertex)
        program.unbind()
    }

    fun readFrameBuffer(): RenderFrame {
        program.bindFramebuffer(GL_FRAMEBUFFER, frameBufferContext.frameBuffer)

        program.readPixels(
            0,
            0,
            gameOptions.width,
            gameOptions.height,
            GL_RGBA,
            GL_UNSIGNED_BYTE,
            frameBufferContext.frameBufferData,
        )

        program.bindFramebuffer(GL_FRAMEBUFFER, null)

        val openGLFrame = OpenGLFrame(frameBufferContext.frameBufferData, gameOptions)

        return openGLFrame
    }

    class VShader : VertexShader(VERTEX_SHADER) {
        val aPos = inVec2("a_pos") // position of the sprite in the viewport
        val uViewport = uniformVec2("u_viewport") // Size of the viewport; in pixel.
    }

    class FShader : FragmentShader(FRAGMENT_SHADER) {
    }

    companion object {
        private const val VERTEX_PER_SPRITE = 6
        // 12 floats are required to generate coordinates for a sprite.
        private const val FLOAT_PER_SPRITE = VERTEX_PER_SPRITE * 2

        //language=Glsl
        private val VERTEX_SHADER =
            """
            void main() {
                gl_Position = vec4((a_pos / u_viewport) * 2.0 - 1.0, 0.0, 1.0);
            }
            """.trimIndent()

        //language=Glsl
        private val FRAGMENT_SHADER =
            """
            void main() {
                fragColor = vec4(0.0, 1.0, 0.0, 1.0);
                
            }
            """.trimIndent()
    }
}
