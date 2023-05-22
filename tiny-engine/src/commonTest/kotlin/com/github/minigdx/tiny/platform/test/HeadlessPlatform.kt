package com.github.minigdx.tiny.platform.test

import com.github.minigdx.tiny.engine.GameLoop
import com.github.minigdx.tiny.engine.GameOptions
import com.github.minigdx.tiny.file.SourceStream
import com.github.minigdx.tiny.graphic.FrameBuffer
import com.github.minigdx.tiny.input.InputHandler
import com.github.minigdx.tiny.input.InputManager
import com.github.minigdx.tiny.platform.ImageData
import com.github.minigdx.tiny.platform.Platform
import com.github.minigdx.tiny.platform.RenderContext
import com.github.minigdx.tiny.platform.SoundData
import com.github.minigdx.tiny.platform.WindowManager
import com.github.minigdx.tiny.sound.MidiSound
import com.github.minigdx.tiny.sound.SoundManager
import com.github.minigdx.tiny.util.MutableFixedSizeList
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers

class NoopRenderContext : RenderContext

class HeadlessPlatform(override val gameOptions: GameOptions, val resources: Map<String, Any>, frames: Int = 10) :
    Platform {

    val input = VirtualInputHandler()

    val frames: MutableFixedSizeList<FrameBuffer> = MutableFixedSizeList(frames)

    private var gameLoop: GameLoop? = null

    override fun initWindowManager(): WindowManager {
        return WindowManager(
            gameOptions.width,
            gameOptions.height,
            gameOptions.width,
            gameOptions.height,
        )
    }

    override fun initRenderManager(windowManager: WindowManager): RenderContext {
        return NoopRenderContext()
    }

    fun advance() {
        gameLoop?.advance(1f)
        gameLoop?.draw()
    }

    override fun gameLoop(gameLoop: GameLoop) {
        this.gameLoop = gameLoop
    }

    override fun draw(context: RenderContext, frameBuffer: FrameBuffer) {
        val newBuffer = FrameBuffer(
            frameBuffer.width, frameBuffer.height, frameBuffer.gamePalette
        ).apply {
            colorIndexBuffer.copyFrom(frameBuffer.colorIndexBuffer)
        }

        frames.add(newBuffer)
    }

    override fun endGameLoop() = Unit

    override fun initInputHandler(): InputHandler = input
    override fun initInputManager(): InputManager = input

    override fun initSoundManager(inputHandler: InputHandler): SoundManager {
        return object : SoundManager {
            override fun initSoundManager(inputHandler: InputHandler) = Unit

            override suspend fun createSound(data: ByteArray): MidiSound {
                return object : MidiSound {
                    override fun play() = Unit

                    override fun loop() = Unit

                    override fun stop() = Unit
                }
            }
        }
    }

    override fun io(): CoroutineDispatcher {
        return Dispatchers.Unconfined
    }

    override fun createByteArrayStream(name: String): SourceStream<ByteArray> {
        val data = resources[name] as? ByteArray ?: throw IllegalStateException("$name is not a valid ByteArray.")
        return ObjectStream(data)
    }

    override fun createImageStream(name: String): SourceStream<ImageData> {
        val data = resources[name] as? ImageData ?: throw IllegalStateException("$name is not a valid ImageData.")
        return ObjectStream(data)
    }

    override fun createSoundStream(name: String): SourceStream<SoundData> {
        val data = resources[name] as? SoundData ?: throw IllegalStateException("$name is not a valid SoundData.")
        return ObjectStream(data)
    }
}
