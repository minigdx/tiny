package com.github.minigdx.tiny.render

import com.danielgergely.kgl.GL_COLOR_BUFFER_BIT
import com.danielgergely.kgl.GL_DEPTH_BUFFER_BIT
import com.danielgergely.kgl.GL_TRIANGLES
import com.danielgergely.kgl.Kgl
import com.github.minigdx.tiny.Pixel
import com.github.minigdx.tiny.engine.GameOptions
import com.github.minigdx.tiny.graphic.PixelFormat
import com.github.minigdx.tiny.log.Logger
import com.github.minigdx.tiny.platform.WindowManager
import com.github.minigdx.tiny.render.shader.FragmentShader
import com.github.minigdx.tiny.render.shader.ShaderProgram
import com.github.minigdx.tiny.render.shader.VertexShader

class OpenGLCPURenderUnit(gl: Kgl, logger: Logger, gameOptions: GameOptions) : RendererUnit<OpenGLCPURenderContext>(
    gl,
    logger,
    gameOptions,
) {
    private var colorPaletteBuffer = ByteArray(0)

    private val uvsData =
        floatArrayOf(
            // bottom right
            2f,
            1f,
            // top left
            0f,
            -1f,
            // bottom left
            0f,
            1f,
        )

    private val vertexData =
        floatArrayOf(
            // bottom right
            3f,
            -1f,
            // top left
            -1f,
            3f,
            // bottom left
            -1f,
            -1f,
        )

    override fun init(windowManager: WindowManager): OpenGLCPURenderContext {
        val program = ShaderProgram(gl, VShader(), FShader())
        program.compileShader()

        program.vertexShader.position.apply(vertexData)
        program.vertexShader.uvs.apply(uvsData)

        val colors = gameOptions.colors()
        // texture of one pixel height and 256 pixel width.
        // one pixel of the texture = one index.
        // OpenGL ES required a texture with squared size.
        // So it's a 256*256 texture, even if only the first
        // row of this texture is used.
        colorPaletteBuffer = ByteArray(256 * 256 * PixelFormat.RGBA)
        var pos = 0
        for (index in 0 until 256) {
            val color = colors.getRGBA(index)

            colorPaletteBuffer[pos++] = color[0]
            colorPaletteBuffer[pos++] = color[1]
            colorPaletteBuffer[pos++] = color[2]
            colorPaletteBuffer[pos++] = color[3]
        }

        program.fragmentShader.colorPalette.applyRGBA(colorPaletteBuffer, 256, 256)

        return OpenGLCPURenderContext(
            windowManager = windowManager,
            shaderProgram = program,
        )
    }

    override fun drawCPU(
        context: OpenGLCPURenderContext,
        image: ByteArray,
        width: Pixel,
        height: Pixel,
    ) {
        val program = context.shaderProgram
        program.use()

        program.fragmentShader.frameBuffer.applyIndex(image, width, height)

        program.bind()

        program.clear(GL_COLOR_BUFFER_BIT or GL_DEPTH_BUFFER_BIT) // FIXME: supprimer GL_DEPTH_BUFFER ?
        program.clearColor(0f, 0f, 0f, 1.0f)

        program.drawArrays(GL_TRIANGLES, 0, 3)

        program.unbind()
    }

    class VShader : VertexShader(VERTEX_SHADER) {
        val position = attributeVec2("position")
        val uvs = attributeVec2("uvs")
        val viewport = varyingVec2("viewport")
    }

    class FShader : FragmentShader(FRAGMENT_SHADER) {
        val viewport = varyingVec2("viewport")
        val colorPalette = uniformSample2D("color_palette")
        val frameBuffer = uniformSample2D("frame_buffer")
    }

    companion object {
        //language=Glsl
        val VERTEX_SHADER =
            """
            void main() {
                gl_Position = vec4(position, 0.0, 1.0);
                viewport = uvs;
            }
            """.trimIndent()

        //language=Glsl
        val FRAGMENT_SHADER =
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
                return readData(color_palette, icolor, 256, 256);
            }
            
            void main() {
                vec4 color = texture2D(frame_buffer, viewport);
                gl_FragColor = readColor(int((color * 255.0) + 0.5));
            }
            """.trimIndent()
    }
}
