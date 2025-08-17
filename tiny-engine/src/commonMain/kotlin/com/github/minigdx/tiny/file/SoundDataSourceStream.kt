package com.github.minigdx.tiny.file

import com.github.minigdx.tiny.platform.SoundData
import com.github.minigdx.tiny.sound.Music
import com.github.minigdx.tiny.sound.SoundManager
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json

class SoundDataSourceStream(
    private val name: String,
    val soundManager: SoundManager,
    val delegate: SourceStream<ByteArray>,
) : SourceStream<SoundData> {
    override suspend fun exists(): Boolean = delegate.exists()

    override suspend fun read(): SoundData {
        val bytes = delegate.read()

        val json = Json { ignoreUnknownKeys = true }
        val music: Music = json.decodeFromString(bytes.decodeToString())
        music.instruments = Array(16) {
            if (it in 0 until music.instruments.size) {
                music.instruments[it]
            } else {
                null
            }
        }
        music.musicalBars.forEach { musicBar ->
            musicBar.instrument = music.instruments[musicBar.instrumentIndex]
        }
        music.sequences.forEach { sequence ->
            sequence.tracks.forEach { track ->
                track.instrument = music.instruments[track.instrumentIndex]
            }
        }

        val sounds = music.musicalBars.map { bar -> soundManager.convert(bar) }

        return SoundData(name, music, sounds)
    }

    override fun wasModified(): Boolean = delegate.wasModified()
}
