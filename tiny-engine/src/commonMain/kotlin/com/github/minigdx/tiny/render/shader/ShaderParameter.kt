package com.github.minigdx.tiny.render.shader

import com.danielgergely.kgl.ByteBuffer
import com.danielgergely.kgl.FloatBuffer
import com.danielgergely.kgl.GL_ARRAY_BUFFER
import com.danielgergely.kgl.GL_DYNAMIC_DRAW
import com.danielgergely.kgl.GL_FLOAT
import com.danielgergely.kgl.GL_NEAREST
import com.danielgergely.kgl.GL_R8
import com.danielgergely.kgl.GL_RED
import com.danielgergely.kgl.GL_REPEAT
import com.danielgergely.kgl.GL_RGBA
import com.danielgergely.kgl.GL_TEXTURE0
import com.danielgergely.kgl.GL_TEXTURE_2D
import com.danielgergely.kgl.GL_TEXTURE_MAG_FILTER
import com.danielgergely.kgl.GL_TEXTURE_MIN_FILTER
import com.danielgergely.kgl.GL_TEXTURE_WRAP_S
import com.danielgergely.kgl.GL_TEXTURE_WRAP_T
import com.danielgergely.kgl.GL_UNSIGNED_BYTE
import com.danielgergely.kgl.GlBuffer
import com.danielgergely.kgl.Texture
import kotlin.jvm.JvmName

sealed class ShaderParameter(val name: String) {
    interface ArrayParameter {
        val size: Int
    }

    interface Uniform

    interface Attribute {
        fun bind(): Unit = TODO()

        fun unbind(): Unit = TODO()
    }

    interface Sampler {
        val index: Int

        fun bind()

        fun unbind()
    }

    interface Varying

    abstract fun create(program: ShaderProgram<*, *>)

    class UniformInt(name: String) : ShaderParameter(name), Uniform {
        override fun create(program: ShaderProgram<*, *>) {
            program.createUniform(name)
        }

        fun apply(
            program: ShaderProgram<*, *>,
            vararg value: Int,
        ) {
            when (value.size) {
                0 -> throw IllegalArgumentException("At least one int is expected")
                1 -> program.uniform1i(program.getUniform(name), value[0])
                2 -> program.uniform2i(program.getUniform(name), value[0], value[1])
                3 -> program.uniform3i(program.getUniform(name), value[0], value[1], value[2])
            }
        }

        override fun toString() = "uniform int $name;"
    }

    class UniformVec2(name: String) : ShaderParameter(name), Uniform {
        private lateinit var program: ShaderProgram<*, *>

        override fun create(program: ShaderProgram<*, *>) {
            program.createUniform(name)
            this.program = program
        }

        fun apply(vararg vec2: Float) {
            when (vec2.size) {
                2 -> program.uniform2f(program.getUniform(name), vec2[0], vec2[1])
                else -> throw IllegalArgumentException("2 values are expected. ${vec2.size} received")
            }
        }

        override fun toString() = "uniform vec2 $name;"
    }

    class UniformVec3(name: String) : ShaderParameter(name), Uniform {
        override fun create(program: ShaderProgram<*, *>) {
            program.createUniform(name)
        }

        fun apply(
            program: ShaderProgram<*, *>,
            vararg vec3: Float,
        ) {
            when (vec3.size) {
                3 -> program.uniform3f(program.getUniform(name), vec3[0], vec3[1], vec3[2])
                else -> throw IllegalArgumentException("3 values are expected. ${vec3.size} received")
            }
        }

        override fun toString() = "uniform vec3 $name;"
    }

    class UniformVec4(name: String) : ShaderParameter(name), Uniform {
        override fun create(program: ShaderProgram<*, *>) {
            program.createUniform(name)
        }

        fun apply(
            program: ShaderProgram<*, *>,
            vararg vec4: Float,
        ) {
            when (vec4.size) {
                4 -> program.uniform4f(program.getUniform(name), vec4[0], vec4[1], vec4[2], vec4[3])
                else -> throw IllegalArgumentException("4 values are expected. ${vec4.size} received")
            }
        }

        override fun toString() = "uniform vec4 $name;"
    }

    class UniformFloat(name: String) : ShaderParameter(name), Uniform {
        override fun create(program: ShaderProgram<*, *>) {
            program.createUniform(name)
        }

        fun apply(
            program: ShaderProgram<*, *>,
            vararg value: Float,
        ) {
            when (value.size) {
                0 -> throw IllegalArgumentException("At least one int is expected")
                1 -> program.uniform1f(program.getUniform(name), value[0])
                2 -> program.uniform2f(program.getUniform(name), value[0], value[1])
                3 -> program.uniform3f(program.getUniform(name), value[0], value[1], value[2])
                4 -> program.uniform4f(program.getUniform(name), value[0], value[1], value[2], value[3])
            }
        }

        override fun toString(): String = "uniform float $name;"
    }

    class UniformArrayFloat(name: String, override val size: Int) : ShaderParameter(name), ArrayParameter, Uniform {
        override fun create(program: ShaderProgram<*, *>) {
            program.createUniform(name)
        }

        fun apply(
            program: ShaderProgram<*, *>,
            f: FloatArray,
        ) {
            program.uniform1fv(program.getUniform(name), f)
        }

        fun apply(
            program: ShaderProgram<*, *>,
            f: List<Float>,
        ) = apply(program, f.toFloatArray())

        override fun toString() = "uniform float $name[$size];"
    }

    class UniformArrayVec2(name: String, override val size: Int) : ShaderParameter(name), ArrayParameter, Uniform {
        override fun create(program: ShaderProgram<*, *>) {
            program.createUniform(name)
        }

        fun apply(
            program: ShaderProgram<*, *>,
            f: FloatArray,
        ) {
            program.uniform2fv(program.getUniform(name), f)
        }

        fun apply(
            program: ShaderProgram<*, *>,
            f: List<Float>,
        ) = apply(program, f.toFloatArray())

        override fun toString() = "uniform vec2 $name[$size];"
    }

    class UniformArrayVec3(name: String, override val size: Int) : ShaderParameter(name), ArrayParameter, Uniform {
        override fun create(program: ShaderProgram<*, *>) {
            program.createUniform(name)
        }

        @JvmName("applyArray")
        fun apply(
            program: ShaderProgram<*, *>,
            f: FloatArray,
        ) {
            program.uniform3fv(program.getUniform(name), f)
        }

        fun apply(
            program: ShaderProgram<*, *>,
            f: List<Float>,
        ) = apply(program, f.toFloatArray())

        override fun toString() = "uniform vec3 $name[$size];"
    }

    class UniformArrayVec4(name: String, override val size: Int) : ShaderParameter(name), ArrayParameter, Uniform {
        override fun create(program: ShaderProgram<*, *>) {
            program.createUniform(name)
        }

        @JvmName("applyArray")
        fun apply(
            program: ShaderProgram<*, *>,
            f: FloatArray,
        ) {
            program.uniform4fv(program.getUniform(name), f)
        }

        fun apply(
            program: ShaderProgram<*, *>,
            f: List<Float>,
        ) = apply(program, f.toFloatArray())

        override fun toString() = "uniform vec4 $name[$size];"
    }

    class AttributeVec2(name: String) : ShaderParameter(name), Attribute {
        private var buffer: GlBuffer? = null

        private lateinit var program: ShaderProgram<*, *>

        override fun create(program: ShaderProgram<*, *>) {
            this.program = program
            program.createAttrib(name)
            buffer = program.createBuffer()
        }

        fun apply(
            data: FloatArray,
            stride: Int = 0,
        ) {
            program.bindBuffer(GL_ARRAY_BUFFER, buffer)
            program.bufferData(GL_ARRAY_BUFFER, FloatBuffer(data), data.size * GL_FLOAT, GL_DYNAMIC_DRAW)
            program.vertexAttribPointer(
                location = program.getAttrib(name),
                size = 2,
                type = GL_FLOAT,
                normalized = false,
                stride = stride,
                offset = 0,
            )
            program.enableVertexAttribArray(program.getAttrib(name))
            program.bindBuffer(GL_ARRAY_BUFFER, null)
        }

        override fun bind() {
            program.bindBuffer(GL_ARRAY_BUFFER, buffer)
            program.enableVertexAttribArray(program.getAttrib(name))
        }

        override fun unbind() {
            program.disableVertexAttribArray(program.getAttrib(name))
            program.bindBuffer(GL_ARRAY_BUFFER, null)
        }

        override fun toString() = "attribute vec2 $name;"
    }

    class AttributeVec3(name: String) : ShaderParameter(name), Attribute {
        override fun create(program: ShaderProgram<*, *>) {
            program.createAttrib(name)
        }

        fun apply(
            program: ShaderProgram<*, *>,
            source: GlBuffer,
        ) {
            program.bindBuffer(GL_ARRAY_BUFFER, source)
            program.vertexAttribPointer(
                location = program.getAttrib(name),
                size = 3,
                type = GL_FLOAT,
                normalized = false,
                stride = 0,
                offset = 0,
            )
            program.enableVertexAttribArray(program.getAttrib(name))
        }

        override fun toString() = "attribute vec3 $name;"
    }

    class AttributeVec4(name: String) : ShaderParameter(name), Attribute {
        override fun create(program: ShaderProgram<*, *>) {
            program.createAttrib(name)
        }

        fun apply(
            program: ShaderProgram<*, *>,
            source: GlBuffer,
        ) {
            program.bindBuffer(GL_ARRAY_BUFFER, source)
            program.vertexAttribPointer(
                location = program.getAttrib(name),
                size = 4,
                type = GL_FLOAT,
                normalized = false,
                stride = 0,
                offset = 0,
            )
            program.enableVertexAttribArray(program.getAttrib(name))
        }

        override fun toString() = "attribute vec3 $name;"
    }

    class UniformSample2D(name: String, override val index: Int) : ShaderParameter(name), Uniform, Sampler {
        private var texture: Texture? = null

        private lateinit var program: ShaderProgram<*, *>

        private var ready = false

        override fun create(program: ShaderProgram<*, *>) {
            program.createUniform(name)

            this.program = program
            texture = program.createTexture()
            program.bindTexture(GL_TEXTURE_2D, texture)
            program.texParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_S, GL_REPEAT)
            program.texParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_T, GL_REPEAT)

            program.texParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_NEAREST)
            program.texParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_NEAREST)
            program.bindTexture(GL_TEXTURE_2D, null)
        }

        fun applyRGBA(
            image: ByteArray,
            width: Int,
            height: Int,
        ) {
            program.bindTexture(GL_TEXTURE_2D, texture)

            program.texImage2D(
                GL_TEXTURE_2D,
                0,
                GL_RGBA,
                width,
                height,
                0,
                GL_RGBA,
                GL_UNSIGNED_BYTE,
                ByteBuffer(image),
            )

            program.bindTexture(GL_TEXTURE_2D, null)
            ready = true
        }

        fun applyBuffer(
            image: ByteBuffer,
            width: Int,
            height: Int,
        ) {
            program.bindTexture(GL_TEXTURE_2D, texture)

            program.texImage2D(
                GL_TEXTURE_2D,
                0,
                GL_RGBA,
                width,
                height,
                0,
                GL_RGBA,
                GL_UNSIGNED_BYTE,
                image,
            )

            program.bindTexture(GL_TEXTURE_2D, null)
            ready = true
        }

        fun applyIndex(
            image: ByteArray,
            width: Int,
            height: Int,
        ) {
            program.bindTexture(GL_TEXTURE_2D, texture)

            program.texImage2D(
                GL_TEXTURE_2D,
                0,
                GL_R8,
                width,
                height,
                0,
                GL_RED,
                GL_UNSIGNED_BYTE,
                ByteBuffer(image),
            )

            program.bindTexture(GL_TEXTURE_2D, null)
            ready = true
        }

        override fun bind() {
            if (!ready) {
                throw IllegalStateException(
                    "No texture as been configured for $name. " +
                        "Did you forget to set up a texture by calling apply method?",
                )
            }

            program.activeTexture(GL_TEXTURE0 + index)
            program.bindTexture(GL_TEXTURE_2D, texture)
            program.uniform1i(program.getUniform(name), index)
        }

        override fun unbind() {
            program.bindTexture(GL_TEXTURE_2D, null)
        }

        override fun toString() = "uniform sampler2D $name;"
    }

    class VaryingVec2(name: String) : ShaderParameter(name), Varying {
        override fun create(program: ShaderProgram<*, *>) = Unit

        override fun toString() = "varying vec2 $name;"
    }

    class VaryingVec3(name: String) : ShaderParameter(name), Varying {
        override fun create(program: ShaderProgram<*, *>) = Unit

        override fun toString() = "varying vec3 $name;"
    }

    class VaryingVec4(name: String) : ShaderParameter(name), Varying {
        override fun create(program: ShaderProgram<*, *>) = Unit

        override fun toString() = "varying vec4 $name;"
    }

    class VaryingFloat(name: String) : ShaderParameter(name), Varying {
        override fun create(program: ShaderProgram<*, *>) = Unit

        override fun toString() = "varying float $name;"
    }

    class VaryingInt(name: String) : ShaderParameter(name), Varying {
        override fun create(program: ShaderProgram<*, *>) = Unit

        override fun toString() = "varying int $name;"
    }
}
