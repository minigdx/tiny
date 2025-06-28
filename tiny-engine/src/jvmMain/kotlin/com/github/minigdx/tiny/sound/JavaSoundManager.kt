package com.github.minigdx.tiny.sound

import com.github.minigdx.tiny.input.InputHandler
import java.io.ByteArrayInputStream
import java.io.File
import java.util.concurrent.ArrayBlockingQueue
import java.util.concurrent.BlockingQueue
import javax.sound.sampled.AudioFileFormat
import javax.sound.sampled.AudioFormat
import javax.sound.sampled.AudioInputStream
import javax.sound.sampled.AudioSystem
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

class JavaSoundManager : SoundManager() {
    private val bufferQueue: BlockingQueue<ByteArray> = ArrayBlockingQueue(10)

    private val mixer = MixerGateway(queue = bufferQueue)
    private val soundPort = SoundPort(queue = bufferQueue)

    override fun initSoundManager(inputHandler: InputHandler) {
        mixer.start()
        soundPort.start()
    }

    override fun createSoundHandler(
        buffer: FloatArray,
        numberOfSamples: Long,
    ): SoundHandler {
        return JavaSoundHandler(
            data = buffer,
            mixerGateway = mixer,
        )
    }

    override fun destroy() {
        soundPort.alive = false
        mixer.add(JavaSoundHandler(FloatArray(0), mixer)) // unlock the sound port
        mixer.alive = false
    }

    override fun exportAsSound(sequence: MusicalSequence) {
        val data = convert(sequence)
        exportToWavFile("export.wav", data)
    }

    fun exportToWavFile(
        filePath: String,
        floatAudioData: FloatArray,
    ): Boolean {
        val audioFormat = AudioFormat(SAMPLE_RATE.toFloat(), BITS_PER_SAMPLE, CHANNELS, IS_SIGNED, IS_BIG_ENDIAN)

        val bytesPerSample = audioFormat.sampleSizeInBits / 8
        val byteAudioData = ByteArray(floatAudioData.size * bytesPerSample * audioFormat.channels)
        var byteIndex = 0

        for (floatSample in floatAudioData) {
            val clippedSample = max(-1.0f, min(1.0f, floatSample))
            val pcmValue = (clippedSample * Short.MAX_VALUE).roundToInt().toShort()

            if (audioFormat.isBigEndian) {
                byteAudioData[byteIndex++] = ((pcmValue.toInt() shr 8) and 0xFF).toByte() // High byte
                byteAudioData[byteIndex++] = (pcmValue.toInt() and 0xFF).toByte() // Low byte
            } else {
                byteAudioData[byteIndex++] = (pcmValue.toInt() and 0xFF).toByte() // Low byte
                byteAudioData[byteIndex++] = ((pcmValue.toInt() shr 8) and 0xFF).toByte() // High byte
            }
        }

        val inputStream = ByteArrayInputStream(byteAudioData)

        val frameSize = audioFormat.frameSize
        val numberOfFrames = (byteAudioData.size / frameSize).toLong()

        val audioInputStream = AudioInputStream(inputStream, audioFormat, numberOfFrames)

        val fileType = AudioFileFormat.Type.WAVE

        val outputFile = File(filePath)
        outputFile.parentFile?.mkdirs() // Crée les répertoires parents s'ils n'existent pas

        AudioSystem.write(audioInputStream, fileType, outputFile)
        return true
    }
}
