package com.github.minigdx.tiny.render.gl

import com.danielgergely.kgl.GL_TRIANGLES
import com.danielgergely.kgl.Kgl
import com.github.minigdx.tiny.engine.GameOptions
import com.github.minigdx.tiny.graphic.PixelFormat
import com.github.minigdx.tiny.platform.performance.PerformanceMonitor
import com.github.minigdx.tiny.render.batch.PrimitiveBatch
import com.github.minigdx.tiny.render.batch.PrimitiveKey
import com.github.minigdx.tiny.render.shader.FragmentShader
import com.github.minigdx.tiny.render.shader.ShaderProgram
import com.github.minigdx.tiny.render.shader.VertexShader

class PrimitiveBatchStage(
    gl: Kgl,
    private val gameOptions: GameOptions,
    private val performanceMonitor: PerformanceMonitor,
) : Stage {
    private val program = ShaderProgram(gl, VShader(), FShader())

    fun init() {
        program.compileShader()
    }

    private val vertex: FloatArray = floatArrayOf(
        0f, 0f,
        1f, 0f,
        1f, 1f,
        1f, 1f,
        0f, 1f,
        0f, 0f,
    )

    override fun startStage() {
        program.use()

        val colorPaletteBuffer = ByteArray(256 * 256 * PixelFormat.RGBA)
        var pos = 0
        for (index in 0 until 256) {
            val color = gameOptions.colors().getRGBA(index)
            colorPaletteBuffer[pos++] = color[0]
            colorPaletteBuffer[pos++] = color[1]
            colorPaletteBuffer[pos++] = color[2]
            colorPaletteBuffer[pos++] = color[3]
        }

        program.setup { vertexShader, fragmentShader ->
            vertexShader.aPos.apply(vertex)
            vertexShader.aUvs.apply(vertex)

            fragmentShader.paletteColors.applyRGBA(colorPaletteBuffer, 256, 256)
        }
    }

    fun execute(
        key: PrimitiveKey,
        batch: PrimitiveBatch,
    ) {
        program.use()
        program.setup { vertexShader, fragmentShader ->

            // Vertex shader uniforms
            vertexShader.uViewport.apply(
                gameOptions.width.toFloat(),
                // Flip the vertical
                gameOptions.height.toFloat() * -1,
            )

            // vertexShader.aShapeType.apply(batch.parametersType)
            vertexShader.aShadeParams12.apply(batch.parameters12)
            vertexShader.aShadeParams34.apply(batch.parameters34)
            //    vertexShader.aShadeParams56.apply(batch.parameters56)

            fragmentShader.uColor.apply(key.color)
        }

        program.bind()
        program.drawArraysInstanced(GL_TRIANGLES, 0, 6, batch.parametersIndex)
        performanceMonitor.drawCall(6)
        program.unbind()
    }

    override fun endStage() = Unit

    class VShader : VertexShader(VERTEX_SHADER) {
        // val aShapeType = inFloat("a_shapeType").forEachInstance() // Shape type (0=rect, 1=circle, 2=line, 3=rounded rect)
        val aShadeParams12 = inVec2("a_shapeParams12").forEachInstance() // Parameters 1-2 (usually x, y or x1, y1)
        val aShadeParams34 = inVec2("a_shapeParams34").forEachInstance() // Parameters 3-4 (usually width, height or x2, y2)
        // val aShadeParams56 = inVec2("a_shapeParams56").forEachInstance() // Parameters 5-6 (extra params like thickness, corner radius)

        val aPos = inVec2("a_pos") // Position of the shape
        val aUvs = inVec2("a_uvs")
        val uViewport = uniformVec2("u_viewport") // Size of the viewport; in pixel.

        val vLocalPos = outVec2("v_localPos")
        val vUvs = outVec2("v_uvs")

        val vShapeType = outVec2("v_shapeType", flat = true)
        val vParams12 = outVec2("v_params12", flat = true)
        val vParams34 = outVec2("v_params34", flat = true)
        val vParams56 = outVec2("v_params56", flat = true)
    }

    class FShader : FragmentShader(FRAGMENT_SHADER) {
        val paletteColors = uniformSample2D("palette_colors")

        val uColor = uniformInt("u_color") // Color of the shape

        val vUvs = inVec2("v_uvs")
        val vLocaPos = inVec2("v_localPos")
        val vShapeType = inVec2("v_shapeType", flat = true)
        val vParams12 = inVec2("v_params12", flat = true)
        val vParams34 = inVec2("v_params34", flat = true)
        val vParams56 = inVec2("v_params56", flat = true)
    }

    companion object {
        //language=Glsl
        private val VERTEX_SHADER =
            """
            void main() {
                // Scale the rectangle (mutiply by the size) then translate (add the offset)
                vec2 vertex_pos =  ((a_pos * a_shapeParams34) + a_shapeParams12);
                // Convert the pixel coordinates into NDC coordinates
                vec2 ndc_pos = vertex_pos / u_viewport;
                // Move the origin to the left/up corner
                vec2 origin_pos = vec2(-1.0, 1.0) + ndc_pos * 2.0;
                
                gl_Position = vec4(origin_pos, 0.0, 1.0);
                
                // Pass data to fragment shader
                v_uvs = a_uvs;
            }
            """.trimIndent()

        //language=Glsl
        private val FRAGMENT_SHADER =
            """
            int imod(int value, int limit) {
                return value - limit * (value / limit);
            }
            
            vec4 readData(sampler2D txt, int index, int textureWidth, int textureHeight) {
                int x = imod(index, textureWidth);
                int y = index / textureWidth;
                vec2 uv = vec2((float(x) + 0.5) / float(textureWidth), (float(y) + 0.5) / float(textureHeight));
                return texture(txt, uv);
            }
            
            vec4 readColor(int index) {
                int icolor = imod(index, 256);
                return readData(palette_colors, icolor, 255, 255);
            }
            
            float sdfRectangle(vec2 p, vec2 size) {
                vec2 d = abs(p - 0.5) - size * 0.5;
                return length(max(d, 0.0)) + min(max(d.x, d.y), 0.0);
            }
            
            void main() {
                // Calculate SDF for rectangle (UV is in [0,1] range)
                float sdf = sdfRectangle(v_uvs, vec2(1.0));
                
                // Anti-aliasing using smoothstep
                float alpha = 1.0 - smoothstep(-0.01, 0.01, sdf);
                
                // Get color from palette
                vec4 color = readColor(u_color);
                
                if (alpha <= 0.1) {
                    // discard;
                    fragColor = vec4(1.0, 0.0, 0.0, 1.0);
                } else {
                    fragColor = vec4(color.rgb, color.a * alpha);
                    fragColor = vec4(1.0, 0.0, 0.0, 1.0);
                }
                    fragColor = vec4(1.0, 0.0, 0.0, 1.0);
            }
            """.trimIndent()
    }
}
