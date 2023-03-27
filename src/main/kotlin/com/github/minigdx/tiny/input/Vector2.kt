package com.github.minigdx.tiny.input

data class Vector2(var x: Float, var y: Float) {

    constructor(other: Vector2) : this(other.x, other.y)

    fun set(x: Float, y: Float) {
        this.x = x
        this.y = y
    }

    fun set(other: Vector2) {
        x = other.x
        y = other.y
    }
}
