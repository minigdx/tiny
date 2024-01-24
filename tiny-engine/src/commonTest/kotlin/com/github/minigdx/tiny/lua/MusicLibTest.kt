package com.github.minigdx.tiny.lua

import kotlin.test.Test
import kotlin.test.assertEquals

class MusicLibTest {

    fun trim(str: String): String {
        val lastIndex = str.lastIndexOf(')')
        if (lastIndex < 0) return str
        return str.substring(0, lastIndex + 2)
    }

    @Test
    fun trimMusic() {
        val str = "*-*-sine(Eb)-*-*-"

        assertEquals("*-*-sine(Eb)-", trim(str))
    }
}
