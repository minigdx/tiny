package com.github.minigdx.tiny.sound

import com.github.minigdx.tiny.sound.SoundManager.Companion.SAMPLE_RATE
import com.github.minigdx.tiny.util.FloatData
import kotlin.math.min

/**
 * Sound handler that allow you to control a sound.
 */
interface SoundHandler {
    /**
     * Start to play a sound.
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
     * Generate the next chunk.
     */
    fun nextChunk(samples: Int): FloatData
}

interface ChunkGenerator {
    fun generateChunk(samples: Int): FloatData
}

class BufferedChunkGenerator(private val data: FloatArray) : ChunkGenerator {
    private var position: Int = 0

    private val chunk = FloatData(data.size)

    override fun generateChunk(samples: Int): FloatData {
        chunk.copyFrom(data, position, position + samples)
        position = min(position + samples, data.size)
        return chunk
    }
}

class SequencedChunkGenerator(data: Sequence<FloatArray>) : ChunkGenerator {
    private var position: Int = 0
    private var currentChunk: FloatArray = floatArrayOf()

    private val iterator = data.iterator()

    // Up to 4 seconds
    private val chunk = FloatData(SAMPLE_RATE * 4)

    override fun generateChunk(samples: Int): FloatData {
        if (iterator.hasNext()) {
            currentChunk = iterator.next()
            position = 0
        } else {
            if (position >= currentChunk.size) {
                currentChunk = floatArrayOf()
                position = 0
            }
        }

        chunk.copyFrom(currentChunk, position, position + samples)

        position += samples

        return chunk
    }
}
