package com.github.minigdx.tiny.render.gl

import com.danielgergely.kgl.ByteBuffer
import com.danielgergely.kgl.GL_BLEND
import com.danielgergely.kgl.GL_COLOR_ATTACHMENT0
import com.danielgergely.kgl.GL_FRAMEBUFFER
import com.danielgergely.kgl.GL_FRAMEBUFFER_COMPLETE
import com.danielgergely.kgl.GL_NEAREST
import com.danielgergely.kgl.GL_ONE_MINUS_SRC_ALPHA
import com.danielgergely.kgl.GL_RGBA
import com.danielgergely.kgl.GL_SRC_ALPHA
import com.danielgergely.kgl.GL_TEXTURE_2D
import com.danielgergely.kgl.GL_TEXTURE_MAG_FILTER
import com.danielgergely.kgl.GL_TEXTURE_MIN_FILTER
import com.danielgergely.kgl.GL_UNSIGNED_BYTE
import com.danielgergely.kgl.Kgl
import com.github.minigdx.tiny.engine.GameOptions
import com.github.minigdx.tiny.engine.RenderOperation
import com.github.minigdx.tiny.graphic.PixelArray
import com.github.minigdx.tiny.graphic.PixelFormat
import com.github.minigdx.tiny.log.Logger
import com.github.minigdx.tiny.platform.WindowManager
import com.github.minigdx.tiny.render.Render
import com.github.minigdx.tiny.render.RenderContext
import com.github.minigdx.tiny.render.RenderFrame

class OpenGLRender(
    private val gl: Kgl,
    private val logger: Logger,
    private val gameOptions: GameOptions,
) : Render {
    private val operationsShader = OperationsShader(gl, logger, gameOptions)
    private val framebufferShader = FramebufferShader(gl, logger, gameOptions)

    override fun init(windowManager: WindowManager): RenderContext {
        operationsShader.init(windowManager)
        framebufferShader.init(windowManager)

        // Framebuffer of the size of the screen
        val fboBuffer = ByteBuffer(gameOptions.width * gameOptions.height * PixelFormat.RGBA)

        gl.enable(GL_BLEND)

        val fbo = gl.createFramebuffer()
        gl.bindFramebuffer(GL_FRAMEBUFFER, fbo)

        // Prepare the texture used for the FBO
        val fboTexture = gl.createTexture()
        gl.bindTexture(GL_TEXTURE_2D, fboTexture)

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
        gl.texParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_NEAREST)
        gl.texParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_NEAREST)
        gl.framebufferTexture2D(GL_FRAMEBUFFER, GL_COLOR_ATTACHMENT0, GL_TEXTURE_2D, fboTexture, 0)

        if (gl.checkFramebufferStatus(GL_FRAMEBUFFER) != GL_FRAMEBUFFER_COMPLETE) {
            throw IllegalStateException("Framebuffer is NOT complete!")
        }
        gl.bindTexture(GL_TEXTURE_2D, null)
        gl.bindFramebuffer(GL_FRAMEBUFFER, null)

        return OpenGLRenderContext(
            windowManager = windowManager,
            fbo = fbo,
            fboBuffer = fboBuffer,
        )
    }

    override fun render(
        context: RenderContext,
        ops: List<RenderOperation>,
    ) {
        context as OpenGLRenderContext

        gl.bindFramebuffer(GL_FRAMEBUFFER, context.fbo)

        gl.viewport(0, 0, gameOptions.width, gameOptions.height)
        gl.blendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA)

        operationsShader.render(context, ops)

        gl.bindFramebuffer(GL_FRAMEBUFFER, null)
    }

    override fun readRender(context: RenderContext): RenderFrame {
        context as OpenGLRenderContext

        gl.bindFramebuffer(GL_FRAMEBUFFER, context.fbo)

        gl.readPixels(
            0,
            0,
            gameOptions.width,
            gameOptions.height,
            GL_RGBA,
            GL_UNSIGNED_BYTE,
            context.fboBuffer,
        )

        gl.bindFramebuffer(GL_FRAMEBUFFER, null)

        return OpenGLFrame(context.fboBuffer, gameOptions)
    }

    override fun readRenderAsFrameBuffer(context: RenderContext): PixelArray {
        val pixelArray = PixelArray(gameOptions.width, gameOptions.height)
        readRender(context).copyInto(pixelArray)
        return pixelArray
    }

    override fun drawOnScreen(context: RenderContext) {
        context as OpenGLRenderContext

        gl.viewport(
            gameOptions.gutter.first * gameOptions.zoom * context.windowManager.ratioWidth,
            gameOptions.gutter.second * gameOptions.zoom * context.windowManager.ratioHeight,
            gameOptions.width * gameOptions.zoom * context.windowManager.ratioWidth,
            gameOptions.height * gameOptions.zoom * context.windowManager.ratioHeight,
        )

        framebufferShader.drawOnScreen(context)
    }
}
