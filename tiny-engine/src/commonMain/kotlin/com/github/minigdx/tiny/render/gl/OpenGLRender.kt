package com.github.minigdx.tiny.render.gl

import com.danielgergely.kgl.ByteBuffer
import com.danielgergely.kgl.GL_COLOR_ATTACHMENT0
import com.danielgergely.kgl.GL_COLOR_BUFFER_BIT
import com.danielgergely.kgl.GL_DEPTH24_STENCIL8
import com.danielgergely.kgl.GL_DEPTH_BUFFER_BIT
import com.danielgergely.kgl.GL_FRAMEBUFFER
import com.danielgergely.kgl.GL_FRAMEBUFFER_COMPLETE
import com.danielgergely.kgl.GL_NEAREST
import com.danielgergely.kgl.GL_RENDERBUFFER
import com.danielgergely.kgl.GL_RGBA
import com.danielgergely.kgl.GL_STENCIL_ATTACHMENT
import com.danielgergely.kgl.GL_STENCIL_BUFFER_BIT
import com.danielgergely.kgl.GL_TEXTURE_2D
import com.danielgergely.kgl.GL_TEXTURE_MAG_FILTER
import com.danielgergely.kgl.GL_TEXTURE_MIN_FILTER
import com.danielgergely.kgl.GL_UNSIGNED_BYTE
import com.danielgergely.kgl.Kgl
import com.github.minigdx.tiny.engine.GameOptions
import com.github.minigdx.tiny.graphic.PixelArray
import com.github.minigdx.tiny.graphic.PixelFormat
import com.github.minigdx.tiny.log.Logger
import com.github.minigdx.tiny.platform.WindowManager
import com.github.minigdx.tiny.render.Render
import com.github.minigdx.tiny.render.RenderContext
import com.github.minigdx.tiny.render.RenderFrame
import com.github.minigdx.tiny.render.operations.RenderOperation

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

        val onscreen = createNewFrameBuffer()
        val offscreen = createNewFrameBuffer()

        return OpenGLRenderContext(
            windowManager = windowManager,
            currentFrameBuffer = onscreen,
            onscreenFrameBuffer = onscreen,
            offscreenFrameBuffer = offscreen,
        )
    }

    private fun createNewFrameBuffer(): FrameBufferContext {
        // Framebuffer of the size of the screen
        val fboBuffer = ByteBuffer(gameOptions.width * gameOptions.height * PixelFormat.RGBA)

        // Attach stencil buffer to the framebuffer.
        val stencilBuffer = gl.createRenderbuffer()
        gl.bindRenderbuffer(GL_RENDERBUFFER, stencilBuffer)
        gl.renderbufferStorage(GL_RENDERBUFFER, GL_DEPTH24_STENCIL8, gameOptions.width, gameOptions.height)

        val fbo = gl.createFramebuffer()
        gl.bindFramebuffer(GL_FRAMEBUFFER, fbo)

        gl.framebufferRenderbuffer(GL_FRAMEBUFFER, GL_STENCIL_ATTACHMENT, GL_RENDERBUFFER, stencilBuffer)

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
        return FrameBufferContext(fbo, fboBuffer, fboTexture)
    }

    override fun render(
        context: RenderContext,
        ops: List<RenderOperation>,
    ) {
        context as OpenGLRenderContext

        gl.bindFramebuffer(GL_FRAMEBUFFER, context.fbo)

        gl.viewport(0, 0, gameOptions.width, gameOptions.height)

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

    override fun executeOffScreen(
        context: RenderContext,
        block: () -> Unit,
    ): RenderFrame {
        context as OpenGLRenderContext
        context.useOffscreen()
        gl.bindFramebuffer(GL_FRAMEBUFFER, context.fbo)

        gl.clearColor(0f, 0f, 0f, 0f)
        gl.clear(GL_COLOR_BUFFER_BIT or GL_DEPTH_BUFFER_BIT or GL_STENCIL_BUFFER_BIT)

        gl.bindFramebuffer(GL_FRAMEBUFFER, null)

        block()
        val frame = readRender(context)

        context.useOnscreen()

        return frame
    }
}
