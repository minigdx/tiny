package com.github.minigdx.tiny.render.gl

import com.danielgergely.kgl.ByteBuffer
import com.danielgergely.kgl.Framebuffer
import com.danielgergely.kgl.GL_COLOR_ATTACHMENT0
import com.danielgergely.kgl.GL_DEPTH24_STENCIL8
import com.danielgergely.kgl.GL_FRAMEBUFFER
import com.danielgergely.kgl.GL_FRAMEBUFFER_COMPLETE
import com.danielgergely.kgl.GL_NEAREST
import com.danielgergely.kgl.GL_RENDERBUFFER
import com.danielgergely.kgl.GL_RGBA
import com.danielgergely.kgl.GL_STENCIL_ATTACHMENT
import com.danielgergely.kgl.GL_TEXTURE_2D
import com.danielgergely.kgl.GL_TEXTURE_MAG_FILTER
import com.danielgergely.kgl.GL_TEXTURE_MIN_FILTER
import com.danielgergely.kgl.GL_TRIANGLES
import com.danielgergely.kgl.GL_UNSIGNED_BYTE
import com.danielgergely.kgl.Kgl
import com.danielgergely.kgl.Texture
import com.github.minigdx.tiny.engine.GameOptions
import com.github.minigdx.tiny.graphic.PixelFormat
import com.github.minigdx.tiny.platform.performance.PerformanceMonitor
import com.github.minigdx.tiny.render.batch.SpriteBatch
import com.github.minigdx.tiny.render.shader.FragmentShader
import com.github.minigdx.tiny.render.shader.ShaderProgram
import com.github.minigdx.tiny.render.shader.VertexShader
import com.github.minigdx.tiny.resources.SpriteSheet

data class FrameBufferContext2(
    /**
     * Framebuffer texture.
     * Use to draw the framebuffer on the screen.
     */
    var frameBufferTexture: Texture,
    /**
     * Reference to the framebuffer.
     * Used to bind the framebuffer in the GPU context.
     */
    var frameBuffer: Framebuffer,
    /**
     * Data in which the Framebuffer will be written.
     * Used to read the rendered framebuffer.
     */
    var frameBufferData: ByteBuffer,
)

class SpriteBatchStage(gl: Kgl, private val gameOptions: GameOptions, private val performanceMonitor: PerformanceMonitor) {
    lateinit var frameBufferContext: FrameBufferContext2

    private val program = ShaderProgram(gl, VShader(), FShader())

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
        program.bindTexture(GL_TEXTURE_2D, null)
        program.bindFramebuffer(GL_FRAMEBUFFER, null)

        frameBufferContext = FrameBufferContext2(
            frameBufferTexture, frameBuffer, frameBufferData,
        )
    }

    fun bindTextures(spritesheets: List<SpriteSheet>) {
        program.use()
        spritesheets.forEach { texture: SpriteSheet ->

            val textureUnit = texture.textureUnit
            checkNotNull(textureUnit) { "Spritesheet is expected to have a texture unit" }
            program.fragmentShader.spritesheets[textureUnit].applyIndex(
                texture.pixels.pixels,
                texture.width,
                texture.height,
            )
        }
    }

    fun execute(batch: SpriteBatch) {
        program.use()

        // 1. Upload palette color if changed
        // -- Set the color palette -- //
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

        // FIXME: TO BE REMOVED. ONLY FOR TESTING
        program.fragmentShader.spritesheets.forEach {
            it.applyRGBA(colorPaletteBuffer, 256, 256)
        }

        program.fragmentShader.paletteColors.applyRGBA(colorPaletteBuffer, 256, 256)

        // 2. upload vertex along site sprite index
        program.vertexShader.aPos.apply(batch.vertex)
        program.vertexShader.aSpr.apply(batch.uvs)

        // FIXME: TODO: aSpr + type de sprite pour savoir quel sprite bank utiliser ?
        // 3. setup uniforms (dithering, ...)
        // 4. draw
        program.bind()
        program.drawArrays(GL_TRIANGLES, 0, batch.numberOfVertex)
        performanceMonitor.drawCall(batch.numberOfVertex)
        program.unbind()
    }

    class VShader : VertexShader(VERTEX_SHADER) {
        val aPos = inVec2("a_pos") // position of the sprite in the viewport
        val aSpr = inVec2("a_spr")
        val uViewport = uniformVec2("u_viewport") // Size of the viewport; in pixel.

        // FIXME: pass the spritesheets size
        val uSpritesheet = uniformVec2("u_spritesheet") // Size of the spritesheet; in pixel.
        val uCamera = uniformVec2("u_camera") // Position of the camera (offset)

        // FIXME(Performance): at the texture unit: spritesheet, system or primitives ?
        val vUvs = outVec2("v_uvs")
        val vPos = outVec2("v_pos")
    }

    class FShader : FragmentShader(FRAGMENT_SHADER) {
        // FIXME(Performance): When the primitives is drawn, reset it.
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
        val spritesheet10 = uniformSample2D("spritesheet10")
        val spritesheet11 = uniformSample2D("spritesheet11")
        val spritesheet12 = uniformSample2D("spritesheet12")
        val spritesheet13 = uniformSample2D("spritesheet13")
        val spritesheet14 = uniformSample2D("spritesheet14")
        val spritesheet15 = uniformSample2D("spritesheet15")
        val spritesheet16 = uniformSample2D("spritesheet16")

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
            spritesheet10,
            spritesheet11,
            spritesheet12,
            spritesheet13,
            spritesheet14,
            spritesheet15,
            spritesheet16,
        )

        val uDither = uniformInt("u_dither")

        val vUvs = inVec2("v_uvs")
        val vPos = inVec2("v_pos")
    }

    companion object {
        private const val VERTEX_PER_SPRITE = 6
        private const val FLOAT_PER_SPRITE =
            VERTEX_PER_SPRITE * 2 // 12 floats are required to generate coordinates for a sprite.

        //language=Glsl
        private val VERTEX_SHADER =
            """
            void main() {
                vec2 final_pos = (a_pos - u_camera);
                // Convert the pixel coordinates into NDC coordinates
                vec2 ndc_pos = final_pos / u_viewport ;
                // Move the origin to the left/up corner
                vec2 origin_pos = vec2(-1.0, 1.0) + ndc_pos * 2.0;
                
                gl_Position = vec4(origin_pos, 0.0, 1.0);
                
                // UV computation
                // Convert the texture coordinates to NDC coordinates
                vec2 ndc_spr = a_spr / u_spritesheet;
                v_uvs = ndc_spr;
                
                v_pos = final_pos;
            }
            """.trimIndent()

        //language=Glsl
        private val FRAGMENT_SHADER =
            """
            int imod(int value, int limit) {
                return value - limit * (value / limit);
            }
            
            bool dither(int pattern, int x, int y) {
                  int a = imod(x,  4);
                  int b = imod(y, 4) * 4;
                  int bitPosition = a + b;
                  
                  float powerOfTwo = pow(2.0, float(bitPosition));
                  int bit = int(floor(mod(float(pattern) / powerOfTwo, 2.0)));
                   
                  return bit > 0;
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
                }  else if (textureIndex == 1) {
                     color = texture(spritesheet1, v_uvs);
                } else {
                     color = texture(spritesheet16, v_uvs);
                }
                
                return int(color.r * 255.0 + 0.5);
            }
                         
            void main() {
                if (dither(u_dither, int(v_pos.x), int(v_pos.y))) {
                    int pixel = readPixel(0, v_uvs);
                    vec4 color = readColor(pixel);
                    if(color.a <= 0.1) {
                        discard;
                    } else {
                        fragColor = color; 
                    }
                } else {
                    discard;
                }
            }
            """.trimIndent()
    }
}
