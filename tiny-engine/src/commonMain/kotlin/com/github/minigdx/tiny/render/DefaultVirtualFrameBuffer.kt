package com.github.minigdx.tiny.render

import com.danielgergely.kgl.ByteBuffer
import com.danielgergely.kgl.GL_BLEND
import com.danielgergely.kgl.GL_COLOR_ATTACHMENT0
import com.danielgergely.kgl.GL_COLOR_BUFFER_BIT
import com.danielgergely.kgl.GL_DEPTH24_STENCIL8
import com.danielgergely.kgl.GL_DEPTH_BUFFER_BIT
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
import com.danielgergely.kgl.GL_UNSIGNED_BYTE
import com.danielgergely.kgl.Kgl
import com.github.minigdx.tiny.ColorIndex
import com.github.minigdx.tiny.Pixel
import com.github.minigdx.tiny.engine.GameOptions
import com.github.minigdx.tiny.graphic.FrameBuffer
import com.github.minigdx.tiny.graphic.PixelFormat
import com.github.minigdx.tiny.platform.WindowManager
import com.github.minigdx.tiny.platform.performance.PerformanceMonitor
import com.github.minigdx.tiny.render.batch.BatchManager
import com.github.minigdx.tiny.render.batch.PrimitiveBatch
import com.github.minigdx.tiny.render.batch.PrimitiveInstance
import com.github.minigdx.tiny.render.batch.PrimitiveKey
import com.github.minigdx.tiny.render.batch.SpriteBatch
import com.github.minigdx.tiny.render.batch.SpriteBatchInstance
import com.github.minigdx.tiny.render.batch.SpriteBatchKey
import com.github.minigdx.tiny.render.gl.FrameBufferContext
import com.github.minigdx.tiny.render.gl.FrameBufferStage
import com.github.minigdx.tiny.render.gl.OpenGLFrame
import com.github.minigdx.tiny.render.gl.PrimitiveBatchStage
import com.github.minigdx.tiny.render.gl.SpriteBatchStage
import com.github.minigdx.tiny.resources.SpriteSheet

// TODO list:
// 4. bien tester et comprendre comment ça marche.
// 5. appliquer instanciacing sur SpriteBatchStage aussi.
// 6. Appliquer dithering et voilà !!!
class DefaultVirtualFrameBuffer(
    private val kgl: Kgl,
    private val gameOptions: GameOptions,
    private val performanceMonitor: PerformanceMonitor,
) : VirtualFrameBuffer {
    lateinit var frameBufferContext: FrameBufferContext

    private val spriteBatchStage = SpriteBatchStage(kgl, gameOptions, performanceMonitor)
    private val primitiveBatchStage = PrimitiveBatchStage(kgl, gameOptions, performanceMonitor)
    private val frameBufferStage = FrameBufferStage(kgl, gameOptions, performanceMonitor)

    private val spriteBatchManager = BatchManager(
        keyGenerator = { SpriteBatchKey() },
        instanceGenerator = { SpriteBatchInstance() },
        batchGenerator = { SpriteBatch() },
    )

    private val primitiveBatchManager = BatchManager(
        // TODO: for primitive, with instanciating -> one key for all?
        keyGenerator = { PrimitiveKey() },
        instanceGenerator = { PrimitiveInstance() },
        batchGenerator = { PrimitiveBatch() },
    )

    private val primitiveBuffer = FrameBuffer(
        gameOptions.width,
        gameOptions.height,
        gameOptions.colors(),
    )

    private val monocolors = createFontPalettes()

    override fun init(windowManager: WindowManager) {
        spriteBatchStage.init()
        primitiveBatchStage.init()
        frameBufferStage.init(windowManager)

        // Framebuffer of the size of the screen
        val frameBufferData = ByteBuffer(gameOptions.width * gameOptions.height * PixelFormat.RGBA)

        // Attach stencil buffer to the framebuffer.
        val stencilBuffer = kgl.createRenderbuffer()
        kgl.bindRenderbuffer(GL_RENDERBUFFER, stencilBuffer)
        kgl.renderbufferStorage(GL_RENDERBUFFER, GL_DEPTH24_STENCIL8, gameOptions.width, gameOptions.height)

        val frameBuffer = kgl.createFramebuffer()
        kgl.bindFramebuffer(GL_FRAMEBUFFER, frameBuffer)

        kgl.framebufferRenderbuffer(GL_FRAMEBUFFER, GL_STENCIL_ATTACHMENT, GL_RENDERBUFFER, stencilBuffer)

        // Prepare the texture used for the FBO
        val frameBufferTexture = kgl.createTexture()
        kgl.bindTexture(GL_TEXTURE_2D, frameBufferTexture)

        kgl.texImage2D(
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
        kgl.texParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_NEAREST)
        kgl.texParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_NEAREST)
        kgl.framebufferTexture2D(GL_FRAMEBUFFER, GL_COLOR_ATTACHMENT0, GL_TEXTURE_2D, frameBufferTexture, 0)

        if (kgl.checkFramebufferStatus(GL_FRAMEBUFFER) != GL_FRAMEBUFFER_COMPLETE) {
            throw IllegalStateException("Framebuffer is NOT complete!")
        }

        frameBufferContext = FrameBufferContext(
            frameBufferTexture = frameBufferTexture,
            frameBuffer = frameBuffer,
            frameBufferData = frameBufferData,
        )

        kgl.enable(GL_BLEND)

        kgl.disable(GL_SCISSOR_TEST)
        kgl.bindTexture(GL_TEXTURE_2D, null)
        kgl.bindFramebuffer(GL_FRAMEBUFFER, null)
    }

    private fun createFontPalettes(): Array<Array<ColorIndex>> {
        return (0 until gameOptions.colors().size).map { index ->
            val palette = Array(gameOptions.colors().size) { index }
            palette[0] = 0 // Set the transparent color
            palette
        }.toTypedArray()
    }

    override fun drawMonocolor(
        source: SpriteSheet,
        color: ColorIndex,
        sourceX: Pixel,
        sourceY: Pixel,
        sourceWidth: Pixel,
        sourceHeight: Pixel,
        destinationX: Pixel,
        destinationY: Pixel,
        flipX: Boolean,
        flipY: Boolean,
    ) {
        val key = spriteBatchManager.createKey()
        key.set(
            source,
            primitiveBuffer.blender.dithering,
            monocolors[color],
            primitiveBuffer.camera,
            primitiveBuffer.clipper,
        )
        val instance = spriteBatchManager.createInstance()
        instance.set(
            sourceX,
            sourceY,
            sourceWidth,
            sourceHeight,
            destinationX,
            destinationY,
            flipX,
            flipY,
        )
        spriteBatchManager.submit(key, instance)
    }

    override fun draw(
        source: SpriteSheet,
        sourceX: Pixel,
        sourceY: Pixel,
        sourceWidth: Pixel,
        sourceHeight: Pixel,
        destinationX: Pixel,
        destinationY: Pixel,
        flipX: Boolean,
        flipY: Boolean,
    ) {
        val key = spriteBatchManager.createKey()
        key.set(
            source,
            primitiveBuffer.blender.dithering,
            // TODO: changing pal might not be working as the array is the same.
            primitiveBuffer.blender.switch,
            primitiveBuffer.camera,
            primitiveBuffer.clipper,
        )
        val instance = spriteBatchManager.createInstance()
        instance.set(
            sourceX,
            sourceY,
            sourceWidth,
            sourceHeight,
            destinationX,
            destinationY,
            flipX,
            flipY,
        )
        spriteBatchManager.submit(key, instance)
    }

    override fun drawPrimitive(block: (FrameBuffer) -> Unit) {
        // FIXME: TODO
    }

    override fun drawRectf(
        x: Pixel,
        y: Pixel,
        width: Pixel,
        height: Pixel,
        colorIndex: ColorIndex,
        filled: Boolean,
    ) {
        val key = primitiveBatchManager.createKey().set(colorIndex)
        val instance = primitiveBatchManager.createInstance().setRect(
            x,
            y,
            width,
            height,
            filled = filled,
        )
        primitiveBatchManager.submit(key, instance)
    }

    private fun renderAllInFrameBuffer() {
        kgl.bindFramebuffer(GL_FRAMEBUFFER, frameBufferContext.frameBuffer)
        kgl.viewport(0, 0, gameOptions.width, gameOptions.height)

        spriteBatchStage.startStage()
        spriteBatchManager.consumeAllBatches { key, batch ->
            spriteBatchStage.execute(key, batch)
        }
        spriteBatchStage.endStage()

        primitiveBatchStage.startStage()
        primitiveBatchManager.consumeAllBatches { key, batch ->
            primitiveBatchStage.execute(key, batch)
        }
        primitiveBatchStage.endStage()

        kgl.bindFramebuffer(GL_FRAMEBUFFER, null)
    }

    override fun draw() {
        renderAllInFrameBuffer()
        frameBufferStage.execute(frameBufferContext.frameBufferTexture)
    }

    override fun bindTextures(spritesheetToBind: List<SpriteSheet>) {
        spriteBatchStage.bindTextures(spritesheetToBind)
    }

    override fun readFrameBuffer(): RenderFrame {
        kgl.bindFramebuffer(GL_FRAMEBUFFER, frameBufferContext.frameBuffer)

        kgl.readPixels(
            0,
            0,
            gameOptions.width,
            gameOptions.height,
            GL_RGBA,
            GL_UNSIGNED_BYTE,
            frameBufferContext.frameBufferData,
        )

        kgl.bindFramebuffer(GL_FRAMEBUFFER, null)

        val openGLFrame = OpenGLFrame(frameBufferContext.frameBufferData, gameOptions)

        return openGLFrame
    }

    override fun clear(color: ColorIndex) {
        spriteBatchManager.clear()
        primitiveBatchManager.clear()
        kgl.bindFramebuffer(GL_FRAMEBUFFER, frameBufferContext.frameBuffer)
        val (r, g, b) = gameOptions.colors().getRGBA(color)
        kgl.clearColor(r.toInt() / 255f, g.toInt() / 255f, b.toInt() / 255f, 1.0f)
        kgl.clear(GL_COLOR_BUFFER_BIT or GL_DEPTH_BUFFER_BIT)
        kgl.bindFramebuffer(GL_FRAMEBUFFER, null)
    }
}
