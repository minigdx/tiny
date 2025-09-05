package com.github.minigdx.tiny.render.gl

interface Stage {
    /**
     * Signal to start processing using this stage.
     */
    fun startStage() = Unit

    /**
     * Signal to end processing using this stage.
     */
    fun endStage() = Unit
}
