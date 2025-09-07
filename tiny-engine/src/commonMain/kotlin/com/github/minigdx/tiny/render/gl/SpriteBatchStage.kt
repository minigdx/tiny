package com.github.minigdx.tiny.render.gl

import com.danielgergely.kgl.GL_FRAMEBUFFER
import com.danielgergely.kgl.GL_SCISSOR_TEST
import com.danielgergely.kgl.GL_TRIANGLES
import com.danielgergely.kgl.Kgl
import com.github.minigdx.tiny.engine.GameOptions
import com.github.minigdx.tiny.graphic.PixelFormat
import com.github.minigdx.tiny.platform.performance.PerformanceMonitor
import com.github.minigdx.tiny.render.batch.SpriteBatch
import com.github.minigdx.tiny.render.batch.SpriteBatchKey
import com.github.minigdx.tiny.render.shader.FragmentShader
import com.github.minigdx.tiny.render.shader.ShaderProgram
import com.github.minigdx.tiny.render.shader.VertexShader
import com.github.minigdx.tiny.resources.SpriteSheet

class SpriteBatchStage(
    gl: Kgl,
    private val gameOptions: GameOptions,
    private val performanceMonitor: PerformanceMonitor,
) : Stage {
    private val program = ShaderProgram(gl, VShader(), FShader())

    fun init() {
        program.compileShader()
    }

    fun bindTextures(spritesheets: List<SpriteSheet>) {
        program.use()
        program.setup { _, fragmentShader ->
            spritesheets.forEach { spriteSheet ->
                if (spriteSheet.textureUnit == null) {
                    spriteSheet.textureUnit = program.createTexture()
                }
                fragmentShader.spritesheet.applyIndex(
                    spriteSheet.pixels.pixels,
                    spriteSheet.width,
                    spriteSheet.height,
                    spriteSheet.textureUnit,
                )
            }
        }
    }

    override fun startStage() {
        program.use()
        program.disable(GL_SCISSOR_TEST)
    }

    fun execute(
        key: SpriteBatchKey,
        batch: SpriteBatch,
    ) {
        val colorsSwitch = key.palette
        val colorPaletteBuffer = ByteArray(256 * 256 * PixelFormat.RGBA)
        var pos = 0
        for (index in 0 until 256) {
            // Get the pal color
            val pal = colorsSwitch[index % colorsSwitch.size]

            val color = gameOptions.colors().getRGBA(pal)
            colorPaletteBuffer[pos++] = color[0]
            colorPaletteBuffer[pos++] = color[1]
            colorPaletteBuffer[pos++] = color[2]
            colorPaletteBuffer[pos++] = color[3]
        }
        program.use()
        program.setup { vertexShader, fragmentShader ->
            // Vertex shader attributes
            vertexShader.aPos.apply(batch.vertex)
            vertexShader.aSpr.apply(batch.uvs)

            // Vertex shader uniforms
            vertexShader.uViewport.apply(
                gameOptions.width.toFloat(),
                // Flip the vertical
                gameOptions.height.toFloat() * -1,
            )
            vertexShader.uSpritesheet.apply(key.spriteSheet.width.toFloat(), key.spriteSheet.height.toFloat())

            // Fragment shader uniforms
            fragmentShader.paletteColors.applyRGBA(colorPaletteBuffer, 256, 256)

            val textureUnit = key.spriteSheet.textureUnit
            checkNotNull(textureUnit) { "Texture unit should be already initialized!" }
            fragmentShader.spritesheet.applyTexture(textureUnit)
        }

        program.bind()
        program.drawArrays(GL_TRIANGLES, 0, batch.numberOfVertex)
        performanceMonitor.drawCall(batch.numberOfVertex)
        program.unbind()
    }

    override fun endStage() {
        program.disable(GL_SCISSOR_TEST)
        program.bindFramebuffer(GL_FRAMEBUFFER, null)
    }

    class VShader : VertexShader(VERTEX_SHADER) {
        val aPos = inVec2("a_pos") // position of the sprite in the viewport
        val uViewport = uniformVec2("u_viewport") // Size of the viewport; in pixel.
        val uSpritesheet = uniformVec2("u_spritesheet") // Size of the spritesheet; in pixel.
        val aSpr = inVec2("a_spr")

        val vUvs = outVec2("v_uvs")
    }

    class FShader : FragmentShader(FRAGMENT_SHADER) {
        val paletteColors = uniformSample2D("palette_colors")
        val spritesheet = uniformSample2D("spritesheet", existingTexture = true) // Spritesheet to be used.

        val vUvs = inVec2("v_uvs") // position of the sprite in the viewport
    }

    companion object {
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
                
                // UV computation
                // Convert the texture coordinates to NDC coordinates
                vec2 ndc_spr = a_spr / u_spritesheet;
                v_uvs = ndc_spr;
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
            int readPixel(vec2 uvs) {
                vec4 color = texture(spritesheet, uvs);
                return int(color.r * 255.0 + 0.5);
            }
            
            void main() {
                // Read the index color from the current texture.
                int pixel = readPixel(v_uvs);
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
