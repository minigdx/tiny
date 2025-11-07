package com.github.minigdx.tiny.render.batch

import com.github.minigdx.tiny.graphic.Clipper

class PrimitiveKey(var clipper: Clipper? = null) : BatchKey {
    override fun reset() {
        clipper = null
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is SpriteBatchKey) return false
        return clipper == other.clipper
    }

    override fun hashCode(): Int {
        val result = clipper.hashCode()
        return result
    }

    fun set(clipper: Clipper): PrimitiveKey {
        this.clipper = clipper
        return this
    }
}
