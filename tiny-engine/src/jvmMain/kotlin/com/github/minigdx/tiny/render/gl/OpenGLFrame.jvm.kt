package com.github.minigdx.tiny.render.gl

import com.danielgergely.kgl.ByteBuffer

actual fun readBytes(
    buffer: ByteBuffer,
    out: ByteArray,
) {
    buffer.get(out)
}
