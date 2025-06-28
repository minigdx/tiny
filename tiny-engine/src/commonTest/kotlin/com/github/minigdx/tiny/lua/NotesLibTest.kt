package com.github.minigdx.tiny.lua

import kotlin.test.Test
import kotlin.test.assertEquals

class NotesLibTest {
    @Test
    fun octave() {
        assertEquals(0, Note.C0.octave)
        assertEquals(1, Note.C1.octave)
        assertEquals(1, Note.B1.octave)
        assertEquals(5, Note.C5.octave)
    }
}
