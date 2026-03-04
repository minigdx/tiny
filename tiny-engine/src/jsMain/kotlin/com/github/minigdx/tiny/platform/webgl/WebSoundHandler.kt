package com.github.minigdx.tiny.platform.webgl

import com.github.minigdx.tiny.sound.ChunkGenerator
import com.github.minigdx.tiny.sound.SoundHandler
import com.github.minigdx.tiny.util.FloatData
import web.audio.AudioBufferSourceNode
import web.events.EventHandler

class WebSoundHandler(
    private val chunkGenerator: ChunkGenerator,
    private val soundManager: WebSoundManager,
) : SoundHandler {
    private var audioNode: AudioBufferSourceNode? = null
    private var playing = false

    override fun play() {
        audioNode = soundManager.playChunkGenerator(chunkGenerator)
        playing = true
        audioNode?.onended = EventHandler { playing = false }
    }

    override fun loop() {
        audioNode = soundManager.playChunkGenerator(chunkGenerator, loop = true)
        playing = true
    }

    override fun stop() {
        playing = false
        audioNode?.stop()
        soundManager.removeSoundHandler(this)
    }

    override fun isPlaying(): Boolean = playing

    override fun nextChunk(samples: Int): FloatData {
        return chunkGenerator.generateChunk(samples)
    }
}
