package com.github.minigdx.tiny.platform.android

import android.content.Context
import com.danielgergely.kgl.KglAndroid
import com.github.minigdx.tiny.engine.GameLoop
import com.github.minigdx.tiny.engine.GameOptions
import com.github.minigdx.tiny.file.LocalFile
import com.github.minigdx.tiny.file.SoundDataSourceStream
import com.github.minigdx.tiny.file.SourceStream
import com.github.minigdx.tiny.input.InputHandler
import com.github.minigdx.tiny.input.InputManager
import com.github.minigdx.tiny.log.Logger
import com.github.minigdx.tiny.platform.ImageData
import com.github.minigdx.tiny.platform.Platform
import com.github.minigdx.tiny.platform.SoundData
import com.github.minigdx.tiny.platform.WindowManager
import com.github.minigdx.tiny.render.Render
import com.github.minigdx.tiny.render.RenderContext
import com.github.minigdx.tiny.render.RenderFrame
import com.github.minigdx.tiny.render.gl.OpenGLRender
import com.github.minigdx.tiny.render.operations.RenderOperation
import com.github.minigdx.tiny.sound.SoundManager
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers

class AndroidPlatform(
    override val gameOptions: GameOptions,
    private val context: Context,
    private val logger: Logger,
    private val render: Render = OpenGLRender(KglAndroid, logger, gameOptions),
) : Platform {
    private lateinit var inputHandler: AndroidInputHandler
    private lateinit var windowManager: AndroidWindowManager
    private lateinit var soundManager: AndroidSoundManager

    override fun initWindowManager(): WindowManager {
        windowManager = AndroidWindowManager(context, gameOptions)
        return windowManager
    }

    override fun initRenderManager(windowManager: WindowManager): RenderContext {
        return render.init(gameOptions, logger)
    }

    override fun gameLoop(gameLoop: GameLoop) {
        // Android's game loop is driven by the GLSurfaceView's renderer
        // This will be called from the TinyView's renderer
        windowManager.setGameLoop(gameLoop)
    }

    override fun endGameLoop() {
        // Clean up resources
        windowManager.destroy()
    }

    override fun initInputHandler(): InputHandler {
        inputHandler = AndroidInputHandler(gameOptions)
        return inputHandler
    }

    override fun initInputManager(): InputManager {
        return InputManager(inputHandler)
    }

    override fun initSoundManager(inputHandler: InputHandler): SoundManager {
        soundManager = AndroidSoundManager(context, gameOptions, logger)
        return soundManager
    }

    override fun io(): CoroutineDispatcher = Dispatchers.IO

    override fun createByteArrayStream(
        name: String,
        canUseJarPrefix: Boolean,
    ): SourceStream<ByteArray> {
        return AndroidAssetStream(context, name)
    }

    override fun createImageStream(
        name: String,
        canUseJarPrefix: Boolean,
    ): SourceStream<ImageData> {
        return AndroidImageStream(context, name)
    }

    override fun createSoundStream(name: String): SourceStream<SoundData> {
        return SoundDataSourceStream(name, soundManager, createByteArrayStream(name))
    }

    override fun createLocalFile(
        name: String,
        parentDirectory: String?,
    ): LocalFile {
        return AndroidLocalFile(context, name, parentDirectory)
    }

    override fun render(
        renderContext: RenderContext,
        ops: List<RenderOperation>,
    ) {
        render.render(renderContext, ops)
    }

    override fun readRender(renderContext: RenderContext): RenderFrame {
        return render.readRender(renderContext)
    }

    override fun draw(renderContext: RenderContext) {
        render.drawOnScreen(renderContext)
    }

    override fun executeOffScreen(
        renderContext: RenderContext,
        block: () -> Unit,
    ): RenderFrame {
        return render.executeOffScreen(renderContext, block)
    }

    override fun record()  = Unit

    override fun screenshot() = Unit
}
