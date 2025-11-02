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

            vertexShader.aDither.apply(batch.dither)
            vertexShader.aShapeType.apply(batch.parametersType)
            vertexShader.aShapeColor.apply(batch.parametersColor)
            vertexShader.aShapeFilled.apply(batch.parametersFilled)

            vertexShader.aShapePosition.apply(batch.meshPosition)
            vertexShader.aShapeSize.apply(batch.meshSize)

            vertexShader.aShadeParams12.apply(batch.parameters12)
            vertexShader.aShadeParams34.apply(batch.parameters34)
            vertexShader.aShadeParams56.apply(batch.parameters56)
        }

        program.bind()
        program.drawArraysInstanced(GL_TRIANGLES, 0, 6, batch.parametersIndex)
        performanceMonitor.drawCall(6)
        program.unbind()
    }

    override fun endStage() = Unit

    class VShader : VertexShader(VERTEX_SHADER) {
        val aShapeType =
            inFloat("a_shapeType").forEachInstance() // Shape type (0=rect, 1=circle, 2=line, 3=rounded rect)
        val aShapeColor = inFloat("a_shapeColor").forEachInstance() // Shape color
        val aShapeFilled = inFloat("a_shapeFilled").forEachInstance() // Shape is filled?
        val aDither = inFloat("a_dither").forEachInstance() // Dithering pattern

        val aShapePosition = inVec3("a_shapePosition").forEachInstance() // Shape position on the screen ; in pixel
        val aShapeSize = inVec2("a_shapeSize").forEachInstance() // Shape size on the screen ; in pixel

        val aShadeParams12 = inVec2("a_shapeParams12").forEachInstance() // Parameters 1-2 (usually x, y or x1, y1)
        val aShadeParams34 =
            inVec2("a_shapeParams34").forEachInstance() // Parameters 3-4 (usually width, height or x2, y2)
        val aShadeParams56 =
            inVec2("a_shapeParams56").forEachInstance() // Parameters 5-6 (extra params like thickness, corner radius)

        val aPos = inVec2("a_pos") // Position of the shape with NDC

        val uViewport = uniformVec2("u_viewport") // Size of the viewport; in pixel.

        val vFragPos = outVec2("v_fragPos")

        val vDither = outFloat("v_dither", flat = true)
        val vShapePosition = outVec2("v_shapePosition", flat = true)
        val vShapeSize = outVec2("v_shapeSize", flat = true)

        val vShapeType = outFloat("v_shapeType", flat = true)
        val vShapeColor = outFloat("v_shapeColor", flat = true)
        val vShapeFilled = outFloat("v_shapeFilled", flat = true)

        val vShapeParams12 = outVec2("v_shapeParams12", flat = true)
        val vShapeParams34 = outVec2("v_shapeParams34", flat = true)
        val vShapeParams56 = outVec2("v_shapeParams56", flat = true)
    }

    class FShader : FragmentShader(FRAGMENT_SHADER) {
        val paletteColors = uniformSample2D("palette_colors")

        val vFragPos = inVec2("v_fragPos")

        val vDither = inFloat("v_dither", flat = true)

        val vShapePosition = inVec2("v_shapePosition", flat = true)
        val vShapeSize = inVec2("v_shapeSize", flat = true)

        val vShapeType = inFloat("v_shapeType", flat = true)
        val vShapeColor = inFloat("v_shapeColor", flat = true)
        val vShapeFilled = inFloat("v_shapeFilled", flat = true)

        val vShapeParams12 = inVec2("v_shapeParams12", flat = true)
        val vShapeParams34 = inVec2("v_shapeParams34", flat = true)
        val vShapeParams56 = inVec2("v_shapeParams56", flat = true)
    }

    companion object {
        //language=Glsl
        private val VERTEX_SHADER =
            """
            void main() {
                // Scale the rectangle (mutiply by the size) then translate (add the offset)
                vec2 vertex_pos =  ((a_pos * a_shapeSize) + vec2(a_shapePosition.x, a_shapePosition.y));
                // Convert the pixel coordinates into NDC coordinates
                vec2 ndc_pos = vertex_pos / u_viewport;
                // Move the origin to the left/up corner
                vec2 origin_pos = vec2(-1.0, 1.0) + ndc_pos * 2.0;
                
                gl_Position = vec4(origin_pos, a_shapePosition.z, 1.0);
                
                v_fragPos = vertex_pos;
                v_dither = a_dither;
                v_shapeType = a_shapeType;
                v_shapeSize = a_shapeSize;
                v_shapePosition = vec2(a_shapePosition.x, a_shapePosition.y);
                v_shapeColor = a_shapeColor;
                v_shapeFilled = a_shapeFilled;
                v_shapeParams12 = a_shapeParams12;
                v_shapeParams34 = a_shapeParams34;
                v_shapeParams56 = a_shapeParams56;
            }
            """.trimIndent()

        //language=Glsl
        private val FRAGMENT_SHADER =
            """
            #define T_RECT 0
            #define T_TRIANGLE 1
            #define T_CIRCLE 2
            #define T_LINE 3
            #define T_POINT 4
            
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
            
            float sdfRectangle(vec2 fragCoord, vec2 position, vec2 size) {
                // Position of the center of the rectangle
                vec2 center = position + size * 0.5;
                // Position of the frag regarding the center
                vec2 pos = abs(fragCoord - center);
                // Distance from the edge to the pos.
                vec2 d = pos - (size * 0.5);
            
                // > 1.0 is outside the rectangle.
                // < 1.0 is inside the rectangle.
                // 0 = is on the rectangle.
                return max(d.x, d.y);
            }
            
            float sdfLine(vec2 fragCoord, vec2 startLine, vec2 endLine) {
                vec2 p = fragCoord;
            
                // Position of the start of the line in pixel
                vec2 p0 = startLine;
                // Position of the end of the line in pixel
                vec2 p1 = endLine;
        
                // The frag is outsine the line on the horizontal side
                if (int(fragCoord.x) < int(startLine.x) || int(fragCoord.x) > int(endLine.x)) {
                    return 2.0;
                }
                
                // The frag is outside the line on the vertical side
                if(int(fragCoord.y) > int((max(startLine.y, endLine.y))) || int(fragCoord.y) < int((min(startLine.y, endLine.y)))) {
                    return 2.0;
                }           
                
                // Check for vertical or horizontal line
                if ((int(p.x) == int(p0.x) && int(p0.x) == int(p1.x)) || 
                     (int(p.y) == int(p0.y) && int(p0.y) == int(p1.y))
                ) {
                    return 0.0;
                }
                
                // Bresenham algorithm
                // See: https://en.wikipedia.org/wiki/Bresenham%27s_line_algorithm#Method
                vec2 d = p1 - p0;
            
                float slope;
                // the frag position from the start point (~ progress of the slope)
                float a;
                // other axis of the start line
                float b;
                // other axis for the frag
                float c;
                if (abs(d.x) > abs(d.y)) {
                    slope = (d.y / d.x);
                    a = p.x - p0.x;
                    b = p0.y;
                    c = p.y;
                } else {
                    slope = (d.x / d.y);
                    a = p.y - p0.y;
                    b = p0.x;
                    c = p.x;
                }
            
                float expected = slope * a + b;
            
                float sdf;
                // Is the current y match the expected y of the algo ?
                if (int(expected) == int(c)) {
                    // It's on the line
                    sdf = 0.0;
                } else {
                    // It's NOT on the line
                    sdf = 2.0;
                }
                return sdf;
            }
            
            float sdfPoint(vec2 frag, vec2 pos) {
                return 0.0;
            }
            
            float sdfCircle(vec2 frag, vec2 center, float radius) {
                vec2 p = frag - center;
            
                // Distance between the frag and the border of the circle
                float dist = length(p + vec2(0.5)) - radius;
            
                // Is on the line?
                if (abs(dist) < 0.5) {
                    return 0.0;
                }
            
                return dist * 4.0;
            }
            
            
            /**
            * Is the point p inside the triangle(a,b,c) ?
            * @return true if inside the triangle. false otherwise
            */
            bool pointInTriangle(vec2 p, vec2 a, vec2 b, vec2 c) {
                vec2 v0 = c - a;
                vec2 v1 = b - a;
                vec2 v2 = p - a;
            
                float dot00 = dot(v0, v0);
                float dot01 = dot(v0, v1);
                float dot02 = dot(v0, v2);
                float dot11 = dot(v1, v1);
                float dot12 = dot(v1, v2);
            
                float invDenom = 1.0 / (dot00 * dot11 - dot01 * dot01);
                float u = (dot11 * dot02 - dot01 * dot12) * invDenom;
                float v = (dot00 * dot12 - dot01 * dot02) * invDenom;
            
                return (u >= 0.0 && v >= 0.0 && u + v <= 1.0);
            }
            
            float sdfTriangle(vec2 frag, vec2 p0, vec2 p1, vec2 p2) {
            
                // BUG: maybe the line should not assume that it's only from left to right 
                float a = sdfLine(frag, p0, p1);
                float b = sdfLine(frag, p0, p2);
                float c = sdfLine(frag, p1, p2);
                // is the frag on a line?
                if (a + b + c < 6.0) { 
                    return 0.0;
                 }
                bool isInside = pointInTriangle(frag, p0, p1, p2);
                if (isInside) {
                    // is inside the triangle
                    return -2.0;
                } else {
                    // is outside the triangle
                    return 2.0;
                }
            }
            
            /**
            * Return true if the pixel x,y pass the pattern mask
            */
            bool dither(int pattern, int x, int y) {
                  int a = imod(x,  4);
                  int b = imod(y, 4) * 4;
                  int bitPosition = a + b;
                  
                  float powerOfTwo = pow(2.0, float(bitPosition));
                  int bit = int(floor(mod(float(pattern) / powerOfTwo, 2.0)));
                   
                  return bit > 0;
            }
            
            void main() {
                if(!dither(int(v_dither), int(v_fragPos.x), int(v_fragPos.y))) {
                    discard;
                }       
                
                float sdf;
                int type = int(v_shapeType);
                if (type == T_LINE) {
                    sdf = sdfLine(v_fragPos, v_shapeParams12, v_shapeParams34);
                } else if (type == T_POINT) {
                    sdf = sdfPoint(v_fragPos, v_shapePosition);
                } else if (type == T_CIRCLE) {
                    sdf = sdfCircle(v_fragPos, v_shapeParams12, v_shapeParams34.x);
                } else if (type == T_TRIANGLE) {
                    sdf = sdfTriangle(v_fragPos, v_shapeParams12, v_shapeParams34, v_shapeParams56);
                } else {
                    sdf = sdfRectangle(v_fragPos, v_shapePosition, v_shapeSize);
                }
            
                // The distance is more than one pixel away (ie: it's out of the border)
                if (sdf >= 1.0) {
                    discard;
                    // If the frag is inside the shape and the shape should NOT be filled.
                } else if (sdf <= -1.0 && v_shapeFilled < 1.0) {
                    discard;
                } else {
                    // If the frag is on the border OR inside the shape AND should be filled
                    // Get color from palette
                    vec4 color = readColor(int(v_shapeColor));
            
                    fragColor = vec4(color.rgb, 1.0);
                }
            
            }
            """.trimIndent()
    }
}
