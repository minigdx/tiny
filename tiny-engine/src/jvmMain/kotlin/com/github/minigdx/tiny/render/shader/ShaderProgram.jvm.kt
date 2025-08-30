package com.github.minigdx.tiny.render.shader

import com.danielgergely.kgl.Kgl
import com.danielgergely.kgl.VertexArrayObject

internal actual fun Kgl.unbindVao() {
    bindVertexArray(null)
}

internal actual fun Kgl.bindVao(vao: VertexArrayObject?) {
    if (vao != null) {
        bindVertexArray(vao)
    }
}

internal actual fun Kgl.createVao(): VertexArrayObject? {
    return createVertexArray()
}
