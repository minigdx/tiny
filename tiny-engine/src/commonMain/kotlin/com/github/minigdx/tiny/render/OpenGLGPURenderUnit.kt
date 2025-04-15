package com.github.minigdx.tiny.render

import com.danielgergely.kgl.GL_TRIANGLES
import com.danielgergely.kgl.Kgl
import com.github.minigdx.tiny.engine.DrawSprite
import com.github.minigdx.tiny.engine.GameOptions
import com.github.minigdx.tiny.engine.RenderOperation
import com.github.minigdx.tiny.graphic.PixelFormat
import com.github.minigdx.tiny.log.Logger
import com.github.minigdx.tiny.platform.WindowManager
import com.github.minigdx.tiny.render.shader.FragmentShader
import com.github.minigdx.tiny.render.shader.ShaderProgram
import com.github.minigdx.tiny.render.shader.VertexShader

class OpenGLGPURenderContext(
    val program: ShaderProgram<OpenGLGPURenderUnit.VShader, OpenGLGPURenderUnit.FShader>,
    val windowManager: WindowManager,
) : GPURenderContext

interface GPUOperationRenderUnit {
    fun drawSprite(
        context: GPURenderContext,
        op: DrawSprite,
    )
}

class OpenGLGPURenderUnit(gl: Kgl, logger: Logger, gameOptions: GameOptions) :
    GPUOperationRenderUnit,
    RendererUnit<OpenGLGPURenderContext>(
        gl,
        logger,
        gameOptions,
    ) {
    override fun init(windowManager: WindowManager): OpenGLGPURenderContext {
        val program = ShaderProgram(gl, VShader(), FShader())
        program.compileShader()

        program.vertexShader.uViewport.apply(
            gameOptions.width.toFloat(),
            // Flip the vertical
            gameOptions.height.toFloat() * -1,
        )
        return OpenGLGPURenderContext(
            windowManager = windowManager,
            program = program,
        )
    }

    override fun drawGPU(
        context: OpenGLGPURenderContext,
        ops: List<RenderOperation>,
    ) {
        // Prepare to draw a list of operation, by preparing the shader and the viewport.
        context.program.use()
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
                    // A - Left/Up
                    a.positionLeft, a.positionUp,
                    // A - Right/Up
                    a.positionRight, a.positionUp,
                    // A - Right/Down
                    a.positionRight, a.positionDown,
                    // B - Right/Down
                    a.positionRight, a.positionDown,
                    // B - Left/Down
                    a.positionLeft, a.positionDown,
                    // B - Left/Up
                    a.positionLeft, a.positionUp,
                )
            }.map { it.toFloat() }
                .toFloatArray()

        val spr =
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
            }.flatMap { listOf(it.first, it.second) }
                .map { it.toFloat() }
                .toFloatArray()

        context.program.vertexShader.aPos.apply(vertexData)
        context.program.vertexShader.aSpr.apply(spr)

        context.program.vertexShader.uSpritesheet.apply(
            op.source.width.toFloat(),
            op.source.height.toFloat(),
        )

        context.program.fragmentShader.spritesheet.applyIndex(
            op.source.pixels.pixels,
            op.source.width,
            op.source.height,
        )

        // -- Configuration de l'uniforme palette_colors -- //
        val colors = gameOptions.colors()
        val colorPaletteBuffer = ByteArray(256 * 256 * PixelFormat.RGBA)
        var pos = 0
        for (index in 0 until 256) {
            val color = colors.getRGBA(index)
            colorPaletteBuffer[pos++] = color[0]
            colorPaletteBuffer[pos++] = color[1]
            colorPaletteBuffer[pos++] = color[2]
            colorPaletteBuffer[pos++] = color[3]
        }

        context.program.fragmentShader.paletteColors.applyRGBA(colorPaletteBuffer, 256, 256)

        context.program.vertexShader.uViewport.apply(
            gameOptions.width.toFloat(),
            // Flip the vertical
            gameOptions.height.toFloat() * -1,
        )

        // There is 2 components per vertex. So the number of vertex = number of components / 2
        val nbVertex = (vertexData.size * 0.5).toInt()

        context.program.bind()
        context.program.drawArrays(GL_TRIANGLES, 0, nbVertex)
        context.program.unbind()
    }

    class VShader : VertexShader(VERTEX_SHADER) {
        val aPos = attributeVec2("a_pos") // position of the sprite in the viewport
        val aSpr = attributeVec2("a_spr")
        val uViewport = uniformVec2("u_viewport") // Size of the viewport; in pixel.
        val uSpritesheet = uniformVec2("u_spritesheet") // Size of the viewport; in pixel.
        val vUvs = varyingVec2("v_uvs")
    }

    class FShader : FragmentShader(FRAGMENT_SHADER) {
        val paletteColors = uniformSample2D("palette_colors")
        val spritesheet = uniformSample2D("spritesheet")
        val vUvs = varyingVec2("v_uvs")
    }

    companion object {
        //language=Glsl
        private val VERTEX_SHADER =
            """
            void main() {
                // Convert the pixel coordinates into NDC coordinates
                vec2 ndc_pos = a_pos / u_viewport ;
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
                return readData(palette_colors, icolor, 255, 255);
            }
            
            void main() {
                int index = int(texture2D(spritesheet, v_uvs).r * 255.0 + 0.5);
                gl_FragColor = readColor(index);
            }
            """.trimIndent()
    }
}
