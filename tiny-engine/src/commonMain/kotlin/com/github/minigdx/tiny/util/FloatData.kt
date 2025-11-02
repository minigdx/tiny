package com.github.minigdx.tiny.util

import kotlin.math.min

class FloatData(maxSize: Int) {
    private val data = FloatArray(maxSize)

    var size = 0
        private set

    operator fun get(index: Int): Float {
        return data[index]
    }

    fun copyFrom(
        source: FloatArray,
        startIndex: Int,
        endIndex: Int,
    ) {
        val actualEndIndex = min(source.size, endIndex)
        source.copyInto(
            destination = data,
            destinationOffset = 0,
            startIndex = startIndex,
            endIndex = actualEndIndex,
        )
        size = actualEndIndex - startIndex
    }
}
