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

        val music: Music = Json.decodeFromString(bytes.decodeToString())

        val sounds = music.musicalBars.map { bar -> soundManager.convert(bar) }

        return SoundData(name, soundManager, music, sounds)
    }

    override fun wasModified(): Boolean = delegate.wasModified()
}
