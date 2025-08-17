package com.github.minigdx.tiny.platform.test

import com.github.minigdx.tiny.ColorIndex
import com.github.minigdx.tiny.Pixel
import com.github.minigdx.tiny.engine.GameLoop
import com.github.minigdx.tiny.engine.GameOptions
import com.github.minigdx.tiny.file.LocalFile
import com.github.minigdx.tiny.file.SourceStream
import com.github.minigdx.tiny.graphic.FrameBuffer
import com.github.minigdx.tiny.graphic.PixelArray
import com.github.minigdx.tiny.input.InputHandler
import com.github.minigdx.tiny.input.InputManager
import com.github.minigdx.tiny.platform.ImageData
import com.github.minigdx.tiny.platform.Platform
import com.github.minigdx.tiny.platform.SoundData
import com.github.minigdx.tiny.platform.WindowManager
import com.github.minigdx.tiny.platform.performance.PerformanceMetrics
import com.github.minigdx.tiny.platform.performance.PerformanceMonitor
import com.github.minigdx.tiny.render.NopRenderContext
import com.github.minigdx.tiny.render.RenderContext
import com.github.minigdx.tiny.render.RenderFrame
import com.github.minigdx.tiny.render.operations.DrawSprite
import com.github.minigdx.tiny.render.operations.RenderOperation
import com.github.minigdx.tiny.sound.SoundHandler
import com.github.minigdx.tiny.sound.SoundManager
import com.github.minigdx.tiny.util.MutableFixedSizeList
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers

class HeadlessPerformanceMonitor : PerformanceMonitor {
    override fun frameStart() = Unit

    override fun frameEnd(): PerformanceMetrics = PerformanceMetrics(0.0, 0.0, 0, 0)

    override fun drawCall(nbVertex: Int) = Unit

    override fun readPixels() = Unit

    override fun drawOnScreen() = Unit

    override fun operationStart(name: String) = Unit

    override fun operationEnd(name: String): Double = 0.0

    override fun getCurrentMemoryUsage(): Long = 0

    override fun getAllocatedMemorySinceLastCheck(): Long = 0

    override fun reset() = Unit

    override fun getAverageMetrics(frameCount: Int): PerformanceMetrics? = null

    override fun now(): Long = 0

    override var isEnabled: Boolean = false
}

class HeadlessPlatform(
    override val gameOptions: GameOptions,
    private val resources: Map<String, Any>,
    frames: Int = 10,
) : Platform {
    private val input = VirtualInputHandler()

    val frames: MutableFixedSizeList<FrameBuffer> = MutableFixedSizeList(frames)

    private var gameLoop: GameLoop? = null

    override val performanceMonitor: PerformanceMonitor = HeadlessPerformanceMonitor()

    override fun initWindowManager(): WindowManager {
        return WindowManager(
            gameOptions.width,
            gameOptions.height,
            gameOptions.width,
            gameOptions.height,
        )
    }

    override fun initRenderManager(windowManager: WindowManager): RenderContext {
        return NopRenderContext
    }

    suspend fun advance() {
        gameLoop?.advance(1f)
        gameLoop?.draw()
    }

    override fun gameLoop(gameLoop: GameLoop) {
        this.gameLoop = gameLoop
    }

    override fun endGameLoop() = Unit

    override fun initInputHandler(): InputHandler = input

    override fun initInputManager(): InputManager = input

    override fun initSoundManager(inputHandler: InputHandler): SoundManager {
        return object : SoundManager() {
            override fun initSoundManager(inputHandler: InputHandler) = Unit

            override fun createSoundHandler(
                buffer: FloatArray,
                numberOfSamples: Long,
            ): SoundHandler =
                object : SoundHandler {
                    override fun play() {
                        TODO("Not yet implemented")
                    }

                    override fun loop() {
                        TODO("Not yet implemented")
                    }

                    override fun stop() {
                        TODO("Not yet implemented")
                    }
                }
        }
    }

    override fun io(): CoroutineDispatcher {
        return Dispatchers.Unconfined
    }

    override fun createByteArrayStream(
        name: String,
        canUseJarPrefix: Boolean,
    ): SourceStream<ByteArray> {
        val data =
            (resources[name] as? String?)?.encodeToByteArray() ?: resources[name] as? ByteArray
                ?: throw IllegalStateException("$name is not a valid ByteArray.")
        return ObjectStream(data)
    }

    override fun createImageStream(
        name: String,
        canUseJarPrefix: Boolean,
    ): SourceStream<ImageData> {
        val data = resources[name] as? ImageData ?: throw IllegalStateException("$name is not a valid ImageData.")
        return ObjectStream(data)
    }

    override fun createSoundStream(
        name: String,
        soundManager: SoundManager,
    ): SourceStream<SoundData> {
        val data = resources[name] as? SoundData ?: throw IllegalStateException("$name is not a valid SoundData.")
        return ObjectStream(data)
    }

    override fun createLocalFile(
        name: String,
        parentDirectory: String?,
    ): LocalFile =
        object : LocalFile {
            override val name: String = "name"
            override val extension: String = ""

            override fun readAll(): ByteArray = ByteArray(0)

            override fun save(content: ByteArray) = Unit
        }

    /**
     * Render of GPU operation is not supported yet
     */
    override fun render(
        renderContext: RenderContext,
        ops: List<RenderOperation>,
    ) {
        val pixels =
            ops.firstOrNull { op -> (op as? DrawSprite)?.source?.name == "framebuffer" }
                ?.let { drawSprite -> (drawSprite as DrawSprite).source?.pixels }

        if (pixels != null) {
            val frameBuffer = FrameBuffer(gameOptions.width, gameOptions.height, gameOptions.colors())
            frameBuffer.copyFrom(pixels)
            frames.add(frameBuffer)
        }
    }

    override fun readRender(renderContext: RenderContext): RenderFrame {
        if (frames.isEmpty()) {
            // Force to generate at least one frame.
            draw(renderContext)
        }
        return FrameBufferFrame(frames.last())
    }

    override fun draw(renderContext: RenderContext) = Unit

    override fun executeOffScreen(
        renderContext: RenderContext,
        block: () -> Unit,
    ): RenderFrame {
        draw(renderContext)
        // Render the frame
        block.invoke()
        val frame = FrameBufferFrame(frames.last())
        // Drop the rendered frame
        frames.removeLast()
        return frame
    }

    fun saveAnimation(name: String) = toGif(name, frames)

    class FrameBufferFrame(frameBuffer: FrameBuffer) : RenderFrame {
        private val frameBuffer =
            FrameBuffer(frameBuffer.width, frameBuffer.height, frameBuffer.gamePalette).apply {
                this.fastCopyFrom(frameBuffer)
            }

        override fun copyInto(pixelArray: PixelArray) {
            pixelArray.copyFrom(frameBuffer.colorIndexBuffer)
        }

        override fun getPixel(
            x: Pixel,
            y: Pixel,
        ): ColorIndex {
            return frameBuffer.pixel(x, y)
        }
    }
}
