package com.github.minigdx.tiny.lua

import com.github.minigdx.tiny.engine.GameOptions
import com.github.minigdx.tiny.engine.GameResourceAccess
import com.github.minigdx.tiny.render.VirtualFrameBuffer
import dev.mokkery.mock
import org.luaj.vm2.LuaTable
import org.luaj.vm2.LuaValue.Companion.valueOf
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class StdLibTest {
    val gameOptions = GameOptions(100, 100)
    val gameResourceAccess = mock<GameResourceAccess> { }
    val virtualFrameBuffer = mock<VirtualFrameBuffer> { }
    val lib = StdLib(gameOptions, gameResourceAccess, virtualFrameBuffer)

    @Test
    fun `new should create instance with access to class fields`() {
        val newFunc = lib.new()

        // Create a class table with active = true
        val classTable = LuaTable()
        classTable.set("active", valueOf(true))

        // Create instance with one argument
        val instance = newFunc.call(classTable)

        // Instance should have access to 'active' field via metatable lookup
        val activeValue = instance.get("active")
        assertTrue(activeValue.toboolean(), "Instance should have access to 'active' field from class")
    }

    @Test
    fun `new with two arguments should override class fields`() {
        val newFunc = lib.new()

        // Create a class table with active = true
        val classTable = LuaTable()
        classTable.set("active", valueOf(true))

        // Create instance with one argument
        val default = LuaTable()
        default.set("active", valueOf("test"))
        val instance = newFunc.call(classTable, default)

        // Instance should have access to 'active' field via metatable lookup
        val activeValue = instance.get("active")
        assertEquals("test", activeValue.tojstring())
    }

    @Test
    fun `new should keep instance neested instance`() {
        val newFunc = lib.new()

        // Create a class table with active = true
        val classTable = LuaTable()
        classTable.set("active", valueOf(true))

        // Create instance with one argument
        val childInstance = newFunc.call(classTable)

        val classPlayerTable = LuaTable()
        val defaultPlayer = LuaTable()
        defaultPlayer.set("child", childInstance)

        val instance = newFunc.call(classPlayerTable, defaultPlayer)
        // Instance should have access to 'active' field via metatable lookup
        val activeValue = instance.get("child").get("active")
        assertTrue(activeValue.toboolean(), "Nested instance should preserve metatable and access 'active' field")
    }

    @Test
    fun `textw returns width of single line`() {
        val textwFunc = lib.textw()
        // "hello" = 5 chars * 4px = 20
        assertEquals(20, textwFunc.call(valueOf("hello")).toint())
    }

    @Test
    fun `textw returns width of widest line with newlines`() {
        val textwFunc = lib.textw()
        // "bonjour" = 7 chars * 4px = 28 (widest line)
        assertEquals(28, textwFunc.call(valueOf("hello\nbonjour")).toint())
    }

    @Test
    fun `textw returns 0 for empty string`() {
        val textwFunc = lib.textw()
        assertEquals(0, textwFunc.call(valueOf("")).toint())
    }

    @Test
    fun `texth returns height for single line`() {
        val texthFunc = lib.texth()
        // 1 line * 6px = 6
        assertEquals(6, texthFunc.call(valueOf("hello")).toint())
    }

    @Test
    fun `texth returns height with newlines`() {
        val texthFunc = lib.texth()
        // 2 lines * 6px = 12
        assertEquals(12, texthFunc.call(valueOf("hello\nworld")).toint())
    }

    @Test
    fun `texth with width wraps text and returns correct height`() {
        val texthFunc = lib.texth()
        // "hello world" with width 24px = 6 chars max per line
        // "hello" (5 chars) fits, "world" (5 chars) next line -> 2 lines * 6px = 12
        assertEquals(12, texthFunc.call(valueOf("hello world"), valueOf(24)).toint())
    }

    @Test
    fun `texth with large width returns single line height`() {
        val texthFunc = lib.texth()
        // "hello world" fits in 200px -> 1 line * 6px = 6
        assertEquals(6, texthFunc.call(valueOf("hello world"), valueOf(200)).toint())
    }

    @Test
    fun `wrapText splits at word boundaries`() {
        // 20px wide = 5 chars max per line
        val lines = StdLib.wrapText("ab cd ef gh", 20)
        // "ab cd" (5 chars) fits, "ef gh" (5 chars) fits -> 2 lines
        assertEquals(2, lines.size)
        assertEquals("ab cd", lines[0])
        assertEquals("ef gh", lines[1])
    }

    @Test
    fun `wrapText handles empty string`() {
        val lines = StdLib.wrapText("", 40)
        assertEquals(1, lines.size)
        assertEquals("", lines[0])
    }

    @Test
    fun `wrapText preserves explicit newlines`() {
        val lines = StdLib.wrapText("hello\nworld", 200)
        assertEquals(2, lines.size)
        assertEquals("hello", lines[0])
        assertEquals("world", lines[1])
    }

    @Test
    fun `wrapText keeps long words intact`() {
        // Word "abcdefghij" is 10 chars, but max is 5 chars per line
        // The word should still be kept intact (not split mid-word)
        val lines = StdLib.wrapText("abcdefghij", 20)
        assertEquals(1, lines.size)
        assertEquals("abcdefghij", lines[0])
    }

    @Test
    fun `charToCoord maps letters correctly`() {
        // 'a' is at index 0, row 0
        assertEquals(0 to 0, StdLib.charToCoord('a'))
        // 'z' is at index 25, row 0
        assertEquals(25 to 0, StdLib.charToCoord('z'))
        // uppercase maps to lowercase
        assertEquals(0 to 0, StdLib.charToCoord('A'))
    }

    @Test
    fun `charToCoord maps digits correctly`() {
        assertEquals(0 to 1, StdLib.charToCoord('0'))
        assertEquals(9 to 1, StdLib.charToCoord('9'))
    }

    @Test
    fun `charToCoord returns null for space`() {
        // space is not mapped to any glyph
        assertEquals(null, StdLib.charToCoord(' '))
    }

    @Test
    fun `merge should copy all keys from source into dest`() {
        val mergeFunc = lib.merge()

        val src = LuaTable()
        src.set("x", valueOf(1))
        src.set("y", valueOf(2))
        src.set("z", valueOf(3))

        val dst = LuaTable()
        dst.set("a", valueOf(4))
        dst.set("b", valueOf(5))

        val result = mergeFunc.call(src, dst)

        // Source keys copied into dest
        assertEquals(1, result.get("x").toint())
        assertEquals(2, result.get("y").toint())
        assertEquals(3, result.get("z").toint())
        // Original dest keys preserved
        assertEquals(4, result.get("a").toint())
        assertEquals(5, result.get("b").toint())
    }
}
