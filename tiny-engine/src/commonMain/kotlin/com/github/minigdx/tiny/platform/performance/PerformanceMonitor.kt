package com.github.minigdx.tiny.platform.performance

/**
 * Performance monitoring interface for tracking frame times and memory allocation
 */
interface PerformanceMonitor {
    /**
     * Start monitoring a frame
     */
    fun frameStart()

    /**
     * End monitoring a frame and return metrics
     */
    fun frameEnd(): PerformanceMetrics

    /**
     * Monitoring draw calls with the number of vertex
     */
    fun drawCall(nbVertex: Int)

    /**
     * Monitoring read pixels calls
     */
    fun readPixels()

    /**
     * Monitoring draw on screen calls
     */
    fun drawOnScreen()

    /**
     * Start monitoring a specific operation
     */
    fun operationStart(name: String)

    /**
     * End monitoring a specific operation
     */
    fun operationEnd(name: String): Double

    /**
     * Get current memory usage in bytes
     */
    fun getCurrentMemoryUsage(): Long

    /**
     * Get total allocated memory since last check
     */
    fun getAllocatedMemorySinceLastCheck(): Long

    /**
     * Reset performance counters
     */
    fun reset()

    /**
     * Get average metrics over the last N frames
     */
    fun getAverageMetrics(frameCount: Int = 60): PerformanceMetrics?

    /**
     * Get the current epoc time in milliseconds
     */
    fun now(): Long

    /**
     * Check if monitoring is enabled
     */
    var isEnabled: Boolean
}
