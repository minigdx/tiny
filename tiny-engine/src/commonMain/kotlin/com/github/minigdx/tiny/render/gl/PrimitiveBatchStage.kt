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

            vertexShader.aShapeType.apply(batch.parametersType)
            vertexShader.aShadeParams12.apply(batch.parameters12)
            vertexShader.aShadeParams34.apply(batch.parameters34)
            vertexShader.aShadeParams56.apply(batch.parameters56)

            fragmentShader.uColor.apply(key.color)
        }

        program.bind()
        program.drawArraysInstanced(GL_TRIANGLES, 0, 6, batch.parametersIndex)
        performanceMonitor.drawCall(6)
        program.unbind()
    }

    override fun endStage() = Unit

    class VShader : VertexShader(VERTEX_SHADER) {
        val aShapeType = inFloat("a_shapeType").forEachInstance() // Shape type (0=rect, 1=circle, 2=line, 3=rounded rect)
        val aShadeParams12 = inVec2("a_shapeParams12").forEachInstance() // Parameters 1-2 (usually x, y or x1, y1)
        val aShadeParams34 = inVec2("a_shapeParams34").forEachInstance() // Parameters 3-4 (usually width, height or x2, y2)
        val aShadeParams56 = inVec2("a_shapeParams56").forEachInstance() // Parameters 5-6 (extra params like thickness, corner radius)

        val aPos = inVec2("a_pos") // Position of the shape
        val aUvs = inVec2("a_uvs")
        val uViewport = uniformVec2("u_viewport") // Size of the viewport; in pixel.

        val vLocalPos = outVec2("v_localPos")
        val vUvs = outVec2("v_uvs")

        val vShapeType = outFloat("v_shapeType", flat = true)
        val vParams12 = outVec2("v_shapeParams12", flat = true)
        val vParams34 = outVec2("v_shapeParams34", flat = true)
        val vParams56 = outVec2("v_shapeParams56", flat = true)
    }

    class FShader : FragmentShader(FRAGMENT_SHADER) {
        val paletteColors = uniformSample2D("palette_colors")

        val uColor = uniformInt("u_color") // Color of the shape

        val vUvs = inVec2("v_uvs")
        val vLocaPos = inVec2("v_localPos")
        val vShapeType = inFloat("v_shapeType", flat = true)
        val vShapeParams12 = inVec2("v_shapeParams12", flat = true)
        val vShapeParams34 = inVec2("v_shapeParams34", flat = true)
        val vParams56 = inVec2("v_shapeParams56", flat = true)
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
                v_shapeType = a_shapeType;
                v_shapeParams12 = a_shapeParams12;
                v_shapeParams34 = a_shapeParams34;
                v_shapeParams56 = a_shapeParams56;
            }
            """.trimIndent()

        //language=Glsl
        private val FRAGMENT_SHADER =
            """
            #define T_RECT 0
            #define T_CIRCLE 2
            #define T_LINE 3
            #define T_RECTF 240
            #define T_CIRCLEF 242
                
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
            
            float sdfCircle(vec2 p, vec2 center, float radius) {
                return length(p - center) - radius;
            }
            
            float sdfCircleBorder(vec2 p, vec2 center, float radius, float thickness) {
                float dist = length(p - center) - radius;
                return abs(dist) - thickness * 0.5;
            }
            
            float sdfRectangleBorder(vec2 p, vec2 size, float thickness) {
                vec2 d = abs(p - 0.5) - size * 0.5;
                float rectSDF = length(max(d, 0.0)) + min(max(d.x, d.y), 0.0);
                
                // Convert to border: distance to edge, minus half thickness
                return abs(rectSDF) - thickness * 0.5;
            }
            
            
            float sdfLine(vec2 p, vec2 a, vec2 b, float thickness) {
                vec2 pa = p - a;
                vec2 ba = b - a;
                float h = clamp(dot(pa, ba) / dot(ba, ba), 0.0, 1.0);
                return length(pa - ba * h) - thickness * 0.5;
            }
            
            void main() {
                vec2 sdf;
                if(int(v_shapeType) == T_LINE) {
                    vec2 lineStart = v_shapeParams12;
                    vec2 lineEnd = v_shapeParams56;
                    // Fragment position in pixels
                    vec2 fragCoord = v_uvs * v_shapeParams34 + v_shapeParams12;
                    
                    // Convertir en coordonnées entières de pixels
                    ivec2 p = ivec2(floor(fragCoord + 0.5));
                    ivec2 p0 = ivec2(floor(lineStart + 0.5));
                    ivec2 p1 = ivec2(floor(lineEnd + 0.5));
                    
                    // Vérifier les bornes d'abord
                    if (p.x < min(p0.x, p1.x) || p.x > max(p0.x, p1.x) ||
                        p.y < min(p0.y, p1.y) || p.y > max(p0.y, p1.y)) {
                        discard;
                    }
                    
                    // Test de Bresenham
                    ivec2 d = p1 - p0;
                    ivec2 pp = p - p0;
                    
                    // Condition de Bresenham : le pixel est sur la ligne si
                    // la distance perpendiculaire est <= 0.5 pixel
                    int cross = abs(pp.x * d.y - pp.y * d.x);
                    int threshold = max(abs(d.x), abs(d.y));
                    
                    if (float(cross) <= float(threshold) / 2.0) {
                        sdf = vec2(0.0);
                    } else {
                        sdf = vec2(1.5);
                    }
                } else if(int(v_shapeType) == T_CIRCLE) {
                    sdf = sdfCircleBorder(v_uvs, vec2(0.5, 0.5), 0.5, 0.0) * v_shapeParams34;
                } else if(int(v_shapeType) == T_CIRCLEF) {
                    sdf = sdfCircle(v_uvs, vec2(0.5, 0.5), 0.5) * v_shapeParams34;
                } else if(int(v_shapeType) == T_RECTF) {
                    sdf = sdfRectangle(v_uvs, vec2(1.0)) * v_shapeParams34;
                } else {
                    // Calculate SDF for rectangle (UV is in [0,1] range)
                    sdf = sdfRectangleBorder(v_uvs, vec2(1.0), 0.0) * v_shapeParams34;
                }
                
                // The distance from the rectangle is more than one pixel away (ie: it's out of the border)
                if (sdf.x > 1.0 || sdf.y > 1.0) {
                    discard;
                } else {
                    // Get color from palette
                    vec4 color = readColor(u_color);
                
                    fragColor = vec4(color.rgb, 1.0);
                }
               
            }
            """.trimIndent()
    }
}
