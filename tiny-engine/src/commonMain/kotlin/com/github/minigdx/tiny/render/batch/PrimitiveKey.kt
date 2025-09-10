package com.github.minigdx.tiny.render.batch

import com.github.minigdx.tiny.ColorIndex

data class PrimitiveKey(
    var color: ColorIndex = 0,
) : BatchKey {
    fun set(color: ColorIndex): PrimitiveKey {
        this.color = color
        return this
    }

    override fun reset() {
        this.color = 0
    }
}
