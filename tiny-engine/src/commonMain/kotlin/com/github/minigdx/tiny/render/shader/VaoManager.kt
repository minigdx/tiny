package com.github.minigdx.tiny.render.shader

import com.danielgergely.kgl.Kgl
import com.danielgergely.kgl.VertexArrayObject

internal expect class VaoManager(gl: Kgl) {
    fun createVao(): VertexArrayObject?

    fun bindVao(vao: VertexArrayObject?)

    fun unbindVao()
}
