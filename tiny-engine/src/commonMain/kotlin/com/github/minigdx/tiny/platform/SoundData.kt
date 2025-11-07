package com.github.minigdx.tiny.platform

import com.github.minigdx.tiny.lua.Note
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
        val DEFAULT_EMPTY = SoundData(
            "default",
            Music(),
            emptyList(),
        )

        val DEFAULT_SFX = SoundData(
            "default",
            Music().apply {
                musicalBars[0].setNote(Note.E4, 0f, 0.5f)
                musicalBars[0].setNote(Note.G4, 1f, 0.5f)
            },
            emptyList(),
        )
    }
}
