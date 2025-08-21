package com.github.minigdx.tiny.render.gl

import com.danielgergely.kgl.Kgl
import com.danielgergely.kgl.Texture
import com.github.minigdx.tiny.render.batch.SpriteBatch
import com.github.minigdx.tiny.render.shader.FragmentShader
import com.github.minigdx.tiny.render.shader.ShaderProgram
import com.github.minigdx.tiny.render.shader.VertexShader
import com.github.minigdx.tiny.resources.SpriteSheet

class SpriteBatchStage(gl: Kgl) {

    val frameBuffer: Texture? = null

    private val program = ShaderProgram(gl, VShader(), FShader())

    fun execute(batch: SpriteBatch) {
        // 1. Upload palette color if changed

        batch.key.palette
        // 2. upload vertex along site sprite index


        // 3. setup uniforms (dithering, ...)
        // 4. draw
    }

    fun bindTextures(spritesheets: List<SpriteSheet>) {
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

    class VShader : VertexShader(VERTEX_SHADER) {
        val aPos = inVec2("a_pos") // position of the sprite in the viewport
        val aSpr = inVec2("a_spr")
        val uViewport = uniformVec2("u_viewport") // Size of the viewport; in pixel.
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
        val spritesheet11= uniformSample2D("spritesheet11")
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
            
            void main() {
                if (dither(u_dither, int(v_pos.x), int(v_pos.y))) {
                    int index = int(texture(spritesheet, v_uvs).r * 255.0 + 0.5);
                    vec4 color = readColor(index);
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