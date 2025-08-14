package com.github.minigdx.tiny.platform.performance

/**
 * Common implementation with shared functionality
 */
abstract class BasePerformanceMonitor() : PerformanceMonitor {
    protected var frameStartTime = 0L
    protected var lastFrameTime = 0L
    protected var lastMemoryCheck = 0L
    protected var numberOfDrawCalls = 0L
    protected var numberOfVertex = 0L
    protected var numberOfDrawOnScreen = 0L
    protected var numberOfReadPixels = 0L

    // Rolling window for averaging
    private val metricsHistory = mutableListOf<PerformanceMetrics>()
    private val maxHistorySize = 300 // Keep last 5 seconds at 60 FPS

    // Operation timing
    private val operationStartTimes = mutableMapOf<String, Long>()

    override var isEnabled: Boolean = false
        set(value) {
            field = value
            if (value) {
                reset()
            }
        }

    override fun frameStart() {
        if (!isEnabled) return
        frameStartTime = now()
    }

    override fun frameEnd(): PerformanceMetrics {
        if (!isEnabled) return PerformanceMetrics(0.0, 0.0, 0L, 0L)

        val endTime = now()
        val frameTime = (endTime - frameStartTime).toDouble()
        val fps = if (frameTime > 0) 1000.0 / frameTime else 0.0

        val currentMemory = getCurrentMemoryUsage()
        val allocatedMemory = getAllocatedMemorySinceLastCheck()

        val metrics = createPlatformSpecificMetrics(
            frameTime = frameTime,
            fps = fps,
            memoryUsed = currentMemory,
            memoryAllocated = allocatedMemory,
            drawCalls = numberOfDrawCalls,
            vertexCount = numberOfVertex,
            readPixels = numberOfReadPixels,
            drawOnScreen = numberOfDrawOnScreen,
        )

        // Add to history
        metricsHistory.add(metrics)
        if (metricsHistory.size > maxHistorySize) {
            metricsHistory.removeAt(0)
        }

        lastFrameTime = endTime
        lastMemoryCheck = currentMemory

        numberOfDrawCalls = 0
        numberOfVertex = 0
        numberOfDrawOnScreen = 0
        numberOfReadPixels = 0

        return metrics
    }

    override fun operationStart(name: String) {
        if (!isEnabled) return
        operationStartTimes[name] = now()
    }

    override fun drawCall(nbVertex: Int) {
        if (!isEnabled) return
        numberOfDrawCalls++
        numberOfVertex += nbVertex
    }

    override fun readPixels() {
        if (!isEnabled) return
        numberOfReadPixels++
    }

    override fun drawOnScreen() {
        if (!isEnabled) return
        numberOfDrawOnScreen++
    }

    override fun operationEnd(name: String): Double {
        if (!isEnabled) return 0.0
        val startTime = operationStartTimes.remove(name) ?: return 0.0
        return (now() - startTime).toDouble()
    }

    override fun getAverageMetrics(frameCount: Int): PerformanceMetrics? {
        if (!isEnabled || metricsHistory.isEmpty()) return null

        val recentMetrics = metricsHistory.takeLast(frameCount.coerceAtMost(metricsHistory.size))
        if (recentMetrics.isEmpty()) return null

        return PerformanceMetrics(
            frameTime = recentMetrics.map { it.frameTime }.average(),
            fps = recentMetrics.map { it.fps }.average(),
            memoryUsed = recentMetrics.map { it.memoryUsed }.average().toLong(),
            memoryAllocated = recentMetrics.sumOf { it.memoryAllocated },
            gcCount = recentMetrics.sumOf { it.gcCount },
            renderTime = recentMetrics.map { it.renderTime }.average(),
            cpuTime = recentMetrics.map { it.cpuTime }.average(),
            drawCalls = recentMetrics.map { it.drawCalls }.average().toLong(),
            vertexCount = recentMetrics.map { it.vertexCount }.average().toLong(),
            readPixels = recentMetrics.map { it.readPixels }.average().toLong(),
            drawOnScreen = recentMetrics.map { it.drawOnScreen }.average().toLong(),
        )
    }

    override fun reset() {
        metricsHistory.clear()
        operationStartTimes.clear()
        lastMemoryCheck = getCurrentMemoryUsage()
    }

    /**
     * Platform-specific metrics creation
     */
    protected abstract fun createPlatformSpecificMetrics(
        frameTime: Double,
        fps: Double,
        memoryUsed: Long,
        memoryAllocated: Long,
        drawCalls: Long,
        vertexCount: Long,
        readPixels: Long,
        drawOnScreen: Long,
    ): PerformanceMetrics
}
