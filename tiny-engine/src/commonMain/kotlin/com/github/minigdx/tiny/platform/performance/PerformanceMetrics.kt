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
)
