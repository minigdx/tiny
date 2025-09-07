package com.github.minigdx.tiny.render.shader

internal expect fun headerProlog(): String

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

    val inParameters: MutableList<ShaderParameter.In> = mutableListOf()

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

    abstract fun inVec2(
        name: String,
        flat: Boolean = false,
    ): ShaderParameter.InVec2

    abstract fun inVec4(
        name: String,
        flat: Boolean = false,
    ): ShaderParameter.InVec4

    abstract fun inFloat(
        name: String,
        flat: Boolean = false,
    ): ShaderParameter.InFloat

    fun outVec2(
        name: String,
        flat: Boolean = false,
    ): ShaderParameter.OutVec2 {
        return ShaderParameter.OutVec2(name, flat).also { parameters += it }
    }

    fun outVec3(
        name: String,
        flat: Boolean = false,
    ): ShaderParameter.OutVec3 {
        return ShaderParameter.OutVec3(name, flat).also { parameters += it }
    }

    fun outVec4(
        name: String,
        flat: Boolean = false,
    ): ShaderParameter.OutVec4 {
        return ShaderParameter.OutVec4(name, flat).also { parameters += it }
    }

    fun outFloat(
        name: String,
        flat: Boolean = false,
    ): ShaderParameter.OutFloat {
        return ShaderParameter.OutFloat(name, flat).also { parameters += it }
    }

    fun uniformSample2D(
        name: String,
        existingTexture: Boolean = false,
    ): ShaderParameter.UniformSample2D {
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
            ${headerProlog()}
            
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
                is ShaderParameter.In -> attributes.add(it)
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

abstract class VertexShader(shader: String) : BaseShader(shader) {
    override fun inVec2(
        name: String,
        flat: Boolean,
    ): ShaderParameter.InVec2 {
        return ShaderParameter.InVec2(name, flat).also {
            parameters += it
            inParameters += it
        }
    }

    override fun inVec4(
        name: String,
        flat: Boolean,
    ): ShaderParameter.InVec4 {
        return ShaderParameter.InVec4(name, flat).also {
            parameters += it
            inParameters += it
        }
    }

    override fun inFloat(
        name: String,
        flat: Boolean,
    ): ShaderParameter.InFloat {
        return ShaderParameter.InFloat(name, flat).also {
            parameters += it
            inParameters += it
        }
    }
}

abstract class FragmentShader(shader: String) : BaseShader(shader) {
    val fragColor = outVec4("fragColor")

    class NameOnlyInVec2(name: String, flat: Boolean = false) : ShaderParameter.InVec2(name, flat) {
        override fun create(program: ShaderProgram<*, *>) = Unit

        override fun bind() = Unit

        override fun unbind() = Unit
    }

    class NameOnlyInVec4(name: String, flat: Boolean = false) : ShaderParameter.InVec4(name, flat) {
        override fun create(program: ShaderProgram<*, *>) = Unit

        override fun bind() = Unit

        override fun unbind() = Unit
    }

    class NameOnlyInFloat(name: String, flat: Boolean = false) : ShaderParameter.InFloat(name, flat) {
        override fun create(program: ShaderProgram<*, *>) = Unit

        override fun bind() = Unit

        override fun unbind() = Unit
    }

    override fun inVec2(
        name: String,
        flat: Boolean,
    ): ShaderParameter.InVec2 {
        return NameOnlyInVec2(name, flat).also {
            parameters += it
            inParameters += it
        }
    }

    override fun inVec4(
        name: String,
        flat: Boolean,
    ): ShaderParameter.InVec4 {
        return NameOnlyInVec4(name, flat).also {
            parameters += it
            inParameters += it
        }
    }

    override fun inFloat(
        name: String,
        flat: Boolean,
    ): ShaderParameter.InFloat {
        return NameOnlyInFloat(name, flat).also {
            parameters += it
            inParameters += it
        }
    }
}
