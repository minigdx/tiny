package com.github.minigdx.tiny.render

import com.danielgergely.kgl.ByteBuffer
import com.danielgergely.kgl.FloatBuffer
import com.danielgergely.kgl.Framebuffer
import com.danielgergely.kgl.GL_ARRAY_BUFFER
import com.danielgergely.kgl.GL_BLEND
import com.danielgergely.kgl.GL_COLOR_ATTACHMENT0
import com.danielgergely.kgl.GL_COLOR_BUFFER_BIT
import com.danielgergely.kgl.GL_COMPILE_STATUS
import com.danielgergely.kgl.GL_DEPTH_BUFFER_BIT
import com.danielgergely.kgl.GL_FALSE
import com.danielgergely.kgl.GL_FLOAT
import com.danielgergely.kgl.GL_FRAGMENT_SHADER
import com.danielgergely.kgl.GL_FRAMEBUFFER
import com.danielgergely.kgl.GL_FRAMEBUFFER_COMPLETE
import com.danielgergely.kgl.GL_LINEAR
import com.danielgergely.kgl.GL_LINK_STATUS
import com.danielgergely.kgl.GL_NEAREST
import com.danielgergely.kgl.GL_ONE_MINUS_SRC_ALPHA
import com.danielgergely.kgl.GL_R8
import com.danielgergely.kgl.GL_RED
import com.danielgergely.kgl.GL_REPEAT
import com.danielgergely.kgl.GL_RGBA
import com.danielgergely.kgl.GL_SRC_ALPHA
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
import com.danielgergely.kgl.Texture
import com.github.minigdx.tiny.ColorIndex
import com.github.minigdx.tiny.Pixel
import com.github.minigdx.tiny.engine.Frame
import com.github.minigdx.tiny.engine.GameOptions
import com.github.minigdx.tiny.engine.RenderOperation
import com.github.minigdx.tiny.graphic.PixelArray
import com.github.minigdx.tiny.graphic.PixelFormat
import com.github.minigdx.tiny.log.Logger
import com.github.minigdx.tiny.platform.RenderContext
import com.github.minigdx.tiny.platform.WindowManager

class GLRender(
    private val gl: Kgl,
    private val logger: Logger,
    private val gameOptions: GameOptions,
) : Render {
    private var colorBuffer = ByteArray(0)

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

    override fun init(windowManager: WindowManager): RenderContext {
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

        // Prepare the FBO
        val fboBuffer = ByteBuffer(windowManager.screenWidth * windowManager.screenHeight * PixelFormat.RGBA)
        val fbo: Framebuffer = gl.createFramebuffer()
        fbo.usingFramebuffer {
            // Prepare the texture used for the FBO
            val fboTexture = gl.createTexture()
            fboTexture.usingTexture {
                // Framebuffer of the size of the screen
                gl.texImage2D(
                    GL_TEXTURE_2D,
                    0,
                    GL_RGBA,
                    gameOptions.width * gameOptions.zoom,
                    gameOptions.height * gameOptions.zoom,
                    0,
                    GL_RGBA,
                    GL_UNSIGNED_BYTE,
                    fboBuffer,
                )
                gl.texParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_LINEAR)
                gl.texParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_LINEAR)
                gl.framebufferTexture2D(GL_FRAMEBUFFER, GL_COLOR_ATTACHMENT0, GL_TEXTURE_2D, fboTexture, 0)

                if (gl.checkFramebufferStatus(GL_FRAMEBUFFER) != GL_FRAMEBUFFER_COMPLETE) {
                    TODO("Framebuffer is NOT complete!")
                }
            }
        }

        // Generate the texture
        val gameTexture = gl.createTexture()
        gameTexture.usingTexture {
            gl.texParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_S, GL_REPEAT)
            gl.texParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_T, GL_REPEAT)

            gl.texParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_NEAREST)
            gl.texParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_NEAREST)
        }

        // Setup the drawing surface
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
        // OpenGL ES required a texture with squared size.
        // So it's a 256*256 texture, even if only the first
        // row of this texture is used.
        colorBuffer = ByteArray(256 * 256 * PixelFormat.RGBA)
        var pos = 0
        for (index in 0 until 256) {
            val color = colors.getRGBA(index)

            colorBuffer[pos++] = color[0]
            colorBuffer[pos++] = color[1]
            colorBuffer[pos++] = color[2]
            colorBuffer[pos++] = color[3]
        }
        val index = gl.createTexture()
        index.usingTexture {
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
                ByteBuffer(colorBuffer),
            )
        }

        return GLRenderContext(
            windowManager = windowManager,
            program = shaderProgram,
            gameTexture = gameTexture,
            colorPalette = index,
            fbo = fbo,
            fboBuffer = fboBuffer,
        )
    }

    override fun draw(
        context: RenderContext,
        ops: List<RenderOperation>,
    ) {
        context as GLRenderContext

        gl.viewport(
            gameOptions.gutter.first * gameOptions.zoom * context.windowManager.ratioWidth,
            gameOptions.gutter.second * gameOptions.zoom * context.windowManager.ratioHeight,
            gameOptions.width * gameOptions.zoom * context.windowManager.ratioWidth,
            gameOptions.height * gameOptions.zoom * context.windowManager.ratioHeight,
        )
        gl.enable(GL_BLEND)
        gl.blendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA) // Or import these constants

        gl.clear(GL_COLOR_BUFFER_BIT or GL_DEPTH_BUFFER_BIT)
        gl.clearColor(0f, 0f, 0f, 1.0f)

        ops.forEach {
            // draw arrays ?
            // fixme: only gpu
        }

    }

    override fun draw(
        context: RenderContext,
        image: ByteArray,
        width: Pixel,
        height: Pixel,
    ) {
        context as GLRenderContext

        gl.viewport(
            gameOptions.gutter.first * gameOptions.zoom * context.windowManager.ratioWidth,
            gameOptions.gutter.second * gameOptions.zoom * context.windowManager.ratioHeight,
            gameOptions.width * gameOptions.zoom * context.windowManager.ratioWidth,
            gameOptions.height * gameOptions.zoom * context.windowManager.ratioHeight,
        )
        gl.disable(GL_BLEND)

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

    override fun drawOffscreen(
        context: RenderContext,
        ops: List<RenderOperation>,
    ): Frame {
        context as GLRenderContext
        context.fbo.usingFramebuffer {
            draw(context, ops)

            // Read ONLY (gutter excluded) the game viewport!
            gl.readPixels(
                gameOptions.gutter.first * gameOptions.zoom * context.windowManager.ratioWidth,
                gameOptions.gutter.second * gameOptions.zoom * context.windowManager.ratioHeight,
                gameOptions.width * gameOptions.zoom * context.windowManager.ratioWidth,
                gameOptions.height * gameOptions.zoom * context.windowManager.ratioHeight,
                GL_RGBA,
                GL_UNSIGNED_BYTE,
                context.fboBuffer,
            )
        }
        return GLFrame(context.fboBuffer, gameOptions)
    }

    fun Framebuffer.usingFramebuffer(block: () -> Unit) {
        gl.bindFramebuffer(GL_FRAMEBUFFER, this)
        block()
        gl.bindFramebuffer(GL_FRAMEBUFFER, null)
    }

    fun Texture.usingTexture(block: () -> Unit) {
        gl.bindTexture(GL_TEXTURE_2D, this)
        block()
        gl.bindTexture(GL_TEXTURE_2D, null)
    }

    private fun createShader(
        shader: String,
        shaderType: Int,
    ): Shader {
        fun addLineNumbers(text: String): String {
            val lines = text.lines()
            val lineNumberWidth = lines.size.toString().length
            return lines.mapIndexed { index, line ->
                val lineNumber = (index + 1).toString().padStart(lineNumberWidth, ' ')
                "$lineNumber: $line"
            }.joinToString("\n")
        }

        val vertexShaderId = gl.createShader(shaderType)!!
        gl.shaderSource(vertexShaderId, shader)
        gl.compileShader(vertexShaderId)
        if (gl.getShaderParameter(vertexShaderId, GL_COMPILE_STATUS) == GL_FALSE) {
            val log = gl.getShaderInfoLog(vertexShaderId)
            gl.deleteShader(vertexShaderId)
            throw RuntimeException(
                "Shader compilation error: $log \n" +
                    "---------- \n" +
                    "Shader code in error: \n" +
                    addLineNumbers(shader),
            )
        }
        return vertexShaderId
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
                gl_FragColor = readColor(int(color.r * 256.0));
            }
            """.trimIndent()

        //language=Glsl
        val FRAGMENT_SHADER_BACKUP =
            """
            #ifdef GL_ES
                precision highp float;
            #endif
                    
            // it goes from 0.0 -> 1.0
            varying vec2 viewport;
            
            // Color palette
            uniform sampler2D colors;
            // Size of the game screen, in pixel, in the game resolution (see _tiny.json)
            uniform vec2 game_screen;
            
            // Type of the shape
            uniform float u_type;
            uniform float u_arg1;
            uniform float u_arg2;
            uniform float u_arg3;
            uniform float u_arg4;
            uniform float u_arg5;
            uniform float u_arg6;
            
            
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
            
            vec4 cls(int color) {
                return readColor(color);
            }
            
            bool is_pixel(int x, int y) {
                int current_x = int(viewport.x * game_screen.x);
                int current_y = int(viewport.y * game_screen.y);
                
                return x <= current_x && current_x < x + 1 &&
                       y <= current_y && current_y < y + 1; 

            }
            
            vec4 pset(int x, int y, int icolor) {
                if(is_pixel(x, y)) {
                     return readColor(icolor);
                } else {
                    return vec4(0.0, 0.0, 0.0, 0.0);
                }
            }
            
            void main() {
                vec4 color = vec4(1.0, 0.0, 0.0, 0.0);
                
                int type = int(u_type);
                if(type == 0) {
                    color = cls(int(u_arg1));
                } else if (type == 1) {
                    color = pset(
                                int(u_arg1), 
                                int(u_arg2),
                                int(u_arg3)
                            );
                }
                gl_FragColor = color;
            }
            """.trimIndent()
    }
}

class GLFrame(
    private val buffer: ByteBuffer,
    private val gameOptions: GameOptions,
) : Frame {
    override fun get(
        x: Pixel,
        y: Pixel,
    ): ColorIndex {
        val i = x * gameOptions.zoom + y * gameOptions.width * gameOptions.zoom * PixelFormat.RGBA
        val result = ByteArray(PixelFormat.RGBA)
        buffer.position = i
        buffer.get(result)
        return gameOptions.colors().fromRGBA(result)
    }

    /**
     * Convert the actual Frame (with RGBA) into a Pixel Array of Color index.
     */
    override fun toPixelArray(): PixelArray {
        val result = PixelArray(gameOptions.width, gameOptions.height, pixelFormat = PixelFormat.INDEX)
        buffer.position = 0
        val tmp = ByteArray(PixelFormat.RGBA)
        var index = 0
        (0 until gameOptions.width * gameOptions.height * PixelFormat.RGBA step PixelFormat.RGBA).forEach { i ->
            buffer.position = i
            buffer.get(tmp)
            result.pixels[index++] = gameOptions.colors().fromRGBA(tmp).toByte()
        }

        return result
    }
}
