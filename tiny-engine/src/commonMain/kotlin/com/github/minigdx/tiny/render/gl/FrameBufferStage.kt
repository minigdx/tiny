package com.github.minigdx.tiny.render.gl

import com.danielgergely.kgl.Kgl
import com.github.minigdx.tiny.engine.GameOptions
import com.github.minigdx.tiny.platform.performance.PerformanceMonitor

class FrameBufferStage(
    gl: Kgl,
    performanceMonitor: PerformanceMonitor
) {

    private val shader = FramebufferShader(gl, performanceMonitor)

    fun init() {
        shader.init()
    }

    fun execute(stage: SpriteBatchStage) {
        shader.draw(stage.frameBuffer)
    }
}