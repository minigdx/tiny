package com.github.minigdx.tiny.render.shader

import com.danielgergely.kgl.Kgl
import com.danielgergely.kgl.VertexArrayObject

internal actual class VaoManager actual constructor(private val gl: Kgl) {
    actual fun createVao(): VertexArrayObject? {
        // WebGL doesn't need VAO - return null
        return null
    }

    actual fun bindVao(vao: VertexArrayObject?) {
        // WebGL doesn't need VAO binding - do nothing
    }

    actual fun unbindVao() {
        // WebGL doesn't need VAO unbinding - do nothing
    }
}
