package com.github.minigdx.tiny.platform.glfw

import com.github.minigdx.tiny.Seconds
import com.github.minigdx.tiny.input.InputHandler
import com.github.minigdx.tiny.lua.SfxLib
import com.github.minigdx.tiny.sound.Sound
import com.github.minigdx.tiny.sound.SoundManager
import com.github.minigdx.tiny.sound.SoundManager.Companion.SAMPLE_RATE
import com.github.minigdx.tiny.sound.WaveGenerator
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
        //  audioStream.close()
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

class JavaMidiSoundManager : SoundManager {

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
                notesLine.write(nextBuffer, 0, nextBuffer.size)
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
        val duration = 60f / 120f / 4.0f
        val waves = SfxLib.convertScoreToWaves(score, duration)

        val buffer = generateScoreBuffer(waves)
        return SfxSound(buffer)
    }

    override fun playNotes(notes: List<WaveGenerator>, longestDuration: Seconds) {
        if (notes.isEmpty()) return

        val buffer = generateAudioBuffer(longestDuration, notes)

        bufferQueue.offer(buffer)
    }

    private fun generateAudioBuffer(
        longestDuration: Seconds,
        notes: List<WaveGenerator>,
    ): ByteArray {
        val numSamples: Int = (SAMPLE_RATE * longestDuration).toInt()
        val buffer = ByteArray(numSamples * 2)
        val fadeOutIndex = getFadeOutIndex(longestDuration)

        for (i in 0 until numSamples) {
            val sample = fadeOut(mix(i, notes), i, fadeOutIndex, numSamples)

            val sampleValue: Float = (sample * Short.MAX_VALUE)
            val clippedValue = sampleValue.coerceIn(Short.MIN_VALUE.toFloat(), Short.MAX_VALUE.toFloat())
            val result = clippedValue.toInt().toShort()

            buffer[2 * i] = (result and 0xFF).toByte()
            buffer[2 * i + 1] = (result.toInt().shr(8) and 0xFF).toByte()
        }
        return buffer
    }

    override fun playSfx(notes: List<WaveGenerator>) {
        if (notes.isEmpty()) return

        val sfxBuffer = generateScoreBuffer(notes)

        bufferQueue.offer(sfxBuffer)
    }

    private fun generateScoreBuffer(notes: List<WaveGenerator>): ByteArray {
        val numSamples: Int = (SAMPLE_RATE * notes.first().duration * notes.size).toInt()
        val sfxBuffer = ByteArray(numSamples * 2)
        var currentIndex = 0
        notes.forEach {
            val buffer = generateAudioBuffer(it.duration, listOf(it))
            buffer.copyInto(sfxBuffer, destinationOffset = currentIndex)
            currentIndex += buffer.size
        }
        return sfxBuffer
    }
}
