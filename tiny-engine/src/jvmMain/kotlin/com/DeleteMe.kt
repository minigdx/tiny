import com.github.minigdx.tiny.lua.Note
import com.github.minigdx.tiny.sound.Envelope
import com.github.minigdx.tiny.sound.Sine2
import com.github.minigdx.tiny.sound.Sweep
import javax.sound.sampled.AudioFormat
import javax.sound.sampled.AudioSystem
import kotlin.experimental.and

fun main() {
    val sampleRate = 44100f // Fréquence d'échantillonnage en Hz
    val duration = 0.2 // Durée en secondes

    val numSamples = (sampleRate * duration).toInt()
    val audioBuffer = FloatArray(numSamples)

    val mod = Sweep(500)
    val env = Envelope(0.0f, 0.00f, 0.8f, 0.01f)

    var buffer = FloatArray(0)

    Note.values().forEachIndexed { i, note ->
        (0 until numSamples).forEach { index ->
            Sine2(
                note.frequency,
                modulation = mod,
                envelope = env,
            ).generate(index, audioBuffer)
        }

        buffer = buffer.copyOf(buffer.size + audioBuffer.size)
        audioBuffer.copyInto(buffer, i * audioBuffer.size, 0, audioBuffer.size)
    }

    // Jouer le son
    playSound(buffer, sampleRate)
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
