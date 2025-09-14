package com.github.minigdx.tiny.sound

import java.util.concurrent.BlockingQueue
import java.util.concurrent.ConcurrentLinkedQueue
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

/**
 * Audio mixer gateway that processes multiple sound sources into a single mixed output stream.
 * Runs in a separate thread to handle real-time audio mixing without blocking the main thread.
 *
 * @param alive Controls the mixer thread lifecycle
 * @param queue Output queue for processed audio chunks
 */
class MixerGateway(var alive: Boolean = true, val queue: BlockingQueue<ByteArray>) : Thread("mixer-gateway") {
    /** Buffer for mixing audio samples from all active sounds */
    val mixBuffer = FloatArray(CHUNK_SIZE)

    /**
     * Main mixing loop that runs in a separate thread.
     * Continuously processes all active sounds and outputs mixed audio chunks.
     */
    override fun run() {
        while (alive) {
            // Clear the mix buffer for the next chunk
            mixBuffer.fill(0f)

            // Skip processing if no sounds are playing
            if (sounds.isEmpty()) continue

            // Process each active sound
            sounds
                .filter { !it.stop }
                .forEach { sound ->
                    val chunk = sound.nextChunk(CHUNK_SIZE)
                    var mixIndex = 0
                    // Mix current sound chunk into the mix buffer
                    (0 until chunk.size).forEach { i ->
                        mixBuffer[mixIndex] = max(-1f, min(1f, (mixBuffer[mixIndex] + chunk[i])))
                        mixIndex++
                    }
                }

            // Remove finished or stopped sounds
            sounds.removeIf { it.stop }

            // Convert mixed audio to bytes and queue for output
            queue.put(playSoundFromFloats(mixBuffer))
            // Sleep for half the chunk duration to maintain smooth playback
            sleep(((CHUNK_DURATION.toFloat() * 0.5f) * 1000f).toLong())
        }
    }

    /** Thread-safe collection of active sound handlers */
    private val sounds = ConcurrentLinkedQueue<JavaSoundHandler>()

    /**
     * Add a sound handler to the mixer queue for processing.
     * @param sound The sound handler to add to the mixing pipeline
     */
    fun add(sound: JavaSoundHandler) {
        sounds.add(sound)
    }

    /**
     * Convert float audio samples to PCM byte array format for audio output.
     * Handles float-to-short conversion and Little Endian byte ordering.
     * @param floatAudioData Array of float samples in [-1.0, 1.0] range
     * @return PCM audio data as byte array ready for audio system
     */
    private fun playSoundFromFloats(floatAudioData: FloatArray): ByteArray {
        val byteAudioData = ByteArray(floatAudioData.size * BYTES_PER_SAMPLE * CHANNELS)
        var byteIndex = 0

        for (floatSample in floatAudioData) {
            // Clip the value to ensure it's within [-1.0, 1.0]
            val clippedSample = max(-1.0f, min(1.0f, floatSample))

            // Convert float (-1.0 to 1.0) to short (-32768 to 32767)
            // Note: The multiplication float * Short.MAX_VALUE returns a float.
            // Convert to Int first for better range handling before converting to Short.
            val pcmValue = (clippedSample * Short.MAX_VALUE).roundToInt().toShort()

            // Convert short to two bytes (Little Endian since IS_BIG_ENDIAN = false)
            // Kotlin/JVM uses the underlying platform's byte order by default,
            // but it's good to be explicit for audio.
            // The order here is correct for Little Endian.
            byteAudioData[byteIndex++] = (pcmValue.toInt() and 0xFF).toByte() // Low byte
            byteAudioData[byteIndex++] = ((pcmValue.toInt() shr 8) and 0xFF).toByte() // High byte
        }

        // Convert processed byte array to audio output
        // Updated display to indicate the number of samples and bytes
        return byteAudioData
    }
}
