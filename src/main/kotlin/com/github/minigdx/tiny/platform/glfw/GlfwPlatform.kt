package com.github.minigdx.tiny.platform.glfw

import com.danielgergely.kgl.KglLwjgl
import com.github.minigdx.tiny.Seconds
import com.github.minigdx.tiny.engine.GameLoop
import com.github.minigdx.tiny.engine.GameOption
import com.github.minigdx.tiny.file.FileStream
import com.github.minigdx.tiny.file.VirtualFileSystem
import com.github.minigdx.tiny.graphic.FrameBuffer
import com.github.minigdx.tiny.log.Logger
import com.github.minigdx.tiny.platform.ImageData
import com.github.minigdx.tiny.platform.Platform
import com.github.minigdx.tiny.platform.RenderContext
import com.github.minigdx.tiny.render.GLRender
import com.github.minigdx.tiny.util.MutableFixedSizeList
import com.squareup.gifencoder.FastGifEncoder
import com.squareup.gifencoder.Image
import com.squareup.gifencoder.ImageOptions
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.flatMapMerge
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.onCompletion
import kotlinx.coroutines.flow.withIndex
import kotlinx.coroutines.launch
import org.lwjgl.glfw.GLFW
import org.lwjgl.glfw.GLFW.GLFW_PRESS
import org.lwjgl.glfw.GLFW.GLFW_RELEASE
import org.lwjgl.glfw.GLFWKeyCallback
import org.lwjgl.opengl.GL
import org.lwjgl.system.MemoryUtil
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.File
import java.util.concurrent.TimeUnit
import javax.imageio.ImageIO
import kotlin.math.min

class WindowManager(
    val windowWidth: Int,
    val windowHeight: Int,
    val screenWidth: Int,
    val screenHeight: Int,
    val ratioWidth: Int = screenWidth / windowWidth,
    val ratioHeight: Int = screenHeight / windowHeight,
)

class GlfwPlatform(
    override val gameOption: GameOption,
    private val logger: Logger,
    private val vfs: VirtualFileSystem
) : Platform {

    private var window: Long = 0

    private var lastFrame: Long = getTime()

    private val render = GLRender(KglLwjgl, logger, gameOption)

    // Keep 30 seconds at 60 frames per seconds
    private val gifBufferCache: MutableFixedSizeList<IntArray> = MutableFixedSizeList(gameOption.record.toInt() * FPS)

    private val recordScope = CoroutineScope(Dispatchers.Default)

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

        GLFW.glfwWindowHint(GLFW.GLFW_CONTEXT_VERSION_MAJOR, 2)
        GLFW.glfwWindowHint(GLFW.GLFW_CONTEXT_VERSION_MINOR, 0)
        GLFW.glfwWindowHint(GLFW.GLFW_OPENGL_PROFILE, GLFW.GLFW_OPENGL_CORE_PROFILE)
        GLFW.glfwWindowHint(GLFW.GLFW_OPENGL_DEBUG_CONTEXT, GLFW.GLFW_TRUE);


        GLFW.glfwDefaultWindowHints() // optional, the current window hints are already the default

        GLFW.glfwWindowHint(
            GLFW.GLFW_VISIBLE,
            GLFW.GLFW_FALSE
        ) // the window will stay hidden after creation
        GLFW.glfwWindowHint(
            GLFW.GLFW_RESIZABLE,
            GLFW.GLFW_FALSE
        ) // the window will be resizable

        // Create the window
        val windowWidth = (gameOption.width + gameOption.gutter.first * 2) * gameOption.zoom
        val windowHeight = (gameOption.height + gameOption.gutter.first * 2) * gameOption.zoom
        window = GLFW.glfwCreateWindow(
            windowWidth,
            windowHeight,
            "Tiny",
            MemoryUtil.NULL,
            MemoryUtil.NULL
        )
        if (window == MemoryUtil.NULL) {
            throw IllegalStateException("Failed to create the GLFW window")
        }

        // Get the resolution of the primary monitor
        val vidmode = GLFW.glfwGetVideoMode(GLFW.glfwGetPrimaryMonitor())
            ?: throw IllegalStateException("No primary monitor found")
        // Center our window
        GLFW.glfwSetWindowPos(
            window,
            (vidmode.width() - windowWidth) / 2,
            (vidmode.height() - windowHeight) / 2
        )


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

        GLFW.glfwSetKeyCallback(
            window,
            object : GLFWKeyCallback() {

                private var controlDown = false
                private var rDown = false
                override fun invoke(window: Long, key: Int, scancode: Int, action: Int, mods: Int) {
                    if (action == GLFW_PRESS) {
                        if (key == GLFW.GLFW_KEY_R) {
                            rDown = true
                        } else if (key == GLFW.GLFW_KEY_LEFT_CONTROL) {
                            controlDown = true
                        }
                    } else if (action == GLFW_RELEASE) {
                        if (key == GLFW.GLFW_KEY_R) {
                            rDown = false
                        } else if (key == GLFW.GLFW_KEY_LEFT_CONTROL) {
                            controlDown = false
                        }
                    }

                    if (rDown && controlDown) {
                        this@GlfwPlatform.record()
                    }
                }
            }
        )

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

    override fun gameLoop(gameLoop: GameLoop) {
        // Render loop
        while (!GLFW.glfwWindowShouldClose(window)) {
            gameLoop.advance(getDelta())
            gameLoop.draw()

            GLFW.glfwSwapBuffers(window) // swap the color buffers
            GLFW.glfwPollEvents()
        }
        GLFW.glfwTerminate()
    }

    override fun draw(context: RenderContext, frameBuffer: FrameBuffer) {
        val image = frameBuffer.generateBuffer()
        render.draw(context, image, frameBuffer.width, frameBuffer.height)
        gifBufferCache.add(frameBuffer.gifBuffer)
    }

    override fun endGameLoop() = Unit

    override fun record() {
        val origin = File("output.gif")
        logger.info("GLWF") { "Starting to generate GIF in '${origin.absolutePath}' (Wait for it...)" }
        val buffer = mutableListOf<IntArray>().apply {
            addAll(gifBufferCache)
        }

        val images = ArrayList<Image>(gameOption.record.toInt() * FPS)

        val now = System.currentTimeMillis()
        recordScope.launch {
            val options = ImageOptions().apply {
                this.setDelay(20, TimeUnit.MILLISECONDS)
            }
            ByteArrayOutputStream().use { out ->
                val encoder = FastGifEncoder(
                    out,
                    gameOption.width * gameOption.zoom,
                    gameOption.height * gameOption.zoom,
                    0,
                    FrameBuffer.rgbPalette
                )
                flowOf(*buffer.toTypedArray())
                    .withIndex()
                    .flatMapMerge(gameOption.record.toInt() * FPS) { indexValue ->
                        val index = indexValue.index
                        val frame = indexValue.value
                        val render = IntArray(gameOption.width * gameOption.zoom * gameOption.height * gameOption.zoom)
                        // Check each pixel of the frame
                        (0 until gameOption.width).forEach { x ->
                            (0 until gameOption.height).forEach { y ->
                                val pixel = frame[y + x * gameOption.height]

                                (0 until gameOption.zoom).forEach { copyx ->
                                    val xx = x * gameOption.zoom + copyx
                                    (0 until gameOption.zoom).forEach { copyy ->
                                        val yy = (y * gameOption.zoom + copyy) * gameOption.width * gameOption.zoom
                                        render[xx + yy] = pixel
                                    }
                                }
                            }
                        }
                        flowOf(index to Image.fromRgb(render, gameOption.width * gameOption.zoom))
                    }
                    .onCompletion {
                        images.forEach { render ->
                            encoder.addImage(
                                render,
                                options
                            )
                        }
                        encoder.finishEncoding()
                        vfs.save(FileStream(origin), out.toByteArray())
                        logger.info("GLFW") { "Screen recorded in '${origin.absolutePath}' in ${System.currentTimeMillis() - now} ms" }
                    }
                    .collect { render ->
                        images.add(render.first, render.second)
                    }

            }
        }
    }

    override fun extractRGBA(imageData: ByteArray): ImageData {
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

    companion object {
        private const val FPS = 60
        private const val RGBA = 4
    }
}

