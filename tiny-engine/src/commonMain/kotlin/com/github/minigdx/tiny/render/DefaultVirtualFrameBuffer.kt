package com.github.minigdx.tiny.render

import com.danielgergely.kgl.ByteBuffer
import com.danielgergely.kgl.GL_ALWAYS
import com.danielgergely.kgl.GL_BLEND
import com.danielgergely.kgl.GL_COLOR_ATTACHMENT0
import com.danielgergely.kgl.GL_COLOR_BUFFER_BIT
import com.danielgergely.kgl.GL_DEPTH24_STENCIL8
import com.danielgergely.kgl.GL_DEPTH_ATTACHMENT
import com.danielgergely.kgl.GL_DEPTH_BUFFER_BIT
import com.danielgergely.kgl.GL_DEPTH_TEST
import com.danielgergely.kgl.GL_EQUAL
import com.danielgergely.kgl.GL_FRAMEBUFFER
import com.danielgergely.kgl.GL_FRAMEBUFFER_COMPLETE
import com.danielgergely.kgl.GL_KEEP
import com.danielgergely.kgl.GL_LEQUAL
import com.danielgergely.kgl.GL_NEAREST
import com.danielgergely.kgl.GL_NOTEQUAL
import com.danielgergely.kgl.GL_ONE
import com.danielgergely.kgl.GL_ONE_MINUS_SRC_ALPHA
import com.danielgergely.kgl.GL_RENDERBUFFER
import com.danielgergely.kgl.GL_REPLACE
import com.danielgergely.kgl.GL_RGBA
import com.danielgergely.kgl.GL_SCISSOR_TEST
import com.danielgergely.kgl.GL_SRC_ALPHA
import com.danielgergely.kgl.GL_STENCIL_ATTACHMENT
import com.danielgergely.kgl.GL_STENCIL_BUFFER_BIT
import com.danielgergely.kgl.GL_STENCIL_TEST
import com.danielgergely.kgl.GL_TEXTURE_2D
import com.danielgergely.kgl.GL_TEXTURE_MAG_FILTER
import com.danielgergely.kgl.GL_TEXTURE_MIN_FILTER
import com.danielgergely.kgl.GL_UNSIGNED_BYTE
import com.danielgergely.kgl.GL_ZERO
import com.danielgergely.kgl.Kgl
import com.github.minigdx.tiny.ColorIndex
import com.github.minigdx.tiny.Pixel
import com.github.minigdx.tiny.engine.GameOptions
import com.github.minigdx.tiny.graphic.FrameBufferParameters
import com.github.minigdx.tiny.graphic.PixelFormat
import com.github.minigdx.tiny.input.internal.ObjectPool
import com.github.minigdx.tiny.platform.DrawingMode
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
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

class DefaultVirtualFrameBuffer(
    private val kgl: Kgl,
    private val gameOptions: GameOptions,
    private val performanceMonitor: PerformanceMonitor,
) : VirtualFrameBuffer {
    lateinit var frameBufferContext: FrameBufferContext

    private val spriteBatchStage = SpriteBatchStage(kgl, gameOptions, performanceMonitor)
    private val primitiveBatchStage = PrimitiveBatchStage(kgl, gameOptions, performanceMonitor)
    private val frameBufferStage = FrameBufferStage(kgl, gameOptions, performanceMonitor)

    private var currentSpritesheet: SpriteSheet? = null
    private var currentDepth: Float = 1f

    private var cachedReadFrame: RenderFrame? = null

    private val spriteBatchManager = BatchManager(
        keyGenerator = { SpriteBatchKey() },
        instanceGenerator = { SpriteBatchInstance() },
        batchGenerator = { SpriteBatch() },
    )

    private val primitiveBatchManager = BatchManager(
        keyGenerator = { PrimitiveKey() },
        instanceGenerator = { PrimitiveInstance() },
        batchGenerator = { PrimitiveBatch() },
    )

    private val parameters = FrameBufferParameters(
        gameOptions.width,
        gameOptions.height,
        gameOptions.colors(),
    )

    private val monocolors = createFontPalettes()

    private val boundingBoxPool = object : ObjectPool<BoundingBox>(100) {
        override fun newInstance(): BoundingBox {
            return BoundingBox(0, 0, 0, 0)
        }

        override fun destroyInstance(obj: BoundingBox) {
            obj.reset()
        }
    }

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
        kgl.framebufferRenderbuffer(GL_FRAMEBUFFER, GL_DEPTH_ATTACHMENT, GL_RENDERBUFFER, stencilBuffer)

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
    ) = isInFrame(destinationX, destinationY, sourceWidth, sourceHeight) {
        invalidateCachedReadFrame()
        updateDepthIndex(source)

        val palColor = parameters.blender.color(color)

        val key = spriteBatchManager.createKey()
        key.set(
            source,
            parameters.blender.dithering,
            monocolors[palColor],
            parameters.clipper(),
        )
        val instance = spriteBatchManager.createInstance()
        instance.set(
            sourceX,
            sourceY,
            sourceWidth,
            sourceHeight,
            destinationX - parameters.camera.x,
            destinationY - parameters.camera.y,
            flipX,
            flipY,
            currentDepth,
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
    ) = isInFrame(destinationX, destinationY, sourceWidth, sourceHeight) {
        invalidateCachedReadFrame()
        updateDepthIndex(source)
        val key = spriteBatchManager.createKey()
        key.set(
            source,
            parameters.blender.dithering,
            parameters.blender.palette(),
            parameters.clipper(),
        )
        val instance = spriteBatchManager.createInstance()
        instance.set(
            sourceX,
            sourceY,
            sourceWidth,
            sourceHeight,
            destinationX - parameters.camera.x,
            destinationY - parameters.camera.y,
            flipX,
            flipY,
            currentDepth,
        )
        spriteBatchManager.submit(key, instance)
    }

    override fun drawRect(
        x: Pixel,
        y: Pixel,
        width: Pixel,
        height: Pixel,
        color: ColorIndex,
        filled: Boolean,
    ) = isInFrame(x, y, width, height) {
        invalidateCachedReadFrame()
        updateDepthIndex(null)
        val key = primitiveBatchManager.createKey().set(parameters.clipper())
        val instance = primitiveBatchManager.createInstance().setRect(
            x - parameters.camera.x,
            y - parameters.camera.y,
            width,
            height,
            filled = filled,
            color = parameters.blender.color(color),
            dither = parameters.blender.dithering,
            depth = currentDepth,
        )
        primitiveBatchManager.submit(key, instance)
    }

    override fun drawLine(
        x1: Pixel,
        y1: Pixel,
        x2: Pixel,
        y2: Pixel,
        color: ColorIndex,
    ) = isInFrame(
        x = min(x1, x2),
        y = min(y1, y2),
        width = abs(x2 - x1) + 1,
        height = abs(y2 - y1) + 1,
    ) {
        invalidateCachedReadFrame()
        updateDepthIndex(null)
        val key = primitiveBatchManager.createKey().set(parameters.clipper())
        val instance = primitiveBatchManager.createInstance().setLine(
            x1 - parameters.camera.x,
            y1 - parameters.camera.y,
            x2 - parameters.camera.x,
            y2 - parameters.camera.y,
            color = parameters.blender.color(color),
            dither = parameters.blender.dithering,
            depth = currentDepth,
        )
        primitiveBatchManager.submit(key, instance)
    }

    override fun drawCircle(
        centerX: Pixel,
        centerY: Pixel,
        radius: Pixel,
        color: ColorIndex,
        filled: Boolean,
    ) = isInFrame(centerX - radius, centerY - radius, radius * 2, radius * 2) {
        invalidateCachedReadFrame()
        updateDepthIndex(null)
        val key = primitiveBatchManager.createKey().set(parameters.clipper())
        val instance = primitiveBatchManager.createInstance().setCircle(
            centerX - parameters.camera.x,
            centerY - parameters.camera.y,
            radius,
            filled = filled,
            color = parameters.blender.color(color),
            dither = parameters.blender.dithering,
            depth = currentDepth,
        )
        primitiveBatchManager.submit(key, instance)
    }

    override fun drawPoint(
        x: Pixel,
        y: Pixel,
        color: ColorIndex,
    ) = isInFrame(x, y, 1, 1) {
        invalidateCachedReadFrame()
        updateDepthIndex(null)
        val key = primitiveBatchManager.createKey().set(parameters.clipper())
        val instance = primitiveBatchManager.createInstance().setPoint(
            x - parameters.camera.x,
            y - parameters.camera.y,
            color = parameters.blender.color(color),
            dither = parameters.blender.dithering,
            depth = currentDepth,
        )
        primitiveBatchManager.submit(key, instance)
    }

    override fun drawTriangle(
        x1: Pixel,
        y1: Pixel,
        x2: Pixel,
        y2: Pixel,
        x3: Pixel,
        y3: Pixel,
        color: ColorIndex,
        filled: Boolean,
    ) {
        val x = min(min(x1, x2), x3)
        val y = min(min(y1, y2), y3)
        val width = max(max(x1, x2), x3) - x
        val height = max(max(y1, y2), y3) - y
        isInFrame(
            x = x,
            y = y,
            width = width,
            height = height,
        ) {
            invalidateCachedReadFrame()
            updateDepthIndex(null)
            val key = primitiveBatchManager.createKey().set(parameters.clipper())
            val instance = primitiveBatchManager.createInstance().setTriangle(
                x1 - parameters.camera.x,
                y1 - parameters.camera.y,
                x2 - parameters.camera.x,
                y2 - parameters.camera.y,
                x3 - parameters.camera.x,
                y3 - parameters.camera.y,
                parameters.blender.color(color),
                parameters.blender.dithering,
                filled,
                depth = currentDepth,
            )
            primitiveBatchManager.submit(key, instance)
        }
    }

    private fun renderAllInFrameBuffer() {
        kgl.bindFramebuffer(GL_FRAMEBUFFER, frameBufferContext.frameBuffer)
        kgl.viewport(0, 0, gameOptions.width, gameOptions.height)
        kgl.enable(GL_DEPTH_TEST)
        // Allow elements with the same depth (ie: same sprite sheet) to be drawn on the previous element.
        kgl.depthFunc(GL_LEQUAL)
        // Clear the depth buffer, to not conflict with the previous element that was drawn.
        kgl.clear(GL_DEPTH_BUFFER_BIT)

        // kgl.depthFunc

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

        kgl.disable(GL_DEPTH_TEST)
        kgl.bindFramebuffer(GL_FRAMEBUFFER, null)

        currentDepth = 1f
        currentSpritesheet = null
    }

    override fun draw() {
        renderAllInFrameBuffer()
        frameBufferStage.execute(frameBufferContext.frameBufferTexture)
    }

    override fun bindTextures(spritesheetToBind: List<SpriteSheet>) {
        spriteBatchStage.bindTextures(spritesheetToBind)
    }

    override fun readFrameBuffer(): RenderFrame {
        fun read(): RenderFrame {
            renderAllInFrameBuffer()

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

            return OpenGLFrame(frameBufferContext.frameBufferData, gameOptions)
        }

        // return the cached frame. Otherwise, read it again.
        val frame = cachedReadFrame ?: read()
        cachedReadFrame = frame
        return frame
    }

    override fun dithering(dither: Int): Int {
        val actual = parameters.blender.dithering
        parameters.blender.dithering = dither
        return actual
    }

    override fun clear(color: ColorIndex) {
        invalidateCachedReadFrame()

        spriteBatchManager.clear()
        primitiveBatchManager.clear()
        kgl.bindFramebuffer(GL_FRAMEBUFFER, frameBufferContext.frameBuffer)
        val (r, g, b) = gameOptions.colors().getRGBA(color)
        kgl.clearColor(r.toUByte().toInt() / 255f, g.toUByte().toInt() / 255f, b.toUByte().toInt() / 255f, 1.0f)
        kgl.clear(GL_COLOR_BUFFER_BIT or GL_DEPTH_BUFFER_BIT)
        kgl.bindFramebuffer(GL_FRAMEBUFFER, null)
    }

    private fun invalidateCachedReadFrame() {
        // Invalidate the cached frame
        cachedReadFrame = null
    }

    private fun updateDepthIndex(spritesheet: SpriteSheet?) {
        // increment depth only when switching to another texture.
        // same texture can use the same depth as the order will
        // do the job
        if (spritesheet?.key != currentSpritesheet?.key) {
            currentDepth -= DEPTH_STEP
            currentSpritesheet = spritesheet
        }
    }

    override fun resetPalette() {
        parameters.blender.pal()
    }

    override fun swapPalette(
        source: Int,
        target: Int,
    ) {
        parameters.blender.pal(source, target)
    }

    override fun setCamera(
        x: Int,
        y: Int,
    ) {
        parameters.camera.set(x, y)
    }

    override fun getCamera(): Pair<Int, Int> {
        return parameters.camera.x to parameters.camera.y
    }

    override fun resetCamera() {
        parameters.camera.set(0, 0)
    }

    override fun setClip(
        x: Int,
        y: Int,
        width: Int,
        height: Int,
    ) {
        parameters.clipper.set(x, y, width, height)
    }

    override fun resetClip() {
        parameters.clipper.reset()
    }

    override fun setDrawMode(mode: DrawingMode) {
        // Render everything to start the new mode with a fresh and clean state.
        renderAllInFrameBuffer()
        kgl.bindFramebuffer(GL_FRAMEBUFFER, frameBufferContext.frameBuffer)
        when (mode) {
            DrawingMode.DEFAULT -> {
                kgl.disable(GL_STENCIL_TEST)
                kgl.enable(GL_BLEND)
                kgl.stencilMask(0x00)
                kgl.blendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA)
                kgl.colorMask(red = true, green = true, blue = true, alpha = true)
            }

            DrawingMode.ALPHA_BLEND -> {
                kgl.disable(GL_STENCIL_TEST)
                kgl.enable(GL_BLEND)
                kgl.blendFuncSeparate(GL_ZERO, GL_ONE, GL_ZERO, GL_ONE_MINUS_SRC_ALPHA)
                kgl.colorMask(red = true, green = true, blue = true, alpha = true)
            }

            DrawingMode.STENCIL_WRITE -> {
                kgl.enable(GL_STENCIL_TEST)
                kgl.disable(GL_BLEND)

                // Clear the stencil before writing inside.
                kgl.stencilMask(0xFF)
                kgl.clear(GL_STENCIL_BUFFER_BIT)

                kgl.stencilFunc(GL_ALWAYS, 1, 0xFF)
                kgl.stencilOp(GL_KEEP, GL_KEEP, GL_REPLACE)
                // Don't write the actual sprite in the color buffer
                kgl.colorMask(red = false, green = false, blue = false, alpha = false)
            }

            DrawingMode.STENCIL_TEST -> {
                kgl.enable(GL_STENCIL_TEST)
                kgl.enable(GL_BLEND)

                kgl.stencilMask(0x00)
                kgl.stencilFunc(GL_EQUAL, 1, 0xFF)
                kgl.stencilOp(GL_KEEP, GL_KEEP, GL_KEEP)
                kgl.colorMask(red = true, green = true, blue = true, alpha = true)

                kgl.blendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA)
            }

            DrawingMode.STENCIL_NOT_TEST -> {
                kgl.enable(GL_STENCIL_TEST)
                kgl.enable(GL_BLEND)

                kgl.stencilMask(0x00)
                kgl.stencilFunc(GL_NOTEQUAL, 1, 0xFF)
                kgl.stencilOp(GL_KEEP, GL_KEEP, GL_KEEP)
                kgl.colorMask(red = true, green = true, blue = true, alpha = true)
                // kgl.stencilMask(0x00)

                kgl.blendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA)
            }
        }
        kgl.bindFramebuffer(GL_FRAMEBUFFER, null)
    }

    /**
     * perform the action if the entity describe by the bounding box (x, y, width, height)
     * is in the clip zone of the camera.
     *
     * Do nothing otherwise.
     */
    private fun isInFrame(
        x: Int,
        y: Int,
        width: Int,
        height: Int,
        action: () -> Unit,
    ) {
        val obj = boundingBoxPool.obtain().set(x, y, width, height)
        val camera = boundingBoxPool.obtain().set(
            x = parameters.camera.x,
            y = parameters.camera.y,
            width = gameOptions.width,
            height = gameOptions.height,
        )
        val clipper = boundingBoxPool.obtain().set(
            x = parameters.clipper.left,
            y = parameters.clipper.top,
            width = parameters.clipper.width,
            height = parameters.clipper.height,
        )

        val drawingZone = boundingBoxPool.obtain()

        intersection(camera, clipper, drawingZone)

        if (overlap(obj, drawingZone)) {
            action()
        }

        boundingBoxPool.free(obj, camera, clipper, drawingZone)
    }

    data class BoundingBox(var x: Int, var y: Int, var width: Int, var height: Int) {
        fun reset() {
            set(0, 0, 0, 0)
        }

        fun set(
            x: Int,
            y: Int,
            width: Int,
            height: Int,
        ): BoundingBox {
            this.x = x
            this.y = y
            this.width = width
            this.height = height
            return this
        }
    }

    private fun intersection(
        a: BoundingBox,
        b: BoundingBox,
        result: BoundingBox,
    ) {
        val x1 = maxOf(a.x, b.x)
        val y1 = maxOf(a.y, b.y)
        val x2 = minOf(a.x + a.width, b.x + b.width)
        val y2 = minOf(a.y + a.height, b.y + b.height)

        result.x = x1
        result.y = y1
        result.width = maxOf(0, x2 - x1)
        result.height = maxOf(0, y2 - y1)
    }

    private fun overlap(
        a: BoundingBox,
        b: BoundingBox,
    ): Boolean {
        val result = boundingBoxPool.obtain()
        intersection(a, b, result)
        val isOverlap = result.width > 0 && result.height > 0
        boundingBoxPool.free(result)
        return isOverlap
    }

    companion object {
        private const val DEPTH_STEP = 0.0001f
    }
}
