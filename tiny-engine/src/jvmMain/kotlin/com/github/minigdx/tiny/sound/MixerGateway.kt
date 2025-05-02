package com.github.minigdx.tiny.sound

import java.util.concurrent.BlockingQueue
import java.util.concurrent.ConcurrentLinkedQueue
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

class MixerGateway(var alive: Boolean = true, val queue: BlockingQueue<ByteArray>) : Thread() {
    val mixBuffer = FloatArray(CHUNK_SIZE)

    override fun run() {
        while (alive) {
            mixBuffer.fill(0f)

            if (sounds.isEmpty()) continue

            sounds
                .filter { !it.stop }
                .forEach { sound ->
                    val lastIndex = min(sound.position + CHUNK_SIZE, sound.data.size)
                    var mixIndex = 0
                    (sound.position until lastIndex).forEach { i ->
                        mixBuffer[mixIndex] = max(-1f, min(1f, (mixBuffer[mixIndex] + sound.data[i])))
                        mixIndex++
                    }

                    sound.position += CHUNK_SIZE
                    if (sound.loop && sound.position >= sound.data.size) {
                        sound.position = sound.position - sound.data.size
                        // Copy the beginning of the sound in the remaining space of the chunk
                        (0 until sound.position).forEach { i ->
                            mixBuffer[mixIndex] = max(-1f, min(1f, (mixBuffer[mixIndex] + sound.data[i])))
                            mixIndex++
                        }
                    }
                }

            sounds.removeIf { it.position >= it.data.size || it.stop }

            queue.put(playSoundFromFloats(mixBuffer))
            sleep(((CHUNK_DURATION.toFloat() * 0.5f) * 1000f).toLong())
        }
    }

    private val sounds = ConcurrentLinkedQueue<JavaSoundHandler>()

    fun add(sound: JavaSoundHandler) {
        sounds.add(sound)
    }

    private fun playSoundFromFloats(floatAudioData: FloatArray): ByteArray {
        val byteAudioData = ByteArray(floatAudioData.size * BYTES_PER_SAMPLE * CHANNELS)
        var byteIndex = 0

        for (floatSample in floatAudioData) {
            // Clipper la valeur pour s'assurer qu'elle est dans [-1.0, 1.0]
            val clippedSample = max(-1.0f, min(1.0f, floatSample))

            // Convertir le float (-1.0 à 1.0) en short (-32768 à 32767)
            // Note: La multiplication float * Short.MAX_VALUE donne un float.
            // Convertir en Int d'abord pour une meilleure gestion de la plage avant de convertir en Short.
            val pcmValue = (clippedSample * Short.MAX_VALUE).roundToInt().toShort()

            // Convertir le short en deux bytes (Little Endian car IS_BIG_ENDIAN = false)
            // Kotlin/JVM utilise l'ordre des octets de la plateforme sous-jacente par défaut,
            // mais il est bon d'être explicite pour l'audio.
            // L'ordre ici est correct pour Little Endian.
            byteAudioData[byteIndex++] = (pcmValue.toInt() and 0xFF).toByte() // Low byte
            byteAudioData[byteIndex++] = ((pcmValue.toInt() shr 8) and 0xFF).toByte() // High byte
        }

        // 2. Lire le tableau de bytes
        // Affichage mis à jour pour indiquer le nombre de samples et de bytes
        return byteAudioData
    }
}
