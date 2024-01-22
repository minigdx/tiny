package com.github.minigdx.tiny.platform.glfw

import com.github.minigdx.tiny.Seconds
import com.github.minigdx.tiny.input.InputHandler
import com.github.minigdx.tiny.sound.MidiSound
import com.github.minigdx.tiny.sound.SoundManager
import com.github.minigdx.tiny.sound.SoundManager.Companion.SAMPLE_RATE
import com.github.minigdx.tiny.sound.WaveGenerator
import java.io.ByteArrayInputStream
import javax.sound.midi.MidiSystem
import javax.sound.midi.Sequencer
import javax.sound.midi.Sequencer.LOOP_CONTINUOUSLY
import javax.sound.sampled.AudioFormat
import javax.sound.sampled.AudioSystem
import javax.sound.sampled.SourceDataLine
import kotlin.experimental.and

class JavaMidiSound(private val data: ByteArray) : MidiSound {

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

    private lateinit var notesLine: SourceDataLine

    private var buffer = ByteArray(0)

    override fun initSoundManager(inputHandler: InputHandler) {
        notesLine = AudioSystem.getSourceDataLine(
            AudioFormat(
                AudioFormat.Encoding.PCM_SIGNED,
                SoundManager.SAMPLE_RATE.toFloat(),
                16,
                1, // TODO: set 2 to get Stereo
                2,
                SoundManager.SAMPLE_RATE.toFloat(),
                false,
            ),
        )

        notesLine.open()
        notesLine.start()
    }

    override suspend fun createSound(data: ByteArray): MidiSound {
        return JavaMidiSound(data)
    }

    override fun playNotes(notes: List<WaveGenerator>, longestDuration: Seconds) {
        if (notes.isEmpty()) return

        buffer = ByteArray((longestDuration * SAMPLE_RATE).toInt() * 2)
        val numSamples: Int = (SAMPLE_RATE * longestDuration).toInt()
        for (i in 0 until numSamples) {
            val sample = mix(i, notes)

            val sampleValue: Float = (sample * Short.MAX_VALUE)
            val clippedValue = sampleValue.coerceIn(Short.MIN_VALUE.toFloat(), Short.MAX_VALUE.toFloat())
            val result = clippedValue.toInt().toShort()

            buffer[2 * i] = (result and 0xFF).toByte()
            buffer[2 * i + 1] = (result.toInt().shr(8) and 0xFF).toByte()
        }

        // generate the byte array
        notesLine.write(buffer, 0, buffer.size)
    }
}
