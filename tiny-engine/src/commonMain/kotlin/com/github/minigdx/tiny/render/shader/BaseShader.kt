package com.github.minigdx.tiny.render.shader

/**
 * Shader code unit.
 *
 * The shader will be generated at runtime by aggregating all shader fragment code.
 */
abstract class BaseShader(private val shader: String) {
    /**
     * All parameters used in the Shader
     * All the parameters will be declared in the header of the file.
     */
    val parameters: MutableList<ShaderParameter> = mutableListOf()

    val attributes: MutableList<ShaderParameter.Attribute> = mutableListOf()

    val samplers: MutableList<ShaderParameter.UniformSample2D> = mutableListOf()

    fun uniformInt(name: String): ShaderParameter.UniformInt {
        return ShaderParameter.UniformInt(name).also { parameters += it }
    }

    fun uniformFloat(name: String): ShaderParameter.UniformFloat {
        return ShaderParameter.UniformFloat(name).also { parameters += it }
    }

    fun uniformVec2(name: String): ShaderParameter.UniformVec2 {
        return ShaderParameter.UniformVec2(name).also { parameters += it }
    }

    fun attributeVec2(name: String): ShaderParameter.AttributeVec2 {
        return ShaderParameter.AttributeVec2(name).also {
            parameters += it
            attributes += it
        }
    }

    fun varyingVec2(name: String): ShaderParameter.VaryingVec2 {
        return ShaderParameter.VaryingVec2(name).also { parameters += it }
    }

    fun uniformSample2D(name: String, existingTexture: Boolean = false): ShaderParameter.UniformSample2D {
        return ShaderParameter.UniformSample2D(name, samplers.size, existingTexture).also {
            parameters += it
            samplers += it
        }
    }


    fun uniformFrameBufferSample2D(name: String): ShaderParameter.UniformSample2D {
        return ShaderParameter.UniformSample2D(name, samplers.size).also {
            parameters += it
            samplers += it
        }
    }

    override fun toString(): String {
        var tmpShader =
            """
            #ifdef GL_ES
            precision highp float;
            #endif
            """.trimIndent()

        val allParameters = parameters
        val uniforms: MutableList<ShaderParameter> = mutableListOf()
        val attributes: MutableList<ShaderParameter> = mutableListOf()
        val varyings: MutableList<ShaderParameter> = mutableListOf()

        allParameters.forEach {
            when (it) {
                is ShaderParameter.Uniform -> uniforms.add(it)
                is ShaderParameter.Attribute -> attributes.add(it)
                is ShaderParameter.Varying -> varyings.add(it)
                else -> throw IllegalArgumentException(
                    "Invalid type parameter! ${it::class.simpleName}. " +
                        "Expected to be Uniform or Attribute.",
                )
            }
        }

        tmpShader += "\n"
        tmpShader += "\n// --- uniforms ---\n"
        tmpShader += uniforms.joinToString("\n", postfix = "\n")
        tmpShader += "\n// --- attributes ---\n"
        tmpShader += attributes.joinToString("\n", postfix = "\n")
        tmpShader += "\n// --- varyings ---\n"
        tmpShader += varyings.joinToString("\n", postfix = "\n")
        tmpShader += "\n"
        tmpShader += shader

        return tmpShader
    }
}

abstract class VertexShader(shader: String) : BaseShader(shader)

abstract class FragmentShader(shader: String) : BaseShader(shader)
