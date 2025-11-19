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
}
