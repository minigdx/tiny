package com.github.minigdx.tiny.platform.webgl

import com.github.minigdx.tiny.sound.SoundHandler
import org.khronos.webgl.Float32Array

class WebSoundHandler(
    val data: Float32Array,
    private val soundMananger: WebSoundMananger,
) : SoundHandler {
    private var audioNode: AudioBufferSourceNode? = null

    override fun play() {
        audioNode = soundMananger.playSfxBuffer(data)
    }

    override fun loop() {
        audioNode = soundMananger.playSfxBuffer(data, loop = true)
    }

    override fun stop() {
        audioNode?.stop()
    }
}
