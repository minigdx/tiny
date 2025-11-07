package com.github.minigdx.tiny.sound

import com.github.minigdx.tiny.util.FloatData

class JavaSoundHandler(
    val chunkGenerator: ChunkGenerator,
    private val mixerGateway: MixerGateway,
    private val soundManager: SoundManager,
) : SoundHandler {
    /**
     * Legacy constructor
     */
    constructor(data: FloatArray, mixerGateway: MixerGateway, soundManager: SoundManager) : this(
        BufferedChunkGenerator(data),
        mixerGateway,
        soundManager,
    )

    var loop: Boolean = false
    var stop: Boolean = false

    override fun play() {
        loop = false
        mixerGateway.add(this)
    }

    override fun loop() {
        loop = true
        mixerGateway.add(this)
    }

    override fun stop() {
        stop = true
        soundManager.removeSoundHandler(this)
    }

    override fun nextChunk(samples: Int): FloatData {
        val chunk = chunkGenerator.generateChunk(samples)
        if (chunk.size == 0) {
            if (!loop) {
                stop()
            }
        }
        return chunk
    }
}
