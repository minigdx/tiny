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
import kotlin.experimental.and
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

    override fun playBuffer(
        buffer: FloatArray,
        numberOfSamples: Long,
    ) {
        mixer.add(buffer)
    }

    override fun destroy() {
        soundPort.alive = false
        mixer.add(FloatArray(0)) // unlock the sound port
        mixer.alive = false
    }

    fun exportToWavFile(
        filePath: String,
        floatAudioData: FloatArray,
    ): Boolean {
        println("Début de l'exportation vers : $filePath")
        println("Nombre d'échantillons flottants : ${floatAudioData.size}")
        val audioFormat = AudioFormat(SAMPLE_RATE.toFloat(), BITS_PER_SAMPLE, CHANNELS, IS_SIGNED, IS_BIG_ENDIAN)

        // 2. Convertir les données Float [-1.0, 1.0] en ByteArray PCM 16 bits
        val bytesPerSample = audioFormat.sampleSizeInBits / 8
        val byteAudioData = ByteArray(floatAudioData.size * bytesPerSample * audioFormat.channels)
        var byteIndex = 0

        for (floatSample in floatAudioData) {
            val clippedSample = max(-1.0f, min(1.0f, floatSample))
            val pcmValue = (clippedSample * Short.MAX_VALUE).roundToInt().toShort()

            // Conversion Little Endian (car IS_BIG_ENDIAN = false dans vos constantes)
            if (audioFormat.isBigEndian) {
                // Big Endian (Moins courant pour WAV standard)
                byteAudioData[byteIndex++] = ((pcmValue.toInt() shr 8) and 0xFF).toByte() // High byte
                byteAudioData[byteIndex++] = (pcmValue.toInt() and 0xFF).toByte() // Low byte
            } else {
                // Little Endian (Standard)
                byteAudioData[byteIndex++] = (pcmValue.toInt() and 0xFF).toByte() // Low byte
                byteAudioData[byteIndex++] = ((pcmValue.toInt() shr 8) and 0xFF).toByte() // High byte
            }
        }
        println("Conversion Float -> Byte terminée. Taille du buffer d'octets: ${byteAudioData.size}")

        // 3. Préparer le flux d'entrée audio à partir du tableau d'octets
        val inputStream = ByteArrayInputStream(byteAudioData)

        // Le nombre de "frames" est le nombre total d'octets divisé par la taille d'une frame
        // Pour mono 16 bits, frame size = 2 bytes. Pour stéréo 16 bits, frame size = 4 bytes.
        val frameSize = audioFormat.frameSize
        val numberOfFrames = (byteAudioData.size / frameSize).toLong()

        val audioInputStream = AudioInputStream(inputStream, audioFormat, numberOfFrames)

        // 4. Définir le type de fichier de sortie
        val fileType = AudioFileFormat.Type.WAVE

        // 5. Créer le fichier de sortie (et les répertoires parents si nécessaire)
        val outputFile = File(filePath)
        outputFile.parentFile?.mkdirs() // Crée les répertoires parents s'ils n'existent pas

        // 6. Écrire le fichier audio
        println("Écriture du fichier WAV...")
        AudioSystem.write(audioInputStream, fileType, outputFile)

        println("Exportation WAV réussie vers : ${outputFile.absolutePath}")
        return true
    }
}
