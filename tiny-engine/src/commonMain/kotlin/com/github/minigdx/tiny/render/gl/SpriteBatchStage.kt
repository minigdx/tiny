package com.github.minigdx.tiny.render.gl

import com.danielgergely.kgl.ByteBuffer
import com.danielgergely.kgl.GL_BLEND
import com.danielgergely.kgl.GL_COLOR_ATTACHMENT0
import com.danielgergely.kgl.GL_DEPTH24_STENCIL8
import com.danielgergely.kgl.GL_FRAMEBUFFER
import com.danielgergely.kgl.GL_FRAMEBUFFER_COMPLETE
import com.danielgergely.kgl.GL_NEAREST
import com.danielgergely.kgl.GL_RENDERBUFFER
import com.danielgergely.kgl.GL_RGBA
import com.danielgergely.kgl.GL_SCISSOR_TEST
import com.danielgergely.kgl.GL_STENCIL_ATTACHMENT
import com.danielgergely.kgl.GL_TEXTURE_2D
import com.danielgergely.kgl.GL_TEXTURE_MAG_FILTER
import com.danielgergely.kgl.GL_TEXTURE_MIN_FILTER
import com.danielgergely.kgl.GL_TRIANGLES
import com.danielgergely.kgl.GL_UNSIGNED_BYTE
import com.danielgergely.kgl.Kgl
import com.github.minigdx.tiny.engine.GameOptions
import com.github.minigdx.tiny.graphic.Clipper
import com.github.minigdx.tiny.graphic.PixelFormat
import com.github.minigdx.tiny.platform.performance.PerformanceMonitor
import com.github.minigdx.tiny.render.RenderFrame
import com.github.minigdx.tiny.render.batch.SpriteBatch
import com.github.minigdx.tiny.render.shader.FragmentShader
import com.github.minigdx.tiny.render.shader.ShaderProgram
import com.github.minigdx.tiny.render.shader.VertexShader
import com.github.minigdx.tiny.resources.SpriteSheet

class SpriteBatchStage(
    gl: Kgl,
    private val gameOptions: GameOptions,
    private val performanceMonitor: PerformanceMonitor,
) {
    class SpriteBatchState private constructor(
        var dither: Int = 0xFFFF,
        val clipper: Clipper,
    ) {
        constructor(gameOptions: GameOptions) : this(0XFFFF, Clipper(gameOptions.width, gameOptions.height))
    }

    lateinit var frameBufferContext: FrameBufferContext

    private val program = ShaderProgram(gl, VShader(), FShader())

    private val spriteBatchState = SpriteBatchState(gameOptions)

    fun init() {
        program.compileShader()

        // Framebuffer of the size of the screen
        val frameBufferData = ByteBuffer(gameOptions.width * gameOptions.height * PixelFormat.RGBA)

        // Attach stencil buffer to the framebuffer.
        val stencilBuffer = program.createRenderbuffer()
        program.bindRenderbuffer(GL_RENDERBUFFER, stencilBuffer)
        program.renderbufferStorage(GL_RENDERBUFFER, GL_DEPTH24_STENCIL8, gameOptions.width, gameOptions.height)

        val frameBuffer = program.createFramebuffer()
        program.bindFramebuffer(GL_FRAMEBUFFER, frameBuffer)

        program.framebufferRenderbuffer(GL_FRAMEBUFFER, GL_STENCIL_ATTACHMENT, GL_RENDERBUFFER, stencilBuffer)

        // Prepare the texture used for the FBO
        val frameBufferTexture = program.createTexture()
        program.bindTexture(GL_TEXTURE_2D, frameBufferTexture)

        program.texImage2D(
            GL_TEXTURE_2D,
            0,
            GL_RGBA,
            gameOptions.width,
            gameOptions.height,
            0,
            GL_RGBA,
            GL_UNSIGNED_BYTE,
            frameBufferData,
        )
        program.texParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_NEAREST)
        program.texParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_NEAREST)
        program.framebufferTexture2D(GL_FRAMEBUFFER, GL_COLOR_ATTACHMENT0, GL_TEXTURE_2D, frameBufferTexture, 0)

        if (program.checkFramebufferStatus(GL_FRAMEBUFFER) != GL_FRAMEBUFFER_COMPLETE) {
            throw IllegalStateException("Framebuffer is NOT complete!")
        }

        frameBufferContext = FrameBufferContext(
            frameBufferTexture = frameBufferTexture,
            frameBuffer = frameBuffer,
            frameBufferData = frameBufferData,
        )

        val emptyByteArray = ByteArray(8 * 8) { 3 }
        program.setup { _, fragmentShader ->
            fragmentShader.spritesheets.forEach {
                it.applyIndex(emptyByteArray, 8, 8)
            }
        }

        program.enable(GL_BLEND)

        program.disable(GL_SCISSOR_TEST)
        program.bindTexture(GL_TEXTURE_2D, null)
        program.bindFramebuffer(GL_FRAMEBUFFER, null)
    }

    fun bindTextures(spritesheets: List<SpriteSheet>) {
        program.setup { _, fragmentShader ->
            spritesheets.forEach { spriteSheet ->
                fragmentShader.spritesheets[spriteSheet.textureUnit!!].applyIndex(
                    spriteSheet.pixels.pixels,
                    spriteSheet.width,
                    spriteSheet.height,
                )
            }
        }
    }

    fun startStage() {
        program.use()
        program.disable(GL_SCISSOR_TEST)
        program.bindFramebuffer(GL_FRAMEBUFFER, frameBufferContext.frameBuffer)
        // program.enable(GL_SCISSOR_TEST)

        program.viewport(0, 0, gameOptions.width, gameOptions.height)
    }

    fun endStage() {
        program.disable(GL_SCISSOR_TEST)
        program.bindFramebuffer(GL_FRAMEBUFFER, null)
    }

    fun execute(batch: SpriteBatch) {
        val colorsSwitch = batch.key.palette
        val colorPaletteBuffer = ByteArray(256 * 256 * PixelFormat.RGBA)
        var pos = 0
        for (index in 0 until 256) {
            val pal = if (colorsSwitch.isNotEmpty()) {
                // Get the pal color
                colorsSwitch[index % colorsSwitch.size]
            } else {
                // Get the straight color
                index
            }

            val color = gameOptions.colors().getRGBA(pal)
            colorPaletteBuffer[pos++] = color[0]
            colorPaletteBuffer[pos++] = color[1]
            colorPaletteBuffer[pos++] = color[2]
            colorPaletteBuffer[pos++] = color[3]
        }

        program.setup { vertexShader, fragmentShader ->
            // Vertex shader attributes
            vertexShader.aPos.apply(batch.vertex)
            vertexShader.aSpr.apply(batch.uvs)
            vertexShader.aSpritesheet.apply(batch.textureSizes)
            vertexShader.aTextureIndex.apply(batch.textureIndices)

            // Vertex shader uniforms
            vertexShader.uViewport.apply(
                gameOptions.width.toFloat(),
                // Flip the vertical
                gameOptions.height.toFloat() * -1,
            )

            // Fragment shader uniforms
            fragmentShader.paletteColors.applyRGBA(colorPaletteBuffer, 256, 256)

        }

        program.bind()
        program.drawArrays(GL_TRIANGLES, 0, batch.numberOfVertex)
        performanceMonitor.drawCall(batch.numberOfVertex)
        program.unbind()
    }

    fun readFrameBuffer(): RenderFrame {
        program.bindFramebuffer(GL_FRAMEBUFFER, frameBufferContext.frameBuffer)

        program.readPixels(
            0,
            0,
            gameOptions.width,
            gameOptions.height,
            GL_RGBA,
            GL_UNSIGNED_BYTE,
            frameBufferContext.frameBufferData,
        )

        program.bindFramebuffer(GL_FRAMEBUFFER, null)

        val openGLFrame = OpenGLFrame(frameBufferContext.frameBufferData, gameOptions)

        return openGLFrame
    }

    class VShader : VertexShader(VERTEX_SHADER) {
        val aPos = inVec2("a_pos") // position of the sprite in the viewport
        val aSpritesheet = inVec2("a_spritesheet") // Size of the spritesheet; in pixel.
        val uViewport = uniformVec2("u_viewport") // Size of the viewport; in pixel.
        val aTextureIndex = inFloat("a_texture_index") // texture unit index for this sprite
        val aSpr = inVec2("a_spr")

        val vPos = outVec2("v_pos")
        val vUvs = outVec2("v_uvs")
        val vTextureIndex = outFloat("v_texture_index")
    }

    class FShader : FragmentShader(FRAGMENT_SHADER) {
        val paletteColors = uniformSample2D("palette_colors")

        // Spritesheets banks
        val spritesheet0 = uniformSample2D("spritesheet0") // reserved for the primitive
        val spritesheet1 = uniformSample2D("spritesheet1")
        val spritesheet2 = uniformSample2D("spritesheet2")
        val spritesheet3 = uniformSample2D("spritesheet3")
        val spritesheet4 = uniformSample2D("spritesheet4")
        val spritesheet5 = uniformSample2D("spritesheet5")
        val spritesheet6 = uniformSample2D("spritesheet6")
        val spritesheet7 = uniformSample2D("spritesheet7")
        val spritesheet8 = uniformSample2D("spritesheet8")
        val spritesheet9 = uniformSample2D("spritesheet9")

        val spritesheets = listOf(
            spritesheet0,
            spritesheet1,
            spritesheet2,
            spritesheet3,
            spritesheet4,
            spritesheet5,
            spritesheet6,
            spritesheet7,
            spritesheet8,
            spritesheet9,
        )
        val vPos = inVec2("v_pos") // position of the sprite in the viewport
        val vUvs = inVec2("v_uvs") // position of the sprite in the viewport
        val vTextureIndex = inFloat("v_texture_index")
    }

    companion object {
        private const val VERTEX_PER_SPRITE = 6

        // 12 floats are required to generate coordinates for a sprite.
        private const val FLOAT_PER_SPRITE = VERTEX_PER_SPRITE * 2

        //language=Glsl
        private val VERTEX_SHADER =
            """
            void main() {
                vec2 final_pos = a_pos;
                // Convert the pixel coordinates into NDC coordinates
                vec2 ndc_pos = final_pos / u_viewport ;
                // Move the origin to the left/up corner
                vec2 origin_pos = vec2(-1.0, 1.0) + ndc_pos * 2.0;
                
                gl_Position = vec4(origin_pos, 0.0, 1.0);
                
                v_pos = final_pos;
                
                // UV computation
                // Convert the texture coordinates to NDC coordinates
                vec2 ndc_spr = a_spr / a_spritesheet;
                v_uvs = ndc_spr;
                v_texture_index = a_texture_index;
            }
            """.trimIndent()

        //language=Glsl
        private val FRAGMENT_SHADER =
            """
            int imod(int value, int limit) {
                return value - limit * (value / limit);
            }
            /**
            * Extract data from a "kind of" texture1D
            */
            vec4 readData(sampler2D txt, int index, int textureWidth, int textureHeight) {
                int x = imod(index, textureWidth); // index % textureWidth
                int y =  index / textureWidth;
                vec2 uv = vec2((float(x) + 0.5) / float(textureWidth), (float(y) + 0.5) / float(textureHeight));
                return texture(txt, uv);
            }
                
            /**
            * Read a color from the colors texture.
            */
            vec4 readColor(int index) {
                int icolor = imod(index, 256);
                return readData(palette_colors, icolor, 255, 255);
            }
            /**
            * Return the pixel color index at the 
            */
            int readPixel(int textureIndex, vec2 uvs) {
                vec4 color;
                if(textureIndex == 0) {
                     color = texture(spritesheet0, v_uvs);
                } else if (textureIndex == 1) {
                     color = texture(spritesheet1, v_uvs);
                } else if (textureIndex == 2) {
                     color = texture(spritesheet2, v_uvs);
                } else if (textureIndex == 3) {
                     color = texture(spritesheet3, v_uvs);
                } else if (textureIndex == 4) {
                     color = texture(spritesheet4, v_uvs);
                } else if (textureIndex == 5) {
                     color = texture(spritesheet5, v_uvs);
                } else if (textureIndex == 6) {
                     color = texture(spritesheet6, v_uvs);
                } else if (textureIndex == 7) {
                     color = texture(spritesheet7, v_uvs);
                } else if (textureIndex == 8) {
                     color = texture(spritesheet8, v_uvs);
                } else {
                     color = texture(spritesheet9, v_uvs);
                } 
               
                return int(color.r * 255.0 + 0.5);
            }
            
            void main() {
                // Read the index color from the current texture.
                int pixel = readPixel(int(v_texture_index), v_uvs);
                // Read the RGBA color from the index color.
                vec4 color = readColor(pixel);
                if(color.a <= 0.1) {
                    discard;
                } else {
                    fragColor = color; 
                }
            }
            """.trimIndent()
    }
}
