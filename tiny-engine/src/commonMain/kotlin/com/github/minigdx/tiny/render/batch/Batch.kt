package com.github.minigdx.tiny.render.batch

interface Batch<T : Instance> {
    fun canAddInto(): Boolean

    fun add(instance: T)

    fun reset()
}
