package com.github.minigdx.tiny.resources.ldtk

import kotlin.test.Test

class LdtkTest {
    @Test
    fun loadLdtkFile() {
        val content = LdtkTest::class.java.classLoader.getResource("reflections.ldtk")!!.readText()
        println(Ldtk.read(content))
    }
}
