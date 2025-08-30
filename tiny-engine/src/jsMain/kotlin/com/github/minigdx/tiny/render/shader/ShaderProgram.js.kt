package com.github.minigdx.tiny.render.shader

import com.danielgergely.kgl.Kgl
import com.danielgergely.kgl.VertexArrayObject

internal actual fun Kgl.unbindVao() {
}

internal actual fun Kgl.bindVao(vao: VertexArrayObject?) {
}

internal actual fun Kgl.createVao(): VertexArrayObject? {
    return null
}
