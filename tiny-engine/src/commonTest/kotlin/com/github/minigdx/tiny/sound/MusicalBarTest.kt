package com.github.minigdx.tiny.sound

import com.github.minigdx.tiny.lua.Note
import kotlin.test.Test
import kotlin.test.assertContentEquals

class MusicalBarTest {
    @Test
    fun it_set_unique_note() {
        val bar = MusicalBar()
        bar.setNote(Note.Gs0, 1f, 2f, true)

        // Start before the actual note
        bar.setNote(Note.E5, 0.5f, 1f, true)
        assertContentEquals(listOf(Note.E5), bar.beats.map { it.note })

        // Start in the actual note
        bar.setNote(Note.G5, 1f, 1f, true)
        assertContentEquals(listOf(Note.G5), bar.beats.map { it.note })

        // Overlap the actual note
        bar.setNote(Note.B5, 0.5f, 3f, true)
        assertContentEquals(listOf(Note.B5), bar.beats.map { it.note })

        // Just another note
        bar.setNote(Note.C5, 4.5f, 3f, true)
        assertContentEquals(listOf(Note.B5, Note.C5), bar.beats.map { it.note })
    }

    @Test
    fun it_support_sequential_notes() {
        val bar = MusicalBar()
        bar.setNote(Note.Cs5, 1f, 0.5f, true)
        bar.setNote(Note.Cs5, 1.5f, 0.5f, true)

        assertContentEquals(listOf(Note.Cs5, Note.Cs5), bar.beats.map { it.note })
    }
}
