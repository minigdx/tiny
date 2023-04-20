package com.github.minigdx.tiny.file

interface TargetStream<T> {
    fun write(data: T)
}
