package com.github.minigdx.tiny.platform.webgl

import com.github.minigdx.tiny.platform.performance.BasePerformanceMonitor
import com.github.minigdx.tiny.platform.performance.PerformanceMetrics
import kotlinx.browser.window
import org.w3c.performance.Performance

/**
 * WebGL/Browser specific performance monitor using Web Performance APIs
 */
class WebGlPerformanceMonitor : BasePerformanceMonitor() {
    private val performance: Performance = window.performance

    private var lastMemoryCheckTime = 0.0
    private var memoryUsageBaseline = 0L
    private var supportsMemoryApi = false

    // WebGL timing extensions
    private var supportsTimerQuery = false

    init {
        // Check for memory API support
        supportsMemoryApi = checkMemoryApiSupport()

        // Check for WebGL timing extension support
        supportsTimerQuery = checkTimerQuerySupport()

        if (supportsMemoryApi) {
            memoryUsageBaseline = getCurrentMemoryUsage()
        }

        lastMemoryCheckTime = performance.now()
    }

    override fun getCurrentMemoryUsage(): Long {
        if (!isEnabled || !supportsMemoryApi) return 0L
        // Use the Memory API if available
        val memoryInfo = js("window.performance.memory")
        return memoryInfo?.usedJSHeapSize?.toLong() ?: 0L
    }

    override fun getAllocatedMemorySinceLastCheck(): Long {
        if (!isEnabled || !supportsMemoryApi) return 0L

        val current = getCurrentMemoryUsage()
        val allocated = (current - memoryUsageBaseline).coerceAtLeast(0L)
        memoryUsageBaseline = current
        return allocated
    }

    override fun now(): Long {
        return performance.now().toLong()
    }

    override fun createPlatformSpecificMetrics(
        frameTime: Double,
        fps: Double,
        memoryUsed: Long,
        memoryAllocated: Long,
        drawCalls: Long,
        vertexCount: Long,
        readPixels: Long,
        drawOnScreen: Long,
    ): PerformanceMetrics {
        // Use high-resolution timer for more accurate frame timing
        val now = performance.now()
        val actualFrameTime = if (lastMemoryCheckTime > 0) {
            now - lastMemoryCheckTime
        } else {
            frameTime
        }
        lastMemoryCheckTime = now

        // Get render time using User Timing API if available
        val renderTime = measureRenderTime()

        // CPU time estimation (limited in browsers)
        val cpuTime = estimateCpuTime(actualFrameTime, renderTime)

        return PerformanceMetrics(
            frameTime = actualFrameTime,
            fps = if (actualFrameTime > 0) 1000.0 / actualFrameTime else fps,
            memoryUsed = memoryUsed,
            memoryAllocated = memoryAllocated,
            // Not available in browsers
            gcCount = 0,
            renderTime = renderTime,
            cpuTime = cpuTime,
            drawOnScreen = drawOnScreen,
            readPixels = readPixels,
        )
    }

    private fun checkMemoryApiSupport(): Boolean {
        return try {
            js("typeof window.performance.memory !== 'undefined'") as Boolean
        } catch (_: Exception) {
            false
        }
    }

    private fun checkTimerQuerySupport(): Boolean {
        return try {
            // This would check for WebGL timer query extensions
            // EXT_disjoint_timer_query or similar
            false // Simplified for now
        } catch (e: Exception) {
            false
        }
    }

    private fun measureRenderTime(): Double {
        if (!supportsTimerQuery) return 0.0

        // This would use WebGL timer queries to measure GPU render time
        // Implementation would require WebGL context and proper extension setup
        return 0.0
    }

    private fun estimateCpuTime(
        frameTime: Double,
        renderTime: Double,
    ): Double {
        // Simple estimation: frame time minus render time
        return (frameTime - renderTime).coerceAtLeast(0.0)
    }
}
