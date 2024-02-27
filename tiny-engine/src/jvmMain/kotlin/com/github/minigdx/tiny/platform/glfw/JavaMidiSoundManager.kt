package com.github.minigdx.tiny.platform.glfw

import com.github.minigdx.tiny.input.InputHandler
import com.github.minigdx.tiny.lua.SfxLib
import com.github.minigdx.tiny.sound.Sound
import com.github.minigdx.tiny.sound.SoundManager
import com.github.minigdx.tiny.sound.SoundManager.Companion.SAMPLE_RATE
import java.io.ByteArrayInputStream
import java.util.concurrent.ArrayBlockingQueue
import java.util.concurrent.BlockingQueue
import javax.sound.midi.MidiSystem
import javax.sound.midi.Sequencer
import javax.sound.midi.Sequencer.LOOP_CONTINUOUSLY
import javax.sound.sampled.AudioFormat
import javax.sound.sampled.AudioInputStream
import javax.sound.sampled.AudioSystem
import javax.sound.sampled.Clip
import kotlin.experimental.and

class SfxSound(byteArray: ByteArray) : Sound {

    private val clip: Clip

    init {
        val audioFormat = AudioFormat(SAMPLE_RATE.toFloat(), 16, 1, true, false)
        val audioStream = AudioInputStream(ByteArrayInputStream(byteArray), audioFormat, byteArray.size.toLong())
        clip = AudioSystem.getClip()
        clip.open(audioStream)
    }
    override fun play() {
        stop()
        clip.start()
    }

    override fun loop() {
        clip.loop(LOOP_CONTINUOUSLY)
    }

    override fun stop() {
        clip.stop()
        clip.framePosition = 0
    }
}
class JavaMidiSound(private val data: ByteArray) : Sound {

    private var sequencer: Sequencer? = null

    private fun _play(loop: Int) {
        val seq: Sequencer = MidiSystem.getSequencer()

        seq.open()

        val sequence = MidiSystem.getSequence(ByteArrayInputStream(data))
        seq.sequence = sequence

        sequencer = seq

        seq.loopCount = loop
        seq.start()
    }

    override fun play() {
        _play(0)
    }

    override fun loop() {
        _play(LOOP_CONTINUOUSLY)
    }

    override fun stop() {
        sequencer?.run {
            if (isRunning) {
                stop()
            }
            if (isOpen) {
                close()
            }
        }
    }
}

class JavaMidiSoundManager : SoundManager() {

    // When closing the application, switch isActive to false to stop the background thread.
    private var isActive = true

    private val bufferQueue: BlockingQueue<ByteArray> = ArrayBlockingQueue(10)

    private val backgroundAudio = object : Thread() {
        override fun run() {
            val notesLine = AudioSystem.getSourceDataLine(
                AudioFormat(
                    AudioFormat.Encoding.PCM_SIGNED,
                    SAMPLE_RATE.toFloat(),
                    16,
                    1, // TODO: set 2 to get Stereo
                    2,
                    SAMPLE_RATE.toFloat(),
                    false,
                ),
            )

            notesLine.open()
            notesLine.start()

            while (isActive) {
                val nextBuffer = bufferQueue.take()
                if (isActive) {
                    notesLine.write(nextBuffer, 0, nextBuffer.size)
                }
            }
            notesLine.close()
        }
    }

    override fun initSoundManager(inputHandler: InputHandler) {
        backgroundAudio.start()
    }

    override suspend fun createMidiSound(data: ByteArray): Sound {
        return JavaMidiSound(data)
    }

    override suspend fun createSfxSound(bytes: ByteArray): Sound {
        val score = bytes.decodeToString()
        val waves = SfxLib.convertScoreToSong2(score)

        val (buf, length) = createBufferFromSong(waves)
        val buffer = convertBuffer(buf, length)
        return SfxSound(buffer)
    }

    override fun playBuffer(buffer: FloatArray, numberOfSamples: Long) {
        bufferQueue.offer(convertBuffer(buffer, numberOfSamples))
    }

    private fun convertBuffer(
        audioBuffer: FloatArray,
        length: Long,
    ): ByteArray {
        val buffer = ByteArray(length.toInt() * 2)
        (0 until length.toInt()).forEach { i ->
            val sample = audioBuffer[i]
            val sampleValue: Float = (sample * Short.MAX_VALUE)
            val clippedValue = sampleValue.coerceIn(Short.MIN_VALUE.toFloat(), Short.MAX_VALUE.toFloat())
            val result = clippedValue.toInt().toShort()
            buffer[2 * i] = (result and 0xFF).toByte()
            buffer[2 * i + 1] = (result.toInt().shr(8) and 0xFF).toByte()
        }
        return buffer
    }

    override fun destroy() {
        isActive = false
        bufferQueue.offer(ByteArray(0)) // unlock the thread to quit
    }
}
