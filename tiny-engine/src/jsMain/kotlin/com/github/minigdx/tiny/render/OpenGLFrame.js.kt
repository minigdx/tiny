package com.github.minigdx.tiny.render

import com.danielgergely.kgl.ByteBuffer

actual fun readBytes(
    buffer: ByteBuffer,
    out: ByteArray,
) {
    for (n in 0 until out.size) {
        out[n] = buffer.get()
        buffer.position++
    }
}
