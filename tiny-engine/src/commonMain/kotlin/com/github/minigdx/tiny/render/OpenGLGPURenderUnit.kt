package com.github.minigdx.tiny.render

import com.danielgergely.kgl.FloatBuffer
import com.danielgergely.kgl.GL_ARRAY_BUFFER
import com.danielgergely.kgl.GL_FALSE
import com.danielgergely.kgl.GL_FLOAT
import com.danielgergely.kgl.GL_FRAGMENT_SHADER
import com.danielgergely.kgl.GL_FRAMEBUFFER
import com.danielgergely.kgl.GL_LINK_STATUS
import com.danielgergely.kgl.GL_STATIC_DRAW
import com.danielgergely.kgl.GL_TRIANGLE_FAN
import com.danielgergely.kgl.GL_VERTEX_SHADER
import com.danielgergely.kgl.GlBuffer
import com.danielgergely.kgl.Kgl
import com.danielgergely.kgl.Program
import com.danielgergely.kgl.Texture
import com.github.minigdx.tiny.engine.DrawSprite
import com.github.minigdx.tiny.engine.GameOptions
import com.github.minigdx.tiny.engine.RenderOperation
import com.github.minigdx.tiny.graphic.PixelFormat
import com.github.minigdx.tiny.log.Logger
import com.github.minigdx.tiny.platform.WindowManager

class OpenGLGPURenderContext(
    val program: Program,
    val windowManager: WindowManager,
    val vertexData: GlBuffer,
    val colorPaletteTexture: Texture?,
    val spritesheet: Texture?,
) : GPURenderContext

interface GPUOperationRenderUnit {
    fun drawSprite(
        context: GPURenderContext,
        op: DrawSprite,
    )
}

class OpenGLGPURenderUnit(gl: Kgl, logger: Logger, gameOptions: GameOptions) :
    RendererUnit<OpenGLGPURenderContext>(
        gl,
        logger,
        gameOptions,
    ),
    GPUOperationRenderUnit {
    private val colorPaletteBuffer = ByteArray(256 * 256 * PixelFormat.RGBA)

    override fun init(windowManager: WindowManager): OpenGLGPURenderContext {
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

        val stride = NB_ATTRIBUTES * Float.SIZE_BYTES // vec2(a_pos) + vec2(a_spr) + vec2(a_flip) = 6 components

        val vertexData = gl.createBuffer()
        gl.bindBuffer(GL_ARRAY_BUFFER, vertexData)
        val position = gl.getAttribLocation(shaderProgram, "a_pos")
        gl.vertexAttribPointer(
            location = position,
            size = 2,
            type = GL_FLOAT,
            normalized = false,
            stride = 0,
            offset = 0,
        )
        gl.enableVertexAttribArray(position)

        gl.uniform2f(gl.getUniformLocation(shaderProgram, "u_viewport")!!, gameOptions.width.toFloat(), gameOptions.height.toFloat())

        /*
        val sprite = gl.getAttribLocation(shaderProgram, "a_spr")
        gl.vertexAttribPointer(
            location = sprite,
            size = 2,
            type = GL_FLOAT,
            normalized = false,
            stride = stride,
            offset = 2 * Float.SIZE_BYTES,
        )
        gl.enableVertexAttribArray(sprite)

        val flip = gl.getAttribLocation(shaderProgram, "a_flip")
        gl.vertexAttribPointer(
            location = flip,
            size = 2,
            type = GL_FLOAT,
            normalized = false,
            stride = stride,
            offset = 4 * Float.SIZE_BYTES,
        )
        gl.enableVertexAttribArray(flip)
*/

        // TODO: setup all textures.
        return OpenGLGPURenderContext(
            program = shaderProgram,
            vertexData = vertexData,
            colorPaletteTexture = null,
            spritesheet = null,
            windowManager = windowManager,
        )
    }

    override fun drawGPU(
        context: OpenGLGPURenderContext,
        ops: List<RenderOperation>,
    ) {
        // Prepare to draw a list of operation, by preparing the shader and the viewport.
        gl.useProgram(context.program)

        ops.forEach { op -> op.executeGPU(context, this) }
    }

    /**
     * Execture the operation of drawing a sprite
     */
    override fun drawSprite(
        context: GPURenderContext,
        op: DrawSprite,
    ) {
        context as OpenGLGPURenderContext

        val vertexData =
            op.attributes.flatMap { a ->
                listOf(
                    a.destinationX,
                    a.destinationY,
                    a.destinationX + a.sourceWidth,
                    a.destinationY,
                    a.destinationX + a.sourceWidth,
                    a.destinationY + a.sourceHeight,
                    a.destinationX,
                    a.destinationY + a.sourceHeight,
                )

            }.map { it.toFloat() }
                .toFloatArray()
/*
        var attrIndex = 0
        val vertexData = Array(op.attributes.size * NB_ATTRIBUTES * 4) { index ->
            val i = index % (NB_ATTRIBUTES * 4)
            val attribute = op.attributes[attrIndex]
            if (i == 23) { // last index, let switch to the next attribute
                attrIndex++
            }
            when (i) {
                // First vertex
                0 -> attribute.destinationX.toFloat()
                1 -> attribute.destinationY.toFloat()
                2 -> attribute.sourceX.toFloat()
                3 -> attribute.sourceY.toFloat()
                4 -> if (attribute.flipX) 1f else 0f
                5 -> if (attribute.flipY) 1f else 0f

                // Second vertex
                6 -> 0.5f // (attribute.destinationX + attribute.sourceWidth).toFloat()
                7 -> -0.5f // attribute.destinationY.toFloat()
                8 -> (attribute.sourceX + attribute.sourceWidth).toFloat()
                9 -> attribute.sourceY.toFloat()
                10 -> if (attribute.flipX) 1f else 0f
                11 -> if (attribute.flipY) 1f else 0f

                // Third vertex
                12 -> 0.5f // (attribute.destinationX + attribute.sourceWidth).toFloat()
                13 -> 0.5f // (attribute.destinationY + attribute.sourceHeight).toFloat()
                14 -> (attribute.sourceX + attribute.sourceWidth).toFloat()
                15 -> (attribute.sourceY + attribute.sourceHeight).toFloat()
                16 -> if (attribute.flipX) 1f else 0f
                17 -> if (attribute.flipY) 1f else 0f

                // Fourth vertex
                18 -> -0.5f // attribute.destinationX.toFloat()
                19 -> 0.5f // (attribute.destinationY + attribute.sourceHeight).toFloat()
                20 -> attribute.sourceX.toFloat()
                21 -> (attribute.sourceY + attribute.sourceHeight).toFloat()
                22 -> if (attribute.flipX) 1f else 0f
                23 -> if (attribute.flipY) 1f else 0f
                // Not expected
                else -> throw IllegalArgumentException()
            }
        }
*/
        gl.bindBuffer(GL_ARRAY_BUFFER, context.vertexData)
        gl.bufferData(GL_ARRAY_BUFFER, FloatBuffer(vertexData), vertexData.size * Float.SIZE_BYTES, GL_STATIC_DRAW)

        gl.drawArrays(GL_TRIANGLE_FAN, 0, 4)
    }

    companion object {
        private const val NB_ATTRIBUTES = 6

        //language=Glsl
        private val VERTEX_SHADER =
            """
            #ifdef GL_ES
                precision highp float;
            #endif
            
            /**
            *     a_pos = (10, 20)
            *     a_size = (8,8)
            *     a_spr = (24, 52)
            *     a_flip = (1, 1)
            *    u_viewport = (320, 180)
            *   u_spritesheet = (64, 64) 
            *   v_uv with UV -> (0, 0) -> (1, 1)
            
            10 -> 20 ; 20 à 10
            *    - convertir a_pos en NDC   vec2(0.0, 1.0) - (a_pos / u_viewport)
                 - convertir a_spr en NDC   a_spr / u_spritesheet
                 - flip rendering : v_uv = a_flip - ndc_spr * a_flip  
                 
                 
                 
            **/
            attribute vec2 a_pos;    // position of the sprite in the viewport.
            attribute vec2 a_spr;   // position of the sprite in the sprite sheet.
            attribute vec2 a_flip;   // (0, 0) = normal ; (1, 0) = flipH, etc
            
            uniform vec2 u_viewport; // Size of the viewport; in pixel.
            uniform vec2 u_spritesheet; // Size of the spritesheet; in pixel
            
            varying vec2 v_uv;
            
            void main() {
                // Convert the pixel coordinates into NDC coordinates
                vec2 ndc_pos = a_pos / u_viewport - vec2(1.0, 1.0) ;
                
                gl_Position = vec4(ndc_pos, 0.0, 1.0);
            
                // UV computation
                // Convert the texture coordinates to NDC coordinates
                vec2 ndc_spr = a_spr / u_spritesheet;
                v_uv = a_flip - ndc_spr; // managing flip
                
            }
            """.trimIndent()

        //language=Glsl
        private val FRAGMENT_SHADER =
            """
            #ifdef GL_ES
                precision highp float;
            #endif
                    
            varying vec2 v_uv;
            
            // Color palette
            uniform sampler2D palette_colors;
            
            uniform sampler2D spritesheet;
            
            
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
                return readData(palette_colors, icolor, 256, 256);
            }
            
            void main() {
                int index = int(texture2D(spritesheet, v_uv).r * 255.0 + 0.5);
                gl_FragColor = readColor(index);
                // = vec4(float(index / 255), 0.0, 1.0, 1.0);
                
                gl_FragColor = vec4(0.0, 0.0, 1.0, 1.0);
            }
            """.trimIndent()
    }
}
