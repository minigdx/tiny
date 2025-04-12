package com.github.minigdx.tiny.render

import com.danielgergely.kgl.ByteBuffer
import com.danielgergely.kgl.FloatBuffer
import com.danielgergely.kgl.Framebuffer
import com.danielgergely.kgl.GL_ARRAY_BUFFER
import com.danielgergely.kgl.GL_BLEND
import com.danielgergely.kgl.GL_COLOR_ATTACHMENT0
import com.danielgergely.kgl.GL_COLOR_BUFFER_BIT
import com.danielgergely.kgl.GL_DEPTH_BUFFER_BIT
import com.danielgergely.kgl.GL_FALSE
import com.danielgergely.kgl.GL_FLOAT
import com.danielgergely.kgl.GL_FRAGMENT_SHADER
import com.danielgergely.kgl.GL_FRAMEBUFFER
import com.danielgergely.kgl.GL_FRAMEBUFFER_COMPLETE
import com.danielgergely.kgl.GL_LINEAR
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
import com.github.minigdx.tiny.Pixel
import com.github.minigdx.tiny.engine.GameOptions
import com.github.minigdx.tiny.graphic.PixelFormat
import com.github.minigdx.tiny.log.Logger
import com.github.minigdx.tiny.platform.WindowManager

class OpenGlCPURenderUnit(gl: Kgl, logger: Logger, gameOptions: GameOptions) : RendererUnit<OpenGLCPURenderContext>(
    gl,
    logger,
    gameOptions,
) {
    private var colorPaletteBuffer = ByteArray(0)

    private val uvsData =
        FloatBuffer(
            floatArrayOf(
                // bottom right
                2f,
                1f,
                // top left
                0f,
                -1f,
                // bottom left
                0f,
                1f,
            ),
        )

    private val vertexData =
        floatArrayOf(
            // bottom right
            3f,
            -1f,
            // top left
            -1f,
            3f,
            // bottom left
            -1f,
            -1f,
        )

    override fun init(windowManager: WindowManager): OpenGLCPURenderContext {
        // Load and compile the shaders
        val shaderProgram = gl.createProgram()!!

        val vertexShaderId = createShader(VERTEX_SHADER, GL_VERTEX_SHADER)
        val fragmentShaderId = createShader(FRAGMENT_SHADER, GL_FRAGMENT_SHADER)

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

        gl.uniform2f(
            gl.getUniformLocation(shaderProgram, "game_screen")!!,
            gameOptions.width.toFloat(),
            gameOptions.height.toFloat(),
        )


        // Generate the texture
        val gameTexture = gl.createTexture()
        gl.usingTexture(gameTexture) {
            gl.texParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_S, GL_REPEAT)
            gl.texParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_T, GL_REPEAT)

            gl.texParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_NEAREST)
            gl.texParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_NEAREST)
        }

        // Setup the drawing surface
        val positionBuffer = gl.createBuffer()
        gl.bindBuffer(GL_ARRAY_BUFFER, positionBuffer)
        gl.bufferData(GL_ARRAY_BUFFER, FloatBuffer(vertexData), vertexData.size * Float.SIZE_BYTES, GL_STATIC_DRAW)

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
            6 * Float.SIZE_BYTES,
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
        // OpenGL ES required a texture with squared size.
        // So it's a 256*256 texture, even if only the first
        // row of this texture is used.
        colorPaletteBuffer = ByteArray(256 * 256 * PixelFormat.RGBA)
        var pos = 0
        for (index in 0 until 256) {
            val color = colors.getRGBA(index)

            colorPaletteBuffer[pos++] = color[0]
            colorPaletteBuffer[pos++] = color[1]
            colorPaletteBuffer[pos++] = color[2]
            colorPaletteBuffer[pos++] = color[3]
        }
        val colorPaletteTexture = gl.createTexture()
        gl.usingTexture(colorPaletteTexture) {
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
                ByteBuffer(colorPaletteBuffer),
            )
        }

        return OpenGLCPURenderContext(
            windowManager = windowManager,
            program = shaderProgram,
            gameTexture = gameTexture,
            colorPalette = colorPaletteTexture,
        )
    }

    override fun drawCPU(
        context: OpenGLCPURenderContext,
        image: ByteArray,
        width: Pixel,
        height: Pixel,
    ) {
        gl.useProgram(context.program)

        // -- game screen -- //
        // Push instructions as textures
        gl.activeTexture(GL_TEXTURE0)
        gl.bindTexture(GL_TEXTURE_2D, context.gameTexture)
        gl.uniform1i(gl.getUniformLocation(context.program, "frameBuffer")!!, 0)
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

        // setup shaders
        gl.activeTexture(GL_TEXTURE1)
        gl.bindTexture(GL_TEXTURE_2D, context.colorPalette)
        gl.uniform1i(gl.getUniformLocation(context.program, "colors")!!, 1)

        gl.clear(GL_COLOR_BUFFER_BIT or GL_DEPTH_BUFFER_BIT) // FIXME: supprimer GL_DEPTH_BUFFER ?
        gl.clearColor(0f, 0f, 0f, 1.0f)

        gl.drawArrays(GL_TRIANGLES, 0, 3)
    }

    companion object {
        //language=Glsl
        val VERTEX_SHADER =
            """
            #ifdef GL_ES
                precision highp float;
            #endif
            
            attribute vec2 position;
            attribute vec2 uvs;
            
            varying vec2 viewport;
            
            void main() {
                gl_Position = vec4(position, 0.0, 1.0);
                viewport = uvs;
            }
            """.trimIndent()

        //language=Glsl
        val FRAGMENT_SHADER =
            """
            #ifdef GL_ES
                precision highp float;
            #endif
                    
            // it goes from 0.0 -> 1.0
            varying vec2 viewport;
            
            // Color palette
            uniform sampler2D colors;
            // Frame Buffer
            uniform sampler2D frameBuffer;
            // Size of the game screen, in pixel, in the game resolution (see _tiny.json)
            uniform vec2 game_screen;
            
            /**
            * Extract data from a "kind of" texture1D
            */
            vec4 readData(sampler2D txt, int index, int textureWidth, int textureHeight) {
                int x = index - textureWidth * (index / textureWidth); // index % textureWidth
                int y =  index / textureWidth;
                vec2 uv = vec2((float(x) + 0.5) / float(textureWidth), (float(y) + 0.5) / float(textureHeight));
                return texture2D(txt, uv);
            }
            
            /**
            * Read a color from the colors texture.
            */
            vec4 readColor(int index) {
                int icolor = index - 256 * (index / 256);
                return readData(colors, icolor, 256, 256);
            }
            
            void main() {
                vec4 color = texture2D(frameBuffer, viewport);
                gl_FragColor = readColor(int((color * 255.0) + 0.5));
            }
            """.trimIndent()
    }
}
