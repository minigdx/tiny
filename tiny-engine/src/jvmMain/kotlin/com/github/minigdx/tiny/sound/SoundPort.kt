package com.github.minigdx.tiny.sound

import java.util.concurrent.BlockingQueue
import javax.sound.sampled.AudioFormat
import javax.sound.sampled.AudioSystem
import javax.sound.sampled.DataLine
import javax.sound.sampled.SourceDataLine

class SoundPort(var alive: Boolean = true, val queue: BlockingQueue<ByteArray>) : Thread("sound-port") {
    init {
        isDaemon = true
    }

    private lateinit var line: SourceDataLine

    fun shutdown() {
        alive = false
        interrupt()
        if (::line.isInitialized) {
            line.close()
        }
    }

    override fun run() {
        val format = AudioFormat(SAMPLE_RATE.toFloat(), BITS_PER_SAMPLE, CHANNELS, IS_SIGNED, IS_BIG_ENDIAN)
        val info = DataLine.Info(SourceDataLine::class.java, format)

        line = (AudioSystem.getLine(info) as SourceDataLine).apply {
            open(format)
            start()
        }

        try {
            while (alive) {
                val buffer = queue.take()
                line.write(buffer, 0, buffer.size)
            }
        } catch (_: InterruptedException) {
            // Shutdown requested
        } finally {
            if (::line.isInitialized) {
                line.close()
            }
        }
    }
}
