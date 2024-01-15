package com.github.minigdx.tiny.render

import com.danielgergely.kgl.ByteBuffer
import com.danielgergely.kgl.FloatBuffer
import com.danielgergely.kgl.GL_ARRAY_BUFFER
import com.danielgergely.kgl.GL_COLOR_BUFFER_BIT
import com.danielgergely.kgl.GL_COMPILE_STATUS
import com.danielgergely.kgl.GL_DEPTH_BUFFER_BIT
import com.danielgergely.kgl.GL_FALSE
import com.danielgergely.kgl.GL_FLOAT
import com.danielgergely.kgl.GL_FRAGMENT_SHADER
import com.danielgergely.kgl.GL_LINK_STATUS
import com.danielgergely.kgl.GL_NEAREST
import com.danielgergely.kgl.GL_R8
import com.danielgergely.kgl.GL_RED
import com.danielgergely.kgl.GL_REPEAT
import com.danielgergely.kgl.GL_RGBA
import com.danielgergely.kgl.GL_STATIC_DRAW
import com.danielgergely.kgl.GL_TEXTURE0
import com.danielgergely.kgl.GL_TEXTURE1
import com.danielgergely.kgl.GL_TEXTURE_2D
import com.danielgergely.kgl.GL_TEXTURE_MAG_FILTER
import com.danielgergely.kgl.GL_TEXTURE_MIN_FILTER
import com.danielgergely.kgl.GL_TEXTURE_WRAP_S
import com.danielgergely.kgl.GL_TEXTURE_WRAP_T
import com.danielgergely.kgl.GL_TRIANGLES
import com.danielgergely.kgl.GL_UNSIGNED_BYTE
import com.danielgergely.kgl.GL_VERTEX_SHADER
import com.danielgergely.kgl.Kgl
import com.danielgergely.kgl.Shader
import com.github.minigdx.tiny.Pixel
import com.github.minigdx.tiny.engine.GameOptions
import com.github.minigdx.tiny.graphic.PixelFormat
import com.github.minigdx.tiny.log.Logger
import com.github.minigdx.tiny.platform.RenderContext
import com.github.minigdx.tiny.platform.WindowManager

class GLRender(
    private val gl: Kgl,
    private val logger: Logger,
    private val gameOptions: GameOptions,
) : Render {

    private var buffer = ByteArray(0)

    private val uvsData = FloatBuffer(
        floatArrayOf(
            2f,
            2f,
            0f,
            0f,
            0f,
            2f,
        ),
    )

    override fun init(windowManager: WindowManager): RenderContext {
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
        uniform sampler2D colors;
        
        void main() {
            vec4 point = texture2D(image, texture);
            vec4 color = texture2D(colors, vec2(point.r, 1.0));
            
            gl_FragColor = color;
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

        gl.deleteShader(vertexShaderId)
        gl.deleteShader(fragmentShaderId)

        // Generate the texture
        val gameTexture = gl.createTexture()
        gl.bindTexture(GL_TEXTURE_2D, gameTexture)

        gl.texParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_S, GL_REPEAT)
        gl.texParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_T, GL_REPEAT)

        gl.texParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_NEAREST)
        gl.texParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_NEAREST)

        val vertexData = floatArrayOf(
            3f,
            -1f,
            -1f,
            3f,
            -1f,
            -1f,
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
            offset = 0,
        )
        gl.enableVertexAttribArray(position)

        // Push the UVs of the texture
        val uvsBuffer = gl.createBuffer()
        gl.bindBuffer(GL_ARRAY_BUFFER, uvsBuffer)
        gl.bufferData(
            GL_ARRAY_BUFFER,
            uvsData,
            6,
            GL_STATIC_DRAW,
        )

        val uvs = gl.getAttribLocation(shaderProgram, "uvs")
        gl.vertexAttribPointer(
            location = uvs,
            size = 2,
            type = GL_FLOAT,
            normalized = false,
            stride = 0,
            offset = 0,
        )
        gl.enableVertexAttribArray(uvs)

        val colors = gameOptions.colors()
        // texture of one pixel height and 256 pixel width.
        // one pixel of the texture = one index.
        buffer = ByteArray(256 * 256 * PixelFormat.RGBA)
        var pos = 0
        for (y in 0 until 256) {
            for (index in 0 until 256) {
                val color = colors.getRGBA(index)

                buffer[pos++] = color[0]
                buffer[pos++] = color[1]
                buffer[pos++] = color[2]
                buffer[pos++] = color[3]
            }
        }
        val index = gl.createTexture()
        gl.bindTexture(GL_TEXTURE_2D, index)

        gl.texParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_S, GL_REPEAT)
        gl.texParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_T, GL_REPEAT)

        gl.texParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_NEAREST)
        gl.texParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_NEAREST)
        gl.texImage2D(
            GL_TEXTURE_2D,
            0,
            GL_RGBA,
            256,
            256,
            0,
            GL_RGBA,
            GL_UNSIGNED_BYTE,
            ByteBuffer(buffer),
        )
        gl.uniform1i(gl.getUniformLocation(shaderProgram, "colors")!!, 1)

        return GLRenderContext(
            windowManager = windowManager,
            program = shaderProgram,
            texture = gameTexture,
            colors = index,
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
                    vertexShader,
            )
        }
        return vertexShaderId
    }

    override fun draw(context: RenderContext, image: ByteArray, width: Pixel, height: Pixel) {
        context as GLRenderContext

        gl.viewport(
            gameOptions.gutter.first * gameOptions.zoom * context.windowManager.ratioWidth,
            gameOptions.gutter.second * gameOptions.zoom * context.windowManager.ratioHeight,
            gameOptions.width * gameOptions.zoom * context.windowManager.ratioWidth,
            gameOptions.height * gameOptions.zoom * context.windowManager.ratioHeight,
        )

        // -- game screen -- //
        gl.activeTexture(GL_TEXTURE0)
        gl.bindTexture(GL_TEXTURE_2D, context.texture)

        gl.texImage2D(
            GL_TEXTURE_2D,
            0,
            GL_R8,
            width,
            height,
            0,
            GL_RED,
            GL_UNSIGNED_BYTE,
            ByteBuffer(image),
        )
        gl.uniform1i(gl.getUniformLocation(context.program, "image")!!, 0)

        gl.activeTexture(GL_TEXTURE1)
        gl.bindTexture(GL_TEXTURE_2D, context.colors)

        gl.clear(GL_COLOR_BUFFER_BIT or GL_DEPTH_BUFFER_BIT)

        gl.clearColor(0f, 0f, 0f, 1.0f)
        gl.drawArrays(GL_TRIANGLES, 0, 3)
    }
}
