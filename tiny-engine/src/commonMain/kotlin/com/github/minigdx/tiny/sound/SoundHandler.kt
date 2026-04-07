package com.github.minigdx.tiny.sound

import com.github.minigdx.tiny.util.FloatData
import kotlin.math.min

/**
 * Sound handler that allow you to control a sound.
 */
interface SoundHandler {
    /**
     * Start to play a sound. Stop it after the last note.
     */
    fun play()

    /**
     * Start to play a sound as a loop.
     */
    fun loop()

    /**
     * Stop the sound.
     */
    fun stop()

    /**
     * Check if the sound is currently playing.
     */
    fun isPlaying(): Boolean

    /**
     * Generate the next chunk.
     */
    fun nextChunk(samples: Int): FloatData

    /**
     * Returns the current playback progress as a value from 0.0 to 1.0.
     */
    fun progress(): Float = 0f
}

interface ChunkGenerator {
    fun generateChunk(samples: Int): FloatData

    /**
     * Reset the generator to the beginning of the buffer.
     */
    fun reset()

    /**
     * Returns the current playback progress as a value from 0.0 to 1.0.
     */
    fun progress(): Float = 0f
}

class BufferedChunkGenerator(
    private val data: FloatArray,
    private val loopStartPosition: Int = 0,
) : ChunkGenerator {
    private var position: Int = 0

    private val chunk = FloatData(data.size)

    override fun generateChunk(samples: Int): FloatData {
        chunk.copyFrom(data, position, position + samples)
        position = min(position + samples, data.size)
        return chunk
    }

    override fun reset() {
        position = if (loopStartPosition in 0..data.size) loopStartPosition else 0
    }

    override fun progress(): Float {
        if (data.isEmpty()) return 0f
        return position.toFloat() / data.size.toFloat()
    }
}
