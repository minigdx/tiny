package com.github.minigdx.tiny.platform.test

import com.danielgergely.kgl.Kgl
import com.github.minigdx.tiny.engine.GameLoop
import com.github.minigdx.tiny.engine.GameOptions
import com.github.minigdx.tiny.file.SourceStream
import com.github.minigdx.tiny.graphic.FrameBufferParameters
import com.github.minigdx.tiny.input.InputHandler
import com.github.minigdx.tiny.input.InputManager
import com.github.minigdx.tiny.lua.Note
import com.github.minigdx.tiny.platform.ImageData
import com.github.minigdx.tiny.platform.Platform
import com.github.minigdx.tiny.platform.SoundData
import com.github.minigdx.tiny.platform.WindowManager
import com.github.minigdx.tiny.platform.performance.PerformanceMetrics
import com.github.minigdx.tiny.platform.performance.PerformanceMonitor
import com.github.minigdx.tiny.sound.ChunkGenerator
import com.github.minigdx.tiny.sound.Instrument
import com.github.minigdx.tiny.sound.SoundHandler
import com.github.minigdx.tiny.sound.SoundManager
import com.github.minigdx.tiny.util.FloatData
import com.github.minigdx.tiny.util.MutableFixedSizeList
import dev.mokkery.mock
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

    val frames: MutableFixedSizeList<FrameBufferParameters> = MutableFixedSizeList(frames)

    private var gameLoop: GameLoop? = null

    override fun saveIntoGameDirectory(
        name: String,
        data: String,
    ) {
        TODO("Not yet implemented")
    }

    override fun saveWave(sound: FloatArray) {
        TODO("Not yet implemented")
    }

    override val performanceMonitor: PerformanceMonitor = HeadlessPerformanceMonitor()

    private val storage = mutableMapOf<String, String>()

    override fun initWindowManager(): WindowManager {
        return WindowManager(
            gameOptions.width,
            gameOptions.height,
            gameOptions.width,
            gameOptions.height,
        )
    }

    override fun initRenderManager(windowManager: WindowManager) = mock<Kgl> { }

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

            override fun createSoundHandler(buffer: FloatArray): SoundHandler =
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

                    override fun nextChunk(samples: Int): FloatData {
                        TODO("Not yet implemented")
                    }
                }

            override fun createSoundHandler(buffer: Sequence<FloatArray>): SoundHandler {
                TODO("Not yet implemented")
            }

            override fun createSoundHandler(chunkGenerator: ChunkGenerator): SoundHandler {
                TODO("Not yet implemented")
            }

            override fun noteOn(
                note: Note,
                instrument: Instrument,
            ) {
                TODO("Not yet implemented")
            }

            override fun noteOff(note: Note) {
                TODO("Not yet implemented")
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

    override fun writeImage(buffer: ByteArray) {
        TODO("Not yet implemented")
    }

    override fun saveIntoHome(
        name: String,
        content: String,
    ) {
        storage[name] = content
    }

    override fun getFromHome(name: String): String? {
        return storage[name]
    }
}
