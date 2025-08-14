package com.github.minigdx.tiny.platform.performance

/**
 * Performance metrics data class
 */
data class PerformanceMetrics(
    // in milliseconds
    val frameTime: Double,
    val fps: Double,
    // in bytes
    val memoryUsed: Long,
    // total allocated in this frame
    val memoryAllocated: Long,
    // GC collections count (platform specific)
    val gcCount: Long = 0,
    // GPU rendering time if available
    val renderTime: Double = 0.0,
    // CPU processing time
    val cpuTime: Double = 0.0,
    // Number of draw calls
    val drawCalls: Long = 0,
    // Number of read pixels
    val readPixels: Long = 0,
    // Number of vertex
    val vertexCount: Long = 0,
    // Number of batch primitives draw
    val drawBatchPrimitives: Long = 0,
    // Number of batch sprites draw
    val drawBatchSprites: Long = 0,
    // Number of screen draw
    val drawOnScreen: Long = 0,
)
