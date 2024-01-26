package com.github.minigdx.tiny.file

import com.github.minigdx.tiny.platform.SoundData
import com.github.minigdx.tiny.sound.SoundManager

class SoundDataSourceStream(
    private val name: String,
    val soundManager: SoundManager,
    val delegate: SourceStream<ByteArray>,
) : SourceStream<SoundData> {

    override suspend fun exists(): Boolean = delegate.exists()

    override suspend fun read(): SoundData {
        val bytes = delegate.read()

        val sound = if (name.endsWith(".sfx")) {
            soundManager.createSfxSound(bytes)
        } else {
            soundManager.createMidiSound(bytes)
        }
        return SoundData(name, sound)
    }

    override fun wasModified(): Boolean = delegate.wasModified()
}
