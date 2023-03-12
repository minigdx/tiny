package com.github.minigdx.tiny.platform.glfw

import com.danielgergely.kgl.ByteBuffer
import com.danielgergely.kgl.FloatBuffer
import com.danielgergely.kgl.GL_ARRAY_BUFFER
import com.danielgergely.kgl.GL_COLOR_BUFFER_BIT
import com.danielgergely.kgl.GL_COMPILE_STATUS
import com.danielgergely.kgl.GL_DEPTH_BUFFER_BIT
import com.danielgergely.kgl.GL_EXTENSIONS
import com.danielgergely.kgl.GL_FALSE
import com.danielgergely.kgl.GL_FLOAT
import com.danielgergely.kgl.GL_FRAGMENT_SHADER
import com.danielgergely.kgl.GL_LINK_STATUS
import com.danielgergely.kgl.GL_NEAREST
import com.danielgergely.kgl.GL_RENDERER
import com.danielgergely.kgl.GL_REPEAT
import com.danielgergely.kgl.GL_RGBA
import com.danielgergely.kgl.GL_SHADING_LANGUAGE_VERSION
import com.danielgergely.kgl.GL_STATIC_DRAW
import com.danielgergely.kgl.GL_TEXTURE_2D
import com.danielgergely.kgl.GL_TEXTURE_MAG_FILTER
import com.danielgergely.kgl.GL_TEXTURE_MIN_FILTER
import com.danielgergely.kgl.GL_TEXTURE_WRAP_S
import com.danielgergely.kgl.GL_TEXTURE_WRAP_T
import com.danielgergely.kgl.GL_TRIANGLES
import com.danielgergely.kgl.GL_UNSIGNED_BYTE
import com.danielgergely.kgl.GL_VENDOR
import com.danielgergely.kgl.GL_VERSION
import com.danielgergely.kgl.GL_VERTEX_SHADER
import com.danielgergely.kgl.Kgl
import com.danielgergely.kgl.KglLwjgl
import com.danielgergely.kgl.Shader
import com.github.minigdx.tiny.Pixel
import com.github.minigdx.tiny.Seconds
import com.github.minigdx.tiny.engine.GameLoop
import com.github.minigdx.tiny.engine.GameOption
import com.github.minigdx.tiny.file.FileStream
import com.github.minigdx.tiny.file.VirtualFileSystem
import com.github.minigdx.tiny.graphic.FrameBuffer
import com.github.minigdx.tiny.log.Logger
import com.github.minigdx.tiny.platform.Platform
import com.github.minigdx.tiny.platform.RenderContext
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
import org.lwjgl.opengl.GL33
import org.lwjgl.system.MemoryUtil
import java.io.ByteArrayOutputStream
import java.io.File
import java.util.concurrent.TimeUnit
import kotlin.math.min


class GlfwPlatform(private val logger: Logger, private val vfs: VirtualFileSystem) : Platform {

    private var window: Long = 0

    private var lastFrame: Long = getTime()

    private val gl: Kgl = KglLwjgl

    // Keep 30 seconds at 60 frames per seconds
    private val gifBufferCache: MutableFixedSizeList<IntArray> = MutableFixedSizeList(8 * 60)

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

    override fun initWindow(gameOption: GameOption) {
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
        window = GLFW.glfwCreateWindow(
            gameOption.width * gameOption.zoom,
            gameOption.height * gameOption.zoom,
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
            (vidmode.width() - 256) / 2,
            (vidmode.height() - 256) / 2
        )

        GLFW.glfwSetFramebufferSizeCallback(window) { _, width, height -> gl.viewport(0, 0, width, height) }

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
                        this@GlfwPlatform.record(gameOption)
                    }
                }
            }
        )
    }

    override fun createDrawContext(): RenderContext {
        GL.createCapabilities(true)
        logger.info("GLFW") { "GL_VENDOR:                \t" + GL33.glGetString(GL_VENDOR) }
        logger.info("GLFW") { "GL_VERSION:               \t" + GL33.glGetString(GL_VERSION) }
        logger.info("GLFW") { "GL_RENDERER:              \t" + GL33.glGetString(GL_RENDERER) }
        logger.info("GLFW") { "SHADING_LANGUAGE_VERSION: \t" + GL33.glGetString(GL_SHADING_LANGUAGE_VERSION) }
        logger.info("GLFW") { "EXTENSIONS:               \t" + GL33.glGetString(GL_EXTENSIONS) }

        // Load and compile the shaders
        val vertexShader = """
        #ifdef GL_ES
            precision highp float;
        #endif
        
        attribute vec3 position;
        attribute vec2 uvs;
        
        varying vec2 texture;
        
        void main() {
            gl_Position = vec4(position, 1.0);
            texture = uvs;
        }
    """.trimIndent()

        val fragmentShader = """
        #ifdef GL_ES
            precision highp float;
        #endif
        
        varying vec2 texture;
        
        uniform sampler2D image;
        
        void main() {
            gl_FragColor = texture2D(image, texture);
        }
    """.trimIndent()

        val shaderProgram = gl.createProgram()!!

        val vertexShaderId = createShader(vertexShader, GL_VERTEX_SHADER)
        val fragmentShaderId = createShader(fragmentShader, GL_FRAGMENT_SHADER)

        gl.attachShader(shaderProgram, vertexShaderId)
        gl.attachShader(shaderProgram, fragmentShaderId)

        gl.linkProgram(shaderProgram)

        if (gl.getProgramParameter(shaderProgram, GL_LINK_STATUS) == GL_FALSE) {
            val programInfoLog = gl.getProgramInfoLog(shaderProgram)
            throw RuntimeException("Unable to link shader program: '$programInfoLog'")
        }

        gl.useProgram(shaderProgram)

        gl.deleteShader(vertexShaderId);
        gl.deleteShader(fragmentShaderId);

        // Generate the texture
        val gameTexture = gl.createTexture()
        gl.bindTexture(GL_TEXTURE_2D, gameTexture)

        val vertexData = floatArrayOf(
            -1f, -3f,
            3f, 1f,
            -1f, 1f
        )

        val positionBuffer = gl.createBuffer()
        gl.bindBuffer(GL_ARRAY_BUFFER, positionBuffer)
        gl.bufferData(GL_ARRAY_BUFFER, FloatBuffer(vertexData), vertexData.size, GL_STATIC_DRAW)

        val position = gl.getAttribLocation(shaderProgram, "position")
        gl.vertexAttribPointer(
            location = position,
            size = 2,
            type = GL_FLOAT,
            normalized = false,
            stride = 0,
            offset = 0
        )
        gl.enableVertexAttribArray(position)

        // Push the UVs of the texture
        val uvsBuffer = gl.createBuffer()
        gl.bindBuffer(GL_ARRAY_BUFFER, uvsBuffer)
        gl.bufferData(
            GL_ARRAY_BUFFER,
            uvsData,
            6,
            GL_STATIC_DRAW
        )

        val uvs = gl.getAttribLocation(shaderProgram, "uvs")
        gl.vertexAttribPointer(
            location = uvs,
            size = 2,
            type = GL_FLOAT,
            normalized = false,
            stride = 0,
            offset = 0
        )
        gl.enableVertexAttribArray(uvs)


        return GLRenderContext(
            program = shaderProgram,
            texture = gameTexture,
        )
    }

    private fun createShader(vertexShader: String, shaderType: Int): Shader {
        val vertexShaderId = gl.createShader(shaderType)!!
        gl.shaderSource(vertexShaderId, vertexShader)
        gl.compileShader(vertexShaderId)
        if (gl.getShaderParameter(vertexShaderId, GL_COMPILE_STATUS) == GL_FALSE) {
            val log = gl.getShaderInfoLog(vertexShaderId)
            gl.deleteShader(vertexShaderId)
            throw RuntimeException(
                "Shader compilation error: $log \n" +
                    "---------- \n" +
                    "Shader code in error: \n" +
                    vertexShader
            )
        }
        return vertexShaderId
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

    private val uvsData = FloatBuffer(
        floatArrayOf(
            2f, 0f,
            0f, 2f,
            0f, 0f,
        )
    )


    override fun draw(context: RenderContext, frameBuffer: FrameBuffer, width: Pixel, height: Pixel) {
        context as GLRenderContext

        val image = frameBuffer.generateBuffer()

        gl.bindTexture(GL_TEXTURE_2D, context.texture)
        gl.texImage2D(GL_TEXTURE_2D, 0, GL_RGBA, width, height, 0, GL_RGBA, GL_UNSIGNED_BYTE, ByteBuffer(image))

        gl.texParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_S, GL_REPEAT)
        gl.texParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_T, GL_REPEAT)

        gl.texParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_NEAREST)
        gl.texParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_NEAREST)

        gl.uniform1i(gl.getUniformLocation(context.program, "image")!!, 0)

        gl.clear(GL_COLOR_BUFFER_BIT or GL_DEPTH_BUFFER_BIT)

        gl.clearColor(0f, 1f, 0.3f, 1.0f)
        gl.drawArrays(GL_TRIANGLES, 0, 3)

        gifBufferCache.add(frameBuffer.gifBuffer)
    }

    override fun endGameLoop() = Unit

    override fun record(gameOption: GameOption) {
        val origin = File("output.gif")
        logger.info("GLWF") { "Starting to generate GIF in '${origin.absolutePath}' (Wait for it...)" }
        val buffer = mutableListOf<IntArray>().apply {
            addAll(gifBufferCache)
        }

        val images = ArrayList<Image>(8 * 60)

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
                    .flatMapMerge(8 * 60) { indexValue ->
                        val index = indexValue.index
                        val frame = indexValue.value
                        val render = IntArray(gameOption.width * gameOption.zoom * gameOption.height * gameOption.zoom)
                        // Check each pixel of the frame
                        (0 until gameOption.width).forEach { x ->
                            (0 until gameOption.height).forEach { y ->
                                val pixel = frame[x + y * gameOption.width]

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
}

