package com.github.minigdx.tiny.platform

import com.github.minigdx.tiny.sound.Music

class SoundData(
    // Name of the file.
    val name: String,
    // Deserialized data of the file.
    val music: Music,
    // Ready to play musical bars. (sfx)
    val musicalBars: List<FloatArray>,
    // Ready to play musical sequences (music)
    val musicalSequences: List<FloatArray> = emptyList(),
) {
    companion object {
        val DEFAULT = SoundData(
            "default",
            Music(),
            emptyList(),
        )
    }
}
