package com.github.minigdx.tiny.render

import com.danielgergely.kgl.GL_EXTENSIONS
import com.danielgergely.kgl.GL_RENDERER
import com.danielgergely.kgl.GL_SHADING_LANGUAGE_VERSION
import com.danielgergely.kgl.GL_VENDOR
import com.danielgergely.kgl.GL_VERSION
import com.danielgergely.kgl.KglLwjgl
import com.github.minigdx.tiny.Pixel
import com.github.minigdx.tiny.engine.GameOptions
import com.github.minigdx.tiny.engine.RenderOperation
import com.github.minigdx.tiny.graphic.FrameBuffer
import com.github.minigdx.tiny.log.Logger
import com.github.minigdx.tiny.platform.WindowManager
import org.lwjgl.opengl.GL33

class LwjglGLRender(
    private val logger: Logger,
    private val gameOptions: GameOptions,
    private val gl: OpenGLRender = OpenGLRender(KglLwjgl, logger, gameOptions),
) : Render {
    override fun init(windowManager: WindowManager): RenderContext {
        logger.info("GLFW") { "GL_VENDOR:                \t" + GL33.glGetString(GL_VENDOR) }
        logger.info("GLFW") { "GL_VERSION:               \t" + GL33.glGetString(GL_VERSION) }
        logger.info("GLFW") { "GL_RENDERER:              \t" + GL33.glGetString(GL_RENDERER) }
        logger.info("GLFW") { "SHADING_LANGUAGE_VERSION: \t" + GL33.glGetString(GL_SHADING_LANGUAGE_VERSION) }
        logger.info("GLFW") { "EXTENSIONS:               \t" + GL33.glGetString(GL_EXTENSIONS) }
        return gl.init(windowManager)
    }

    override fun draw(
        context: RenderContext,
        image: ByteArray,
        width: Pixel,
        height: Pixel,
    ) = gl.draw(context, image, width, height)

    override fun drawToFrameBuffer(
        context: RenderContext,
        frameBuffer: FrameBuffer,
        ops: List<RenderOperation>,
    ) = gl.drawToFrameBuffer(context, frameBuffer, ops)
}
