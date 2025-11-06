package com.github.minigdx.tiny.platform.webgl

import com.github.minigdx.tiny.sound.ChunkGenerator
import com.github.minigdx.tiny.sound.SoundHandler
import com.github.minigdx.tiny.util.FloatData
import web.audio.AudioBufferSourceNode

class WebSoundHandler(
    private val chunkGenerator: ChunkGenerator,
    private val soundManager: WebSoundManager,
) : SoundHandler {
    private var audioNode: AudioBufferSourceNode? = null

    override fun play() {
        // FIXME: replace
        // audioNode = soundMananger.playSfxBuffer(data)
    }

    override fun loop() {
        // FIXME: replace
        // audioNode = soundMananger.playSfxBuffer(data, loop = true)
    }

    override fun stop() {
        audioNode?.stop()
    }

    override fun nextChunk(samples: Int): FloatData {
        return chunkGenerator.generateChunk(samples)
    }
}
