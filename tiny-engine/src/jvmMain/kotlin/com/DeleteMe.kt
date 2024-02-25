import com.github.minigdx.tiny.lua.Note
import com.github.minigdx.tiny.sound.SineWave2
import com.github.minigdx.tiny.sound.Sweep
import com.github.minigdx.tiny.sound.Vibrato
import javax.sound.sampled.AudioFormat
import javax.sound.sampled.AudioSystem
import kotlin.experimental.and

fun main() {
    val sampleRate = 44100f // Fréquence d'échantillonnage en Hz
    val duration = 2.0 // Durée en secondes
    val frequency = 440.0f // Fréquence du son en Hz

    val numSamples = (sampleRate * duration).toInt()
    val audioBuffer = FloatArray(numSamples)

    val final = FloatArray(numSamples * 2)

    var chain = listOf(
        SineWave2(frequency, arrayOf(Sweep(100), Vibrato(10f, 1f))),
        // Envelope(0f, 1f, 0.5f, 0.1f)
    )

    // Générer l'onde carrée
    for (i in 0 until numSamples) {
        chain.forEach { effect ->
            effect.apply(i, audioBuffer)
        }
    }
    audioBuffer.copyInto(final, 0, 0, audioBuffer.size)

// Générer l'onde carrée
     chain = listOf(
        SineWave2(Note.C6.frequency, arrayOf(Sweep(100), Vibrato(10f, 1f))),
        // Envelope(0f, 1f, 0.5f, 0.1f)
    )
    for (i in 0 until numSamples) {
        chain.forEach { effect ->
            effect.apply(i, audioBuffer)
        }
    }
    audioBuffer.copyInto(final, audioBuffer.size, 0, audioBuffer.size)

    // Jouer le son
    playSound(final, sampleRate)
}


fun playSound(buffer: FloatArray, sampleRate: Float) {
    val format = AudioFormat(sampleRate, 16, 1, true, false)
    val line = AudioSystem.getSourceDataLine(format)

    line.open(format)
    line.start()

    val byteBuffer = ByteArray(buffer.size * 2)
    for (i in buffer.indices) {
        val sample = (buffer[i] * Short.MAX_VALUE).toInt().toShort()
        byteBuffer[i * 2] = (sample and 0xFF).toByte()
        byteBuffer[i * 2 + 1] = ((sample.toInt() ushr 8) and 0xFF).toByte()
    }

    line.write(byteBuffer, 0, byteBuffer.size)
    line.drain()
    line.close()
}
