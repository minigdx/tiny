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
        audioNode = soundManager.playChunkGenerator(chunkGenerator)
    }

    override fun loop() {
        audioNode = soundManager.playChunkGenerator(chunkGenerator, loop = true)
    }

    override fun stop() {
        audioNode?.stop()
        soundManager.removeSoundHandler(this)
    }

    override fun nextChunk(samples: Int): FloatData {
        return chunkGenerator.generateChunk(samples)
    }
}
