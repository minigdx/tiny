package com.github.minigdx.tiny.render

import com.danielgergely.kgl.ByteBuffer
import com.danielgergely.kgl.FloatBuffer
import com.danielgergely.kgl.GL_ARRAY_BUFFER
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
import com.github.minigdx.tiny.ColorIndex
import com.github.minigdx.tiny.Pixel
import com.github.minigdx.tiny.engine.Frame
import com.github.minigdx.tiny.engine.GameOptions
import com.github.minigdx.tiny.engine.Operation
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
            2f, // bottom right
            1f,
            0f, // top left
            -1f,
            0f, // bottom left
            1f,
        ),
    )

    private val vertexData = floatArrayOf(
        3f,
        -1f,
        -1f,
        3f,
        -1f,
        -1f,
    )

    override fun init(windowManager: WindowManager): RenderContext {
        // Load and compile the shaders
        //language=Glsl
        val vertexShader = """
        #ifdef GL_ES
            precision highp float;
        #endif
        
        attribute vec3 position;
        attribute vec2 uvs;
        
        varying vec2 viewport;
        
        void main() {
            gl_Position = vec4(position, 1.0);
            viewport = uvs;
        }
        """.trimIndent()

        //language=Glsl
        val fragmentShader = """
        #ifdef GL_ES
            precision highp float;
        #endif
        
        // FIXME: il y a un truc pourri sur comment je passes les instructions
        // Ca fait trop d'iteration a chaque fois
        #define MAX_TEXTURE_WIDTH 64
        #define MAX_TEXTURE_HEIGHT 64
        #define MAX_OPTS (MAX_TEXTURE_WIDTH * MAX_TEXTURE_HEIGHT)
        
        // it goes from 0.0 -> 1.0
        varying vec2 viewport;
        
        uniform sampler2D colors;
        // Opcodes are passed as a texture as it's the best way to pass 
        uniform sampler2D opcodes;
        // Number of opcode. It's also the size of the texture `opcodes`.
        uniform int opcodesSize;
        // Size of the screen in pixel. (resolution of the game * zoom * screen density)
        uniform vec2 screen;
        // Size of the game screen, in pixel, in the game resolution (see _tiny.json)
        uniform vec2 game_screen;
        
        // TODO: generate user texture uniform regarding the number of texture in the game (spritesheets + levels)
        //       warning: what about the "to_sheet" method? -> rajouter 5 blocks pour ça ? 
        
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
        * Read data from the opcodes texture.
        */
        vec4 readOpsAsVec4(int index) {
            return readData(opcodes, index, MAX_TEXTURE_WIDTH, MAX_TEXTURE_HEIGHT);
        }
        
        /**
        * Read data from the opcodes texture. Return the value as int.
        */
        int readOpsAsInt(int index) {
            return int(readOpsAsVec4(index).r * 255.0);
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
        
        // screen.x => 1280
        bool is_pixel(int x, int y) {
            int current_x = int(viewport.x * game_screen.x);
            int current_y = int(viewport.y * game_screen.y);
            
            return x <= current_x && current_x < x + 1 &&
                   y <= current_y && current_y < y + 1; 

        }
        
        vec4 pset(int x, int y, int icolor, vec4 color) {
            if(is_pixel(x, y)) {
                 return readColor(icolor);
            } else {
                return color;
            }
        }
        
        void main() {
            // Number of ops to skip as it's args.
            int opsToSkip = 0;
            vec4 color = vec4(0.0, 0.0, 0.0, 1.0);
            // Index of the pixel in the opcode texture
            // Loop over all pixels (ie: opcode) of the opcodes texture
            // OpenGL ES needs loop that can be evaluated at compile time.
            for (int pixelIndex = 0 ; pixelIndex < MAX_OPTS ; pixelIndex++) {
                // Safe guard: execute only expected ops 
                if(pixelIndex < opcodesSize && opsToSkip == 0) {
                    // Get the base pixel of the opcode. 
                    // It will give the type of the opcode, the number of parameters and the first parameters (if any)
                    int opcode = readOpsAsInt(pixelIndex);
                    
                    // TODO: make the engine generate this part ??
                    if(opcode == 0) { 
                        int arg1 = readOpsAsInt(pixelIndex + 1);
                        color = cls(arg1);
                        opsToSkip = 1;
                    } else if (opcode == 1) {
                        int x = readOpsAsInt(pixelIndex + 1);
                        int y = readOpsAsInt(pixelIndex + 2);
                        int pcolor = readOpsAsInt(pixelIndex + 3);
                        color = pset(x, y, pcolor, color);
                        opsToSkip = 3;
                    } else if (opcode == 2) {
                        opsToSkip = 2;
                    } else {
                        color = vec4(1.0, 0.0, 0.0, 1.0); // RED
                    }
                } else if(opsToSkip > 0) {
                    // it's an arg processed by the current ops.
                    // Let's skip it until we reach the next ops.
                    opsToSkip--;
                }
            }
            
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

        // Prepare the FBO
        val fbo = gl.createFramebuffer()
        gl.bindFramebuffer(GL_FRAMEBUFFER, fbo)

        // Prepare the texture used for the FBO
        val fboTexture = gl.createTexture()
        // Framebuffer of the size of the screen
        val fboBuffer = ByteBuffer(windowManager.screenWidth * windowManager.screenHeight * PixelFormat.RGBA)
        gl.bindTexture(GL_TEXTURE_2D, fboTexture)
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

        gl.bindFramebuffer(GL_FRAMEBUFFER, null)

        // Generate the texture
        val gameTexture = gl.createTexture()
        gl.bindTexture(GL_TEXTURE_2D, gameTexture)

        gl.texParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_S, GL_REPEAT)
        gl.texParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_T, GL_REPEAT)

        gl.texParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_NEAREST)
        gl.texParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_NEAREST)

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
        buffer = ByteArray(256 * 256 * PixelFormat.RGBA)
        var pos = 0
        for (index in 0 until 256) {
            val color = colors.getRGBA(index)

            buffer[pos++] = color[0]
            buffer[pos++] = color[1]
            buffer[pos++] = color[2]
            buffer[pos++] = color[3]
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
            fbo = fbo,
            fboBuffer = fboBuffer,
        )
    }

    override fun draw(context: RenderContext, ops: List<Operation>) {
        context as GLRenderContext

        gl.viewport(
            gameOptions.gutter.first * gameOptions.zoom * context.windowManager.ratioWidth,
            gameOptions.gutter.second * gameOptions.zoom * context.windowManager.ratioHeight,
            gameOptions.width * gameOptions.zoom * context.windowManager.ratioWidth,
            gameOptions.height * gameOptions.zoom * context.windowManager.ratioHeight,
        )

        // -- game screen -- //
        // Push instructions as textures
        gl.activeTexture(GL_TEXTURE0)
        gl.bindTexture(GL_TEXTURE_2D, context.texture)

        // FIXME: for performance reason, it can be created before
        // FIXME: fill up the ops texture. When it's filled, run the batch
        //    and executed the remaining instructions
        val image = ByteArray(64 * 64 * PixelFormat.INDEX)
        // Inject each instruction into the texture
        var index = 0
        ops.forEach { op ->
            check(index < image.size) {
                "Writing too many ops in the ops texture!! " +
                    "You need to use less ops or adjust the engine to support more ops"
            }
            image[index++] = op.type.toByte()
            index = op.write(index, image)
        }

        // FIXME: il y a clairemnt un probleme avec la taille de la texture
        gl.texImage2D(
            target = GL_TEXTURE_2D,
            level = 0,
            internalFormat = GL_R8,
            width = 64, // See Shader for the size of texture
            height = 64, // See Shader for the size of the texture
            border = 0,
            format = GL_RED, // For now: put only one component of the color
            type = GL_UNSIGNED_BYTE,
            buffer = ByteBuffer(image),
        )
        gl.uniform1i(gl.getUniformLocation(context.program, "opcodes")!!, 0)

        // setup shaders
        gl.activeTexture(GL_TEXTURE1)
        gl.bindTexture(GL_TEXTURE_2D, context.colors)
        gl.uniform1i(gl.getUniformLocation(context.program, "colors")!!, 1) // Unité de texture 1 pour 'colors'

        gl.uniform1i(gl.getUniformLocation(context.program, "opcodesSize")!!, index)
        // Set the screen size as screen densitiy resolution dependant.
        gl.uniform2f(
            gl.getUniformLocation(context.program, "screen")!!,
            gameOptions.width.toFloat() * gameOptions.zoom.toFloat() * context.windowManager.ratioWidth.toFloat(),
            gameOptions.height.toFloat() * gameOptions.zoom.toFloat() * context.windowManager.ratioHeight.toFloat(),
        )
        // TODO: ça peut être fait avant car cette resolution ne change jamais!
        gl.uniform2f(
            gl.getUniformLocation(context.program, "game_screen")!!,
            gameOptions.width.toFloat(),
            gameOptions.height.toFloat(),
        )

        gl.clear(GL_COLOR_BUFFER_BIT or GL_DEPTH_BUFFER_BIT)

        gl.clearColor(0f, 0f, 0f, 1.0f)

        gl.drawArrays(GL_TRIANGLES, 0, 3)
    }

    private fun createShader(shader: String, shaderType: Int): Shader {
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

    override fun drawOffscreen(context: RenderContext, ops: List<Operation>): Frame {
        context as GLRenderContext
        gl.bindFramebuffer(GL_FRAMEBUFFER, context.fbo)
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
        gl.bindFramebuffer(GL_FRAMEBUFFER, null)
        return GLFrame(context.fboBuffer, gameOptions)
    }
}

class GLFrame(
    private val buffer: ByteBuffer,
    private val gameOptions: GameOptions,
) : Frame {
    override fun get(x: Pixel, y: Pixel): ColorIndex {
        val i = x * gameOptions.zoom + y * gameOptions.width * gameOptions.zoom * PixelFormat.RGBA
        val result = ByteArray(PixelFormat.RGBA)
        buffer.position = i
        buffer.get(result)
        return gameOptions.colors().fromRGBA(result)
    }
}
