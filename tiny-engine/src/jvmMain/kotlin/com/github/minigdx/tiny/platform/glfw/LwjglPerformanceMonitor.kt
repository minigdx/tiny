package com.github.minigdx.tiny.platform.glfw

import com.github.minigdx.tiny.platform.performance.BasePerformanceMonitor
import com.github.minigdx.tiny.platform.performance.PerformanceMetrics
import java.lang.management.GarbageCollectorMXBean
import java.lang.management.ManagementFactory
import java.lang.management.MemoryMXBean

/**
 * LWJGL/Desktop specific performance monitor using JVM MX beans and GLFW timing
 */
class LwjglPerformanceMonitor : BasePerformanceMonitor() {
    private val memoryBean: MemoryMXBean = ManagementFactory.getMemoryMXBean()
    private val gcBeans: List<GarbageCollectorMXBean> = ManagementFactory.getGarbageCollectorMXBeans()

    private var lastGcCount = 0L
    private var lastAllocatedBytes = 0L

    init {
        // Initialize baseline values
        lastAllocatedBytes = getTotalAllocatedBytes()
        lastGcCount = getTotalGcCount()
    }

    override fun getCurrentMemoryUsage(): Long {
        return if (isEnabled) {
            memoryBean.heapMemoryUsage.used + memoryBean.nonHeapMemoryUsage.used
        } else {
            0L
        }
    }

    override fun getAllocatedMemorySinceLastCheck(): Long {
        if (!isEnabled) return 0L

        val currentAllocated = getTotalAllocatedBytes()
        val allocated = currentAllocated - lastAllocatedBytes
        lastAllocatedBytes = currentAllocated
        return allocated.coerceAtLeast(0L)
    }

    override fun now(): Long {
        return System.currentTimeMillis()
    }

    override fun createPlatformSpecificMetrics(
        frameTime: Double,
        fps: Double,
        memoryUsed: Long,
        memoryAllocated: Long,
    ): PerformanceMetrics {
        val currentGcCount = getTotalGcCount()
        val gcCountDelta = currentGcCount - lastGcCount
        lastGcCount = currentGcCount

        // Get CPU time for this thread
        val threadBean = ManagementFactory.getThreadMXBean()
        val cpuTime = if (threadBean.isCurrentThreadCpuTimeSupported) {
            threadBean.currentThreadCpuTime / 1_000_000.0 // Convert nanoseconds to milliseconds
        } else {
            0.0
        }

        // GPU timing would go here if supported
        val renderTime = 0.0

        return PerformanceMetrics(
            frameTime = frameTime,
            fps = fps,
            memoryUsed = memoryUsed,
            memoryAllocated = memoryAllocated,
            gcCount = gcCountDelta,
            renderTime = renderTime,
            cpuTime = cpuTime,
        )
    }

    private fun getTotalAllocatedBytes(): Long {
        return try {
            // Use JVM-specific allocation tracking if available
            val platformBean = ManagementFactory.getPlatformMXBean(com.sun.management.OperatingSystemMXBean::class.java)
            platformBean?.totalMemorySize ?: 0L
        } catch (e: Exception) {
            // Fallback to heap usage
            memoryBean.heapMemoryUsage.used
        }
    }

    private fun getTotalGcCount(): Long {
        return gcBeans.sumOf { it.collectionCount }
    }

    private fun measureGpuTime(): Double {
        // This would implement OpenGL timer queries
        // For now, return 0.0 as it requires proper GL context setup
        return 0.0
    }

    /**
     * Get detailed JVM memory information
     */
    fun getDetailedMemoryInfo(): JvmMemoryInfo {
        if (!isEnabled) return JvmMemoryInfo()

        val heapUsage = memoryBean.heapMemoryUsage
        val nonHeapUsage = memoryBean.nonHeapMemoryUsage

        return JvmMemoryInfo(
            heapUsed = heapUsage.used,
            heapMax = heapUsage.max,
            heapCommitted = heapUsage.committed,
            nonHeapUsed = nonHeapUsage.used,
            nonHeapMax = nonHeapUsage.max,
            nonHeapCommitted = nonHeapUsage.committed,
            gcCount = getTotalGcCount(),
        )
    }

    /**
     * Get system performance information
     */
    fun getSystemInfo(): SystemInfo {
        if (!isEnabled) return SystemInfo()

        return try {
            val osBean = ManagementFactory.getOperatingSystemMXBean()
            val runtimeBean = ManagementFactory.getRuntimeMXBean()

            SystemInfo(
                cpuCores = Runtime.getRuntime().availableProcessors(),
                systemLoad = osBean.systemLoadAverage,
                jvmUptime = runtimeBean.uptime,
                maxMemory = Runtime.getRuntime().maxMemory(),
                totalMemory = Runtime.getRuntime().totalMemory(),
                freeMemory = Runtime.getRuntime().freeMemory(),
            )
        } catch (e: Exception) {
            SystemInfo()
        }
    }
}

/**
 * Detailed JVM memory information
 */
data class JvmMemoryInfo(
    val heapUsed: Long = 0L,
    val heapMax: Long = 0L,
    val heapCommitted: Long = 0L,
    val nonHeapUsed: Long = 0L,
    val nonHeapMax: Long = 0L,
    val nonHeapCommitted: Long = 0L,
    val gcCount: Long = 0L,
)

/**
 * System performance information
 */
data class SystemInfo(
    val cpuCores: Int = 0,
    val systemLoad: Double = 0.0,
    val jvmUptime: Long = 0L,
    val maxMemory: Long = 0L,
    val totalMemory: Long = 0L,
    val freeMemory: Long = 0L,
)
