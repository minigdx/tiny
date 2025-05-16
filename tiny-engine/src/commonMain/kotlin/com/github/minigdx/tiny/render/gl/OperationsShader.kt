package com.github.minigdx.tiny.render.gl

import com.danielgergely.kgl.GL_ALWAYS
import com.danielgergely.kgl.GL_BLEND
import com.danielgergely.kgl.GL_DEPTH_TEST
import com.danielgergely.kgl.GL_EQUAL
import com.danielgergely.kgl.GL_KEEP
import com.danielgergely.kgl.GL_NOTEQUAL
import com.danielgergely.kgl.GL_ONE
import com.danielgergely.kgl.GL_ONE_MINUS_SRC_ALPHA
import com.danielgergely.kgl.GL_REPLACE
import com.danielgergely.kgl.GL_ALWAYS
import com.danielgergely.kgl.GL_BLEND
import com.danielgergely.kgl.GL_DEPTH_TEST
import com.danielgergely.kgl.GL_EQUAL
import com.danielgergely.kgl.GL_KEEP
import com.danielgergely.kgl.GL_NOTEQUAL
import com.danielgergely.kgl.GL_ONE
import com.danielgergely.kgl.GL_ONE_MINUS_SRC_ALPHA
import com.danielgergely.kgl.GL_REPLACE
import com.danielgergely.kgl.GL_SCISSOR_TEST
import com.danielgergely.kgl.GL_SRC_ALPHA
import com.danielgergely.kgl.GL_STENCIL_TEST
import com.danielgergely.kgl.GL_TRIANGLES
import com.danielgergely.kgl.GL_ZERO
import com.danielgergely.kgl.Kgl
import com.github.minigdx.tiny.engine.GameOptions
import com.github.minigdx.tiny.graphic.PixelFormat
import com.github.minigdx.tiny.log.Logger
import com.github.minigdx.tiny.platform.DrawingMode
import com.github.minigdx.tiny.platform.WindowManager
import com.github.minigdx.tiny.render.NopRenderContext
import com.github.minigdx.tiny.render.OperationsRender
import com.github.minigdx.tiny.render.RenderContext
import com.github.minigdx.tiny.render.WriteRender
import com.github.minigdx.tiny.render.operations.DrawSprite
import com.github.minigdx.tiny.render.operations.DrawSprite.Companion.MAX_SPRITE_PER_COMMAND
import com.github.minigdx.tiny.render.operations.DrawingModeOperation
import com.github.minigdx.tiny.render.operations.RenderOperation
import com.github.minigdx.tiny.render.shader.FragmentShader
import com.github.minigdx.tiny.render.shader.ShaderProgram
import com.github.minigdx.tiny.render.shader.VertexShader

class OperationsShader(
    private val gl: Kgl,
    private val logger: Logger,
    private val gameOptions: GameOptions,
) : OperationsRender, WriteRender {
    private val program = ShaderProgram(gl, VShader(), FShader())

    override fun init(windowManager: WindowManager): RenderContext {
        program.compileShader()
        return NopRenderContext
    }

    override fun render(
        context: RenderContext,
        ops: List<RenderOperation>,
    ) {
        // Prepare to draw a list of operation, by preparing the shader and the viewport.
        program.use()
        program.disable(GL_DEPTH_TEST)
        ops.forEach { op -> op.executeGPU(context, this) }
    }

    private val vertexData = FloatArray(MAX_SPRITE_PER_COMMAND * FLOAT_PER_SPRITE)
    private val spr = FloatArray(MAX_SPRITE_PER_COMMAND * FLOAT_PER_SPRITE)

    /**
     * Execute the operation of drawing a sprite
     */
    override fun drawSprite(
        context: RenderContext,
        op: DrawSprite,
    ) {
        var indexVertex = 0
        op.attributes.forEach { a ->
            // A - Left/Up
            vertexData[indexVertex++] = a.positionLeft.toFloat()
            vertexData[indexVertex++] = a.positionUp.toFloat()
            // A - Right/Up
            vertexData[indexVertex++] = a.positionRight.toFloat()
            vertexData[indexVertex++] = a.positionUp.toFloat()
            // A - Right/Down
            vertexData[indexVertex++] = a.positionRight.toFloat()
            vertexData[indexVertex++] = a.positionDown.toFloat()
            // B - Right/Down
            vertexData[indexVertex++] = a.positionRight.toFloat()
            vertexData[indexVertex++] = a.positionDown.toFloat()
            // B - Left/Down
            vertexData[indexVertex++] = a.positionLeft.toFloat()
            vertexData[indexVertex++] = a.positionDown.toFloat()
            // B - Left/Up
            vertexData[indexVertex++] = a.positionLeft.toFloat()
            vertexData[indexVertex++] = a.positionUp.toFloat()
        }

        indexVertex = 0
        op.attributes.flatMap { a ->
            // A - Left/Up
            val v1 = a.uvLeft to a.uvUp
            // A - Right/Up
            val v2 = a.uvRight to a.uvUp
            // A - Right/Down
            val v3 = a.uvRight to a.uvDown
            // B - Right/Down
            val va = a.uvRight to a.uvDown
            // B - Left/Down
            val vb = a.uvLeft to a.uvDown
            // B - Left/Up
            val vc = a.uvLeft to a.uvUp

            if (!a.flipX && !a.flipY) {
                listOf(v1, v2, v3, va, vb, vc)
            } else if (a.flipX && !a.flipY) {
                listOf(v2, v1, vb, vb, va, v2)
            } else if (!a.flipX && a.flipY) {
                listOf(vb, va, v2, v2, vc, vb)
            } else {
                listOf(va, vb, vc, v1, v2, v3)
            }
        }.forEach { (x, y) ->
            spr[indexVertex++] = x.toFloat()
            spr[indexVertex++] = y.toFloat()
        }

        program.vertexShader.aPos.apply(vertexData)
        program.vertexShader.aSpr.apply(spr)

        val source = op.source!!

        program.vertexShader.uSpritesheet.apply(
            source.width.toFloat(),
            source.height.toFloat(),
        )

        program.fragmentShader.spritesheet.applyIndex(
            source.pixels.pixels,
            source.width,
            source.height,
        )

        // -- Set the color palette -- //
        val colors = gameOptions.colors()
        val colorPaletteBuffer = ByteArray(256 * 256 * PixelFormat.RGBA)
        var pos = 0
        for (index in 0 until 256) {
            val pal = if (op.pal.isNotEmpty()) {
                // Get the pal color
                op.pal[index % op.pal.size]
            } else {
                // Get the straight color
                index
            }

            val color = colors.getRGBA(pal)
            colorPaletteBuffer[pos++] = color[0]
            colorPaletteBuffer[pos++] = color[1]
            colorPaletteBuffer[pos++] = color[2]
            colorPaletteBuffer[pos++] = color[3]
        }

        program.fragmentShader.paletteColors.applyRGBA(colorPaletteBuffer, 256, 256)
        program.fragmentShader.uDither.apply(op.dither)

        program.vertexShader.uViewport.apply(
            gameOptions.width.toFloat(),
            // Flip the vertical
            gameOptions.height.toFloat() * -1,
        )

        when (val camera = op.camera) {
            null -> program.vertexShader.uCamera.apply(0f, 0f)
            else -> program.vertexShader.uCamera.apply(camera.x.toFloat(), camera.y.toFloat())
        }

        op.clipper?.run {
            gl.enable(GL_SCISSOR_TEST)
            val width = this.right - this.left
            val height = this.bottom - this.top
            gl.scissor(
                x = this.left,
                y = gameOptions.height - this.top - height,
                width = width,
                height = height,
            )
        }

        // There is 2 components per vertex. So the number of vertex = number of components / 2
        val nbVertex = VERTEX_PER_SPRITE * op.attributes.size

        program.bind()
        program.drawArrays(GL_TRIANGLES, 0, nbVertex)
        program.unbind()

        gl.disable(GL_SCISSOR_TEST)

        op.release()
    }

    override fun setDrawingMode(
        context: RenderContext,
        op: DrawingModeOperation,
    ) {
        when (op.mode) {
            DrawingMode.DEFAULT -> {
                gl.enable(GL_BLEND)
                gl.disable(GL_STENCIL_TEST)
                gl.blendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA)
                gl.colorMask(true, true, true, true)
            }
            DrawingMode.ALPHA_BLEND -> {
                gl.enable(GL_BLEND)
                gl.disable(GL_STENCIL_TEST)
                gl.blendFuncSeparate(GL_ZERO, GL_ONE, GL_ZERO, GL_ONE_MINUS_SRC_ALPHA)
                gl.colorMask(true, true, true, true)
            }
            DrawingMode.STENCIL_WRITE -> {
                gl.enable(GL_STENCIL_TEST)
                gl.stencilOp(GL_KEEP, GL_KEEP, GL_REPLACE)
                gl.stencilFunc(GL_ALWAYS, 1, 0xFF)
                gl.stencilMask(0xFF)
                // Don't write the actual sprite in the color buffer
                gl.colorMask(false, false, false, false)
            }
            DrawingMode.STENCIL_TEST -> {
                gl.enable(GL_STENCIL_TEST)
                gl.stencilFunc(GL_EQUAL, 1, 0xFF)
                gl.stencilMask(0x00)
                gl.stencilOp(GL_KEEP, GL_KEEP, GL_KEEP)
                gl.enable(GL_BLEND)
                gl.blendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA)
                gl.colorMask(true, true, true, true)
            }
            DrawingMode.STENCIL_NOT_TEST -> {
                gl.enable(GL_STENCIL_TEST)
                gl.stencilFunc(GL_NOTEQUAL, 1, 0xFF)
                gl.stencilMask(0x00)
                gl.stencilOp(GL_KEEP, GL_KEEP, GL_KEEP)
                gl.enable(GL_BLEND)
                gl.blendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA)
                gl.colorMask(true, true, true, true)
            }
        }
    }

    class VShader : VertexShader(VERTEX_SHADER) {
        val aPos = attributeVec2("a_pos") // position of the sprite in the viewport
        val aSpr = attributeVec2("a_spr")
        val uViewport = uniformVec2("u_viewport") // Size of the viewport; in pixel.
        val uSpritesheet = uniformVec2("u_spritesheet") // Size of the viewport; in pixel.
        val uCamera = uniformVec2("u_camera") // Position of the camera (offset)

        val vUvs = varyingVec2("v_uvs")
        val vPos = varyingVec2("v_pos")
    }

    class FShader : FragmentShader(FRAGMENT_SHADER) {
        val paletteColors = uniformSample2D("palette_colors")
        val spritesheet = uniformSample2D("spritesheet")
        val uDither = uniformInt("u_dither")

        val vUvs = varyingVec2("v_uvs")
        val vPos = varyingVec2("v_pos")
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
                return texture2D(txt, uv);
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
                    int index = int(texture2D(spritesheet, v_uvs).r * 255.0 + 0.5);
                    vec4 color = readColor(index);
                    if(color.a <= 0.1) {
                        discard;
                    } else {
                        gl_FragColor = color; 
                    }
                } else {
                    discard;
                }
            }
            """.trimIndent()
    }
}
