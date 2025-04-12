package com.github.minigdx.tiny.render

import com.danielgergely.kgl.ByteBuffer
import com.danielgergely.kgl.Framebuffer
import com.danielgergely.kgl.GL_BLEND
import com.danielgergely.kgl.GL_COLOR_ATTACHMENT0
import com.danielgergely.kgl.GL_FRAMEBUFFER
import com.danielgergely.kgl.GL_FRAMEBUFFER_COMPLETE
import com.danielgergely.kgl.GL_LINEAR
import com.danielgergely.kgl.GL_RGBA
import com.danielgergely.kgl.GL_TEXTURE_2D
import com.danielgergely.kgl.GL_TEXTURE_MAG_FILTER
import com.danielgergely.kgl.GL_TEXTURE_MIN_FILTER
import com.danielgergely.kgl.GL_UNSIGNED_BYTE
import com.danielgergely.kgl.Kgl
import com.github.minigdx.tiny.Pixel
import com.github.minigdx.tiny.engine.GameOptions
import com.github.minigdx.tiny.engine.RenderOperation
import com.github.minigdx.tiny.graphic.FrameBuffer
import com.github.minigdx.tiny.graphic.PixelFormat
import com.github.minigdx.tiny.log.Logger
import com.github.minigdx.tiny.platform.WindowManager

class OpenGLRender(
    private val gl: Kgl,
    private val logger: Logger,
    private val gameOptions: GameOptions,
) : Render {

    data class OpenGLRenderContext(
        val cpuContext: OpenGLCPURenderContext,
        val gpuContext: OpenGLGPURenderContext,
        val fbo: Framebuffer,
        val fboBuffer: ByteBuffer,
        val windowManager: WindowManager,
    ) :
        RenderContext

    private val cpuRenderUnit = OpenGlCPURenderUnit(gl, logger, gameOptions)
    private val gpuRenderUnit = OpenGLGPURenderUnit(gl, logger, gameOptions)

    override fun init(windowManager: WindowManager): RenderContext {
        val cpuContext = cpuRenderUnit.init(windowManager)
        val gpuContext = gpuRenderUnit.init(windowManager)

        val fboBuffer = ByteBuffer(gameOptions.width * gameOptions.height * PixelFormat.RGBA)
        val fbo = gl.createFramebuffer()
        gl.usingFramebuffer(fbo) {
            // Prepare the texture used for the FBO
            val fboTexture = gl.createTexture()
            gl.usingTexture(fboTexture) {
                // Framebuffer of the size of the screen
                gl.texImage2D(
                    GL_TEXTURE_2D,
                    0,
                    GL_RGBA,
                    gameOptions.width,
                    gameOptions.height,
                    0,
                    GL_RGBA,
                    GL_UNSIGNED_BYTE,
                    fboBuffer,
                )
                gl.texParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_LINEAR)
                gl.texParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_LINEAR)
                gl.framebufferTexture2D(GL_FRAMEBUFFER, GL_COLOR_ATTACHMENT0, GL_TEXTURE_2D, fboTexture, 0)

                if (gl.checkFramebufferStatus(GL_FRAMEBUFFER) != GL_FRAMEBUFFER_COMPLETE) {
                    TODO("Framebuffer is NOT complete!")
                }
            }
        }
        return OpenGLRenderContext(
            cpuContext = cpuContext,
            gpuContext = gpuContext,
            windowManager = windowManager,
            fbo = fbo,
            fboBuffer = fboBuffer,
        )
    }

    override fun draw(
        context: RenderContext,
        image: ByteArray,
        width: Pixel,
        height: Pixel,
    ) {
        context as OpenGLRenderContext

        gl.viewport(
            gameOptions.gutter.first * gameOptions.zoom * context.windowManager.ratioWidth,
            gameOptions.gutter.second * gameOptions.zoom * context.windowManager.ratioHeight,
            gameOptions.width * gameOptions.zoom * context.windowManager.ratioWidth,
            gameOptions.height * gameOptions.zoom * context.windowManager.ratioHeight,
        )
        gl.disable(GL_BLEND)

        cpuRenderUnit.drawCPU(context.cpuContext, image, width, height)
    }

    override fun drawToFrameBuffer(
        context: RenderContext,
        frameBuffer: FrameBuffer,
        ops: List<RenderOperation>,
    ): FrameBuffer {
        context as OpenGLRenderContext

        val image = frameBuffer.generateBuffer()

        gl.usingFramebuffer(context.fbo) {
            gl.viewport(0, 0, frameBuffer.width, frameBuffer.height)
            gl.disable(GL_BLEND)
            cpuRenderUnit.drawCPU(context.cpuContext, image, frameBuffer.width, frameBuffer.height)
            gl.enable(GL_BLEND)
            gpuRenderUnit.drawGPU(context.gpuContext, ops)

            gl.readPixels(
                0,
                0,
                gameOptions.width,
                gameOptions.height,
                GL_RGBA,
                GL_UNSIGNED_BYTE,
                context.fboBuffer,
            )
        }
        val openGLFrame =
            OpenGLFrame(
                buffer = context.fboBuffer,
                gameOptions = gameOptions,
            )
        val toFrameBuffer = openGLFrame.toFrameBuffer()
        return toFrameBuffer
    }

}
