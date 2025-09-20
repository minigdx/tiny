package com.github.minigdx.tiny.sound

import com.github.minigdx.tiny.input.InputHandler
import com.github.minigdx.tiny.input.internal.ObjectPool
import com.github.minigdx.tiny.lua.Note
import com.github.minigdx.tiny.sound.SoundManager.Companion.MASTER_VOLUME
import com.github.minigdx.tiny.util.MutableFixedSizeList
import java.io.ByteArrayInputStream
import java.io.File
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.concurrent.ArrayBlockingQueue
import java.util.concurrent.BlockingQueue
import java.util.concurrent.ConcurrentLinkedQueue
import javax.sound.sampled.AudioFileFormat
import javax.sound.sampled.AudioFormat
import javax.sound.sampled.AudioInputStream
import javax.sound.sampled.AudioSystem
import javax.sound.sampled.DataLine
import javax.sound.sampled.SourceDataLine
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

enum class NoteEventType {
    NOTE_ON,
    NOTE_OFF,
}

data class NoteEvent(
    var type: NoteEventType = NoteEventType.NOTE_OFF,
    var note: Note? = null,
    var instrumentPlayer: InstrumentPlayer? = null,
) {
    fun reset() {
        type = NoteEventType.NOTE_OFF
        note = null
        instrumentPlayer = null
    }
}

class NoteEventPool : ObjectPool<NoteEvent>(1000) {
    override fun newInstance(): NoteEvent {
        return NoteEvent()
    }

    override fun destroyInstance(obj: NoteEvent) {
        obj.reset()
    }
}

class StreamingAudioThread(
    private val queue: ConcurrentLinkedQueue<NoteEvent>,
    private val noteEventPool: NoteEventPool,
) : Thread("tiny-streaming-audio-thread") {
    init {
        priority = MAX_PRIORITY
    }

    private lateinit var line: SourceDataLine

    @Volatile
    private var running = true

    private val instrumentPlayers = MutableFixedSizeList<InstrumentPlayer>(MAX_INSTRUMENTS)

    private val byteBuffer = ByteArray(BUFFER_SIZE * 2)
    private val floatData = FloatArray(BUFFER_SIZE)

    override fun run() {
        val format = AudioFormat(
            SAMPLE_RATE.toFloat(),
            BITS_PER_SAMPLE,
            CHANNELS,
            IS_SIGNED,
            IS_BIG_ENDIAN,
        )
        val info = DataLine.Info(SourceDataLine::class.java, format)

        if (!AudioSystem.isLineSupported(info)) {
            throw IllegalStateException("Audio line is not supported. Not sound will be played for this game!")
        }

        line = (AudioSystem.getLine(info) as SourceDataLine).apply {
            open(format, BUFFER_SIZE * 2)
            start()
        }

        while (running) {
            processEvents()
            generateSamples(floatData)

            line.write(byteBuffer, 0, byteBuffer.size)
        }
    }

    private fun generateSamples(floatData: FloatArray) {
        val buffer = ByteBuffer.wrap(byteBuffer).order(ByteOrder.LITTLE_ENDIAN)
        (0 until BUFFER_SIZE).forEach { sample ->
            floatData[sample] = 0f
            instrumentPlayers.forEach { instrumentPlayer ->
                floatData[sample] += instrumentPlayer.generate()
            }
            floatData[sample] = (floatData[sample] * MASTER_VOLUME).coerceIn(-1f, 1f)

            val sampleValue = (floatData[sample] * 32767f).toInt().coerceIn(-32768, 32767)

            buffer.putShort(sampleValue.toShort())
        }
    }

    private fun processEvents() {
        var event = queue.poll()
        var count = 0
        while (event != null && count < MAX_EVENTS) {
            when (event.type) {
                NoteEventType.NOTE_ON -> {
                    event.instrumentPlayer?.noteOn(event.note!!)
                    instrumentPlayers.add(event.instrumentPlayer!!)
                }

                NoteEventType.NOTE_OFF -> {
                    event.instrumentPlayer?.noteOff(event.note!!)
                    // Don't remove immediately - let the release phase play out
                }
            }
            noteEventPool.free(event)
            count++
            event = queue.poll()
        }
    }

    companion object {
        private const val MAX_EVENTS = 16
        private const val MAX_INSTRUMENTS = 8
        private const val BUFFER_SIZE = 512
    }
}

class JavaSoundManager : SoundManager() {
    private val bufferQueue: BlockingQueue<ByteArray> = ArrayBlockingQueue(10)

    private val mixer = MixerGateway(queue = bufferQueue)
    private val soundPort = SoundPort(queue = bufferQueue)

    private val concurrentLinkedQueue = ConcurrentLinkedQueue<NoteEvent>()
    private val noteEventPool = NoteEventPool()

    // Store active instrument players by note
    private val activeInstrumentPlayers = mutableMapOf<Note, InstrumentPlayer>()

    private val streamingAudioThread = StreamingAudioThread(
        concurrentLinkedQueue,
        noteEventPool,
    )

    override fun initSoundManager(inputHandler: InputHandler) {
        mixer.start()
        soundPort.start()
        streamingAudioThread.start()
    }

    override fun noteOn(
        note: Note,
        instrument: Instrument,
    ) {
        val instrumentPlayer = InstrumentPlayer(instrument)
        activeInstrumentPlayers[note] = instrumentPlayer

        val event = noteEventPool.obtain()
        event.type = NoteEventType.NOTE_ON
        event.note = note
        event.instrumentPlayer = instrumentPlayer
        concurrentLinkedQueue.add(event)
    }

    override fun noteOff(note: Note) {
        val instrumentPlayer = activeInstrumentPlayers[note]
        if (instrumentPlayer != null) {
            val event = noteEventPool.obtain()
            event.type = NoteEventType.NOTE_OFF
            event.note = note
            event.instrumentPlayer = instrumentPlayer
            concurrentLinkedQueue.add(event)
            activeInstrumentPlayers.remove(note)
        }
    }

    override fun createSoundHandler(buffer: FloatArray): SoundHandler {
        return JavaSoundHandler(
            data = buffer,
            mixerGateway = mixer,
        )
    }

    override fun createSoundHandler(buffer: Sequence<FloatArray>): SoundHandler {
        return JavaSoundHandler(SequencedChunkGenerator(buffer), mixerGateway = mixer)
    }

    override fun createSoundHandler(chunkGenerator: ChunkGenerator): SoundHandler {
        return JavaSoundHandler(chunkGenerator, mixerGateway = mixer)
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
