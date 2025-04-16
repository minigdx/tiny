package com.github.minigdx.tiny.render.shader

import com.danielgergely.kgl.GL_COMPILE_STATUS
import com.danielgergely.kgl.GL_FALSE
import com.danielgergely.kgl.GL_FRAGMENT_SHADER
import com.danielgergely.kgl.GL_LINK_STATUS
import com.danielgergely.kgl.GL_VERTEX_SHADER
import com.danielgergely.kgl.Kgl
import com.danielgergely.kgl.Program
import com.danielgergely.kgl.Shader
import com.danielgergely.kgl.UniformLocation

class ShaderProgram<V : VertexShader, F : FragmentShader>(
    val gl: Kgl,
    val vertexShader: V,
    val fragmentShader: F,
) : Kgl by gl {
    private val attributes = mutableMapOf<String, Int>()

    private val uniforms = mutableMapOf<String, UniformLocation>()

    private var program: Program? = null
    private var vertexShaderId: Shader? = null
    private var fragmentShaderId: Shader? = null

    fun compileShader() {
        val vertexShaderId = createShader(vertexShader.toString(), GL_VERTEX_SHADER)
        val fragmentShaderId = createShader(fragmentShader.toString(), GL_FRAGMENT_SHADER)

        program = gl.createProgram() ?: throw IllegalStateException("Could not create OpenGL program")

        gl.attachShader(program!!, vertexShaderId)
        gl.attachShader(program!!, fragmentShaderId)

        gl.linkProgram(program!!)

        if (gl.getProgramParameter(program!!, GL_LINK_STATUS) == GL_FALSE) {
            val programInfoLog = gl.getProgramInfoLog(program!!)
            throw RuntimeException("Unable to link shader program: '$programInfoLog'")
        }

        gl.deleteShader(vertexShaderId)
        gl.deleteShader(fragmentShaderId)

        this.vertexShaderId = vertexShaderId
        this.fragmentShaderId = fragmentShaderId

        gl.useProgram(program!!)

        vertexShader.parameters.forEach { parameter ->
            parameter.create(this)
        }
        fragmentShader.parameters.forEach { parameter ->
            parameter.create(this)
        }
    }

    private fun createShader(
        shader: String,
        shaderType: Int,
    ): Shader {
        fun addLineNumbers(text: String): String {
            val lines = text.lines()
            val lineNumberWidth = lines.size.toString().length
            return lines.mapIndexed { index, line ->
                val lineNumber = (index + 1).toString().padStart(lineNumberWidth, ' ')
                "$lineNumber: $line"
            }.joinToString("\n")
        }

        val vertexShaderId = gl.createShader(shaderType)!!
        gl.shaderSource(vertexShaderId, shader)
        gl.compileShader(vertexShaderId)
        if (gl.getShaderParameter(vertexShaderId, GL_COMPILE_STATUS) == GL_FALSE) {
            val log = gl.getShaderInfoLog(vertexShaderId)
            gl.deleteShader(vertexShaderId)
            throw RuntimeException(
                "Shader compilation error: $log \n" +
                    "---------- \n" +
                    "Shader code in error: \n" +
                    addLineNumbers(shader),
            )
        }
        return vertexShaderId
    }

    fun createAttrib(name: String) {
        attributes[name] = gl.getAttribLocation(program!!, name)
    }

    fun createUniform(name: String) {
        uniforms[name] =
            gl.getUniformLocation(program!!, name) ?: throw IllegalArgumentException("Uniform $name not found")
    }

    fun getAttrib(name: String): Int = attributes[name] ?: throw IllegalStateException("Attributes '$name' not created!")

    fun getUniform(name: String): UniformLocation {
        return uniforms[name] ?: throw IllegalStateException("Uniform '$name' not created!")
    }

    fun use() {
        useProgram(program!!)
    }

    fun bind() {
        for (attribute in vertexShader.attributes) {
            attribute.bind()
        }

        for (sampler in fragmentShader.samplers) {
            sampler.bind()
        }
    }

    fun unbind() {
        for (sampler in fragmentShader.samplers) {
            sampler.unbind()
        }

        for (attribute in vertexShader.attributes) {
            attribute.unbind()
        }
    }
}
