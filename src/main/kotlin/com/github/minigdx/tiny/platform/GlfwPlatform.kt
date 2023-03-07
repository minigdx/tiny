package com.github.minigdx.tiny.platform

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
import com.danielgergely.kgl.GlBuffer
import com.danielgergely.kgl.Kgl
import com.danielgergely.kgl.KglLwjgl
import com.danielgergely.kgl.Program
import com.danielgergely.kgl.Shader
import com.danielgergely.kgl.Texture
import com.github.minigdx.tiny.Pixel
import com.github.minigdx.tiny.Seconds
import com.github.minigdx.tiny.engine.GameLoop
import com.github.minigdx.tiny.engine.GameOption
import com.github.minigdx.tiny.log.Logger
import org.lwjgl.glfw.GLFW
import org.lwjgl.opengl.GL
import org.lwjgl.opengl.GL33
import org.lwjgl.system.MemoryUtil
import kotlin.math.min

interface DrawContext
class GLDrawContext(
    val program: Program,
    val texture: Texture,
) : DrawContext

class GlfwPlatform(private val logger: Logger) : Platform {

    private var window: Long = 0

    private var lastFrame: Long = getTime()

    private val gl: Kgl = KglLwjgl

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
        // GLFW.glfwWindowHint(GLFW.GLFW_OPENGL_FORWARD_COMPAT, GLFW.GLFW_TRUE)
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
        val vidmode =
            GLFW.glfwGetVideoMode(GLFW.glfwGetPrimaryMonitor())
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
    }

    override fun createDrawContext(): DrawContext {
        GL.createCapabilities(true)
        logger.info("GLFW") { "GL_VENDOR:                \t" + GL33.glGetString(GL_VENDOR) }
        logger.info("GLFW") { "GL_VERSION:               \t" + GL33.glGetString(GL_VERSION) }
        logger.info("GLFW") { "GL_RENDERER:              \t" + GL33.glGetString(GL_RENDERER) }
        logger.info("GLFW") { "SHADING_LANGUAGE_VERSION: \t" + GL33.glGetString(GL_SHADING_LANGUAGE_VERSION) }
        logger.info("GLFW") { "EXTENSIONS:               \t" + GL33.glGetString(GL_EXTENSIONS) }
        // https://github.com/AradiPatrik/learn-opengl/blob/403632f16a279571700e1d952e214884b195cb27/src/main/kotlin/com/aradipatrik/learn/opengl/Main.kt

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


        return GLDrawContext(
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

    /*
    private val uvsData = FloatBuffer(
        floatArrayOf(
            0f, 0f,
            2f, 2f,
            0f, 1f
        )
    )
*/
    private val uvsData = FloatBuffer(
        floatArrayOf(
            0f, 2f,
            2f, 0f,
            0f, 0f,
        )
    )


    override fun draw(context: DrawContext, image: ByteArray, width: Pixel, height: Pixel) {
        context as GLDrawContext

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
    }

    override fun endGameLoop() = Unit
}
