package com.github.minigdx.tiny.platform.glfw

import com.danielgergely.kgl.KglLwjgl
import com.github.minigdx.tiny.Seconds
import com.github.minigdx.tiny.engine.GameLoop
import com.github.minigdx.tiny.engine.GameOptions
import com.github.minigdx.tiny.file.FileStream
import com.github.minigdx.tiny.file.InputStreamStream
import com.github.minigdx.tiny.file.JvmLocalFile
import com.github.minigdx.tiny.file.LocalFile
import com.github.minigdx.tiny.file.SoundDataSourceStream
import com.github.minigdx.tiny.file.SourceStream
import com.github.minigdx.tiny.file.VirtualFileSystem
import com.github.minigdx.tiny.graphic.PixelFormat.RGBA
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
import com.github.minigdx.tiny.sound.JavaSoundManager
import com.github.minigdx.tiny.sound.SoundManager
import com.github.minigdx.tiny.util.MutableFixedSizeList
import com.squareup.gifencoder.FastGifEncoder
import com.squareup.gifencoder.ImageOptions
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.lwjgl.glfw.GLFW
import org.lwjgl.glfw.GLFW.GLFW_CURSOR
import org.lwjgl.glfw.GLFW.GLFW_CURSOR_HIDDEN
import org.lwjgl.opengl.GL
import org.lwjgl.system.MemoryUtil
import java.awt.image.BufferedImage
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.File
import java.util.concurrent.TimeUnit
import javax.imageio.ImageIO
import kotlin.math.min

class GlfwPlatform(
    override val gameOptions: GameOptions,
    private val logger: Logger,
    private val vfs: VirtualFileSystem,
    private val workdirectory: File,
    private val render: Render = OpenGLRender(KglLwjgl, logger, gameOptions),
    private val jarResourcePrefix: String = "",
) : Platform {
    private var window: Long = 0

    private var lastFrame: Long = getTime()

    // Keep 30 seconds at 60 frames per seconds
    private val gifFrameCache: MutableFixedSizeList<IntArray> = MutableFixedSizeList(gameOptions.record.toInt() * FPS)

    private var lastDraw: ByteArray? = null

    private val lwjglInputHandler = LwjglInput(gameOptions)

    private val recordScope = CoroutineScope(Dispatchers.IO)

    /**
     * Get the time in milliseconds
     *
     * @return The system time in milliseconds
     */
    private fun getTime(): Long {
        return System.nanoTime() / 1000000
    }

    private fun getDelta(): Seconds {
        val time = getTime()
        val delta = (time - lastFrame)
        lastFrame = time
        return min(delta / 1000f, 1 / 60f)
    }

    override fun initWindowManager(): WindowManager {
        if (!GLFW.glfwInit()) {
            throw IllegalStateException("Unable to initialize GLFW")
        }

        GLFW.glfwWindowHint(GLFW.GLFW_CONTEXT_VERSION_MAJOR, 3)
        GLFW.glfwWindowHint(GLFW.GLFW_CONTEXT_VERSION_MINOR, 3)
        GLFW.glfwWindowHint(GLFW.GLFW_OPENGL_PROFILE, GLFW.GLFW_OPENGL_CORE_PROFILE)
        GLFW.glfwWindowHint(GLFW.GLFW_OPENGL_FORWARD_COMPAT, GLFW.GLFW_TRUE)
        GLFW.glfwWindowHint(GLFW.GLFW_OPENGL_DEBUG_CONTEXT, GLFW.GLFW_TRUE)

        GLFW.glfwDefaultWindowHints() // optional, the current window hints are already the default

        GLFW.glfwWindowHint(
            GLFW.GLFW_VISIBLE,
            GLFW.GLFW_FALSE,
        ) // the window will stay hidden after creation
        GLFW.glfwWindowHint(
            GLFW.GLFW_RESIZABLE,
            GLFW.GLFW_FALSE,
        ) // the window will be resizable

        // Create the window
        val windowWidth = (gameOptions.width + gameOptions.gutter.first * 2) * gameOptions.zoom
        val windowHeight = (gameOptions.height + gameOptions.gutter.first * 2) * gameOptions.zoom
        window =
            GLFW.glfwCreateWindow(
                windowWidth,
                windowHeight,
                "Tiny",
                MemoryUtil.NULL,
                MemoryUtil.NULL,
            )
        if (window == MemoryUtil.NULL) {
            throw IllegalStateException("Failed to create the GLFW window")
        }

        // Get the resolution of the primary monitor
        val vidmode =
            GLFW.glfwGetVideoMode(GLFW.glfwGetPrimaryMonitor())
                ?: throw IllegalStateException("No primary monitor found")
        // Center our window
        GLFW.glfwSetWindowPos(
            window,
            (vidmode.width() - windowWidth) / 2,
            (vidmode.height() - windowHeight) / 2,
        )

        // Hide the mouse cursor if configured like so in the game.
        if (gameOptions.hideMouseCursor) {
            GLFW.glfwSetInputMode(window, GLFW_CURSOR, GLFW_CURSOR_HIDDEN)
        }

        // Make the OpenGL context current
        GLFW.glfwMakeContextCurrent(window)

        // Enable v-sync
        GLFW.glfwSwapInterval(1)

        // Make the window visible
        GLFW.glfwShowWindow(window)

        // Get the size of the device window
        val tmpWidth = MemoryUtil.memAllocInt(1)
        val tmpHeight = MemoryUtil.memAllocInt(1)
        GLFW.glfwGetWindowSize(window, tmpWidth, tmpHeight)

        val tmpFrameBufferWidth = MemoryUtil.memAllocInt(1)
        val tmpFrameBufferHeight = MemoryUtil.memAllocInt(1)
        GLFW.glfwGetFramebufferSize(window, tmpFrameBufferWidth, tmpFrameBufferHeight)

        lwjglInputHandler.attachHandler(window)

        GL.createCapabilities(true)
        return WindowManager(
            windowWidth = tmpWidth.get(),
            windowHeight = tmpHeight.get(),
            screenWidth = tmpFrameBufferWidth.get(),
            screenHeight = tmpFrameBufferHeight.get(),
        )
    }

    override fun initRenderManager(windowManager: WindowManager): RenderContext {
        return render.init(windowManager)
    }

    override fun initInputManager(): InputManager {
        return lwjglInputHandler
    }

    override fun io(): CoroutineDispatcher = Dispatchers.IO

    override fun initInputHandler(): InputHandler {
        return lwjglInputHandler
    }

    override fun gameLoop(gameLoop: GameLoop) {
        // Render loop
        runBlocking {
            while (!GLFW.glfwWindowShouldClose(window)) {
                gameLoop.advance(getDelta())
                gameLoop.draw()

                GLFW.glfwSwapBuffers(window) // swap the color buffers
                GLFW.glfwPollEvents()
            }
        }
        gameLoop.end()
        GLFW.glfwTerminate()
    }

    private fun convert(data: ByteArray): IntArray {
        val result = IntArray(data.size)
        val colorPalette = gameOptions.colors()
        data.forEachIndexed { index, byte ->
            result[index] = colorPalette.getRGAasInt(byte.toInt())
        }
        return result
    }

    override fun draw(renderContext: RenderContext) {
        render.drawOnScreen(renderContext)
        val pixels = render.readRenderAsFrameBuffer(renderContext)

        val imageCopy = pixels.pixels.copyOf()
        recordScope.launch {
            gifFrameCache.add(convert(imageCopy))
        }

        lastDraw = imageCopy
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

    override fun endGameLoop() = Unit

    override fun record() {
        val origin = newFile("video", "gif")

        logger.info("GLWF") { "Starting to generate GIF in '${origin.absolutePath}' (Wait for it...)" }
        val buffer =
            mutableListOf<IntArray>().apply {
                addAll(gifFrameCache)
            }

        recordScope.launch {
            val now = System.currentTimeMillis()
            val options =
                ImageOptions().apply {
                    this.setDelay(20, TimeUnit.MILLISECONDS)
                }
            ByteArrayOutputStream().use { out ->
                val encoder =
                    FastGifEncoder(
                        out,
                        gameOptions.width,
                        gameOptions.height,
                        0,
                        gameOptions.colors(),
                    )

                buffer.forEach { img ->
                    encoder.addImage(img, gameOptions.width, options)
                }
                encoder.finishEncoding()
                vfs.save(FileStream(origin), out.toByteArray())
            }
            logger.info("GLFW") { "Screen recorded in '${origin.absolutePath}' in ${System.currentTimeMillis() - now} ms" }
        }
    }

    private fun newFile(
        prefixName: String,
        extension: String,
    ): File {
        var index = 0
        var origin = workdirectory.resolve("output_${index.toString().padStart(3, '0')}.$extension")
        while (origin.exists()) {
            index++
            if (index >= 999) {
                throw IllegalStateException(
                    "Too many file '${prefixName}_xxx.$extension' generated! " +
                        "You might need to delete some",
                )
            }
            origin = workdirectory.resolve("output_${index.toString().padStart(3, '0')}.$extension")
        }
        return origin
    }

    override fun screenshot() {
        val buffer = lastDraw ?: return

        recordScope.launch {
            val origin = newFile("screenshoot", "png")
            val width = gameOptions.width
            val height = gameOptions.height
            val image = BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB)

            for (y in 0 until height) {
                for (x in 0 until width) {
                    val colorData = gameOptions.colors().getRGBA(buffer[x + y * width].toInt())

                    val r = colorData[0].toInt() and 0xff
                    val g = colorData[1].toInt() and 0xff
                    val b = colorData[2].toInt() and 0xff
                    val a = colorData[3].toInt() and 0xff
                    val color = (a shl 24) or (r shl 16) or (g shl 8) or b
                    image.setRGB(x, y, color)
                }
            }

            ImageIO.write(image, "png", origin)
        }
    }

    private fun extractRGBA(imageData: ByteArray): ImageData {
        val image = ImageIO.read(ByteArrayInputStream(imageData))
        val width = image.width
        val height = image.height

        val rgb = image.getRGB(0, 0, width, height, null, 0, width)

        val result = ByteArray(width * height * RGBA)

        (0 until rgb.size).forEach { pixel ->
            // rgb is in ARGB format
            val p = rgb[pixel]
            val r = (p shr 16) and 0xFF
            val g = (p shr 8) and 0xFF
            val b = p and 0xFF
            val a = (p shr 24) and 0xFF

            result[pixel * RGBA + 0] = r.toByte()
            result[pixel * RGBA + 1] = g.toByte()
            result[pixel * RGBA + 2] = b.toByte()
            result[pixel * RGBA + 3] = a.toByte()
        }
        return ImageData(result, width, height)
    }

    override fun createByteArrayStream(
        name: String,
        canUseJarPrefix: Boolean,
    ): SourceStream<ByteArray> {
        val resourceName =
            if (canUseJarPrefix) {
                "$jarResourcePrefix/$name"
            } else {
                "/$name"
            }

        val fromJar = GlfwPlatform::class.java.getResourceAsStream(resourceName)
        return if (fromJar != null) {
            InputStreamStream(fromJar)
        } else {
            FileStream(workdirectory.resolve(name))
        }
    }

    override fun createImageStream(
        name: String,
        canUseJarPrefix: Boolean,
    ): SourceStream<ImageData> {
        return object : SourceStream<ImageData> {
            private val delegate = createByteArrayStream(name, canUseJarPrefix)

            override suspend fun exists(): Boolean = delegate.exists()

            override suspend fun read(): ImageData {
                return extractRGBA(delegate.read())
            }

            override fun wasModified(): Boolean = delegate.wasModified()
        }
    }

    override fun createSoundStream(name: String): SourceStream<SoundData> {
        return SoundDataSourceStream(name, soundManager, createByteArrayStream(name))
    }

    private lateinit var soundManager: SoundManager

    override fun initSoundManager(inputHandler: InputHandler): SoundManager {
        return JavaSoundManager().also {
            soundManager = it
        }.also {
            it.initSoundManager(inputHandler)
        }
    }

    override fun createLocalFile(name: String): LocalFile = JvmLocalFile(name, workdirectory.resolve("data"))

    companion object {
        private const val FPS = 60
    }
}
