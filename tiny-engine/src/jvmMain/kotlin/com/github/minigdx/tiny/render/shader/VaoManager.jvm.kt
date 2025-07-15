package com.github.minigdx.tiny.render.shader

import com.danielgergely.kgl.Kgl
import com.danielgergely.kgl.VertexArrayObject

internal actual class VaoManager actual constructor(private val gl: Kgl) {
    actual fun createVao(): VertexArrayObject? {
        val vao = gl.createVertexArray()
        return vao
    }

    actual fun bindVao(vao: VertexArrayObject?) {
        if (vao != null) {
            gl.bindVertexArray(vao)
        }
    }

    actual fun unbindVao() {
        gl.bindVertexArray(null)
    }
}
