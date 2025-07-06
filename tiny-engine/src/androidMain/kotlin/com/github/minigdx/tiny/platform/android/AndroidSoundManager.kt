package com.github.minigdx.tiny.platform.android

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import com.github.minigdx.tiny.Percent
import com.github.minigdx.tiny.engine.GameOptions
import com.github.minigdx.tiny.log.Logger
import com.github.minigdx.tiny.sound.SoundHandler
import com.github.minigdx.tiny.sound.SoundManager
import kotlin.math.max

class AndroidSoundManager(
    private val context: Context,
    private val gameOptions: GameOptions,
    private val logger: Logger,
) : SoundManager() {
    companion object {
        const val SAMPLE_RATE = 44100
        const val CHANNELS = AudioFormat.CHANNEL_OUT_STEREO
        const val ENCODING = AudioFormat.ENCODING_PCM_FLOAT
        const val BUFFER_SIZE_FRAMES = 512
    }

    private var audioTrack: AudioTrack? = null
    private val soundHandler = AndroidSoundHandler()
    private var isPlaying = false

    init {
        initAudioTrack()
    }

    private fun initAudioTrack() {
        val bufferSize = AudioTrack.getMinBufferSize(SAMPLE_RATE, CHANNELS, ENCODING)
        val actualBufferSize = max(bufferSize, BUFFER_SIZE_FRAMES * 4 * 2) // 4 bytes per float, 2 channels

        audioTrack = AudioTrack.Builder()
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_GAME)
                    .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                    .build(),
            )
            .setAudioFormat(
                AudioFormat.Builder()
                    .setSampleRate(SAMPLE_RATE)
                    .setChannelMask(CHANNELS)
                    .setEncoding(ENCODING)
                    .build(),
            )
            .setBufferSizeInBytes(actualBufferSize)
            .setTransferMode(AudioTrack.MODE_STREAM)
            .build()

        audioTrack?.play()
        isPlaying = true

        // Start a thread to continuously feed audio data
        Thread {
            val buffer = FloatArray(BUFFER_SIZE_FRAMES * 2) // Stereo
            while (isPlaying) {
                try {
                    // Get audio data from the sound handler
                    soundHandler.fillBuffer(buffer)

                    // Write to AudioTrack
                    audioTrack?.write(buffer, 0, buffer.size, AudioTrack.WRITE_BLOCKING)
                } catch (e: Exception) {
                    logger.error { "Error in audio playback: ${e.message}" }
                }
            }
        }.start()
    }

    override fun createSoundHandler(): SoundHandler = soundHandler

    override fun masterVolume(volume: Percent) {
        super.masterVolume(volume)
        audioTrack?.setVolume(volume)
    }

    override fun close() {
        isPlaying = false
        audioTrack?.stop()
        audioTrack?.release()
        audioTrack = null
    }
}

class AndroidSoundHandler : SoundHandler {
    private val mixBuffer = FloatArray(AndroidSoundManager.BUFFER_SIZE_FRAMES * 2)
    private var currentMusic: FloatArray? = null
    private var currentSfx = mutableListOf<FloatArray>()
    private var musicPosition = 0
    private var sfxPositions = mutableMapOf<FloatArray, Int>()

    override fun playMusic(data: FloatArray) {
        currentMusic = data
        musicPosition = 0
    }

    override fun playSfx(data: FloatArray) {
        currentSfx.add(data)
        sfxPositions[data] = 0
    }

    override fun stopMusic() {
        currentMusic = null
        musicPosition = 0
    }

    override fun stopSfx() {
        currentSfx.clear()
        sfxPositions.clear()
    }

    fun fillBuffer(buffer: FloatArray) {
        // Clear the buffer
        buffer.fill(0f)

        // Mix music
        currentMusic?.let { music ->
            var bufferPos = 0
            while (bufferPos < buffer.size && musicPosition < music.size) {
                buffer[bufferPos] += music[musicPosition] * 0.5f // Scale down to prevent clipping
                bufferPos++
                musicPosition++
            }

            // Loop music if needed
            if (musicPosition >= music.size) {
                musicPosition = 0
            }
        }

        // Mix sound effects
        val completedSfx = mutableListOf<FloatArray>()
        for (sfx in currentSfx) {
            val position = sfxPositions[sfx] ?: 0
            var bufferPos = 0
            var sfxPos = position

            while (bufferPos < buffer.size && sfxPos < sfx.size) {
                buffer[bufferPos] += sfx[sfxPos] * 0.5f // Scale down to prevent clipping
                bufferPos++
                sfxPos++
            }

            sfxPositions[sfx] = sfxPos

            // Mark completed sound effects
            if (sfxPos >= sfx.size) {
                completedSfx.add(sfx)
            }
        }

        // Remove completed sound effects
        for (sfx in completedSfx) {
            currentSfx.remove(sfx)
            sfxPositions.remove(sfx)
        }

        // Clamp values to prevent distortion
        for (i in buffer.indices) {
            buffer[i] = buffer[i].coerceIn(-1f, 1f)
        }
    }
}
