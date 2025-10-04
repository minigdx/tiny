package com.github.minigdx.tiny.lua

import com.github.minigdx.tiny.engine.GameOptions
import com.github.minigdx.tiny.log.StdOutLogger
import com.github.minigdx.tiny.platform.test.HeadlessPlatform
import org.luaj.vm2.LuaTable
import org.luaj.vm2.LuaValue
import org.luaj.vm2.LuaValue.Companion.valueOf
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class FloppyLibTest {
    private val platform = HeadlessPlatform(
        GameOptions(
            width = 256,
            height = 256,
            palette = listOf("#000000", "#FFFFFF"),
            gameScripts = emptyList(),
            spriteSheets = emptyList(),
        ),
        emptyMap(),
    )

    private val lib = FloppyLib(platform, StdOutLogger("floppy-test"))

    @Test
    fun putAndGetSimpleTable() {
        val put = lib.put()
        val get = lib.get()

        val table = LuaTable()
        table["name"] = valueOf("test")
        table["value"] = valueOf(42)
        table["flag"] = valueOf(true)

        put.call(valueOf("test.json"), table)
        val result = get.call(valueOf("test.json")).checktable()!!

        assertEquals("test", result["name"].tojstring())
        assertEquals(42, result["value"].toint())
        assertTrue(result["flag"].toboolean())
    }

    @Test
    fun putAndGetNestedTable() {
        val put = lib.put()
        val get = lib.get()

        val nested = LuaTable()
        nested["x"] = valueOf(10)
        nested["y"] = valueOf(20)

        val table = LuaTable()
        table["position"] = nested
        table["id"] = valueOf(123)

        put.call(valueOf("nested.json"), table)
        val result = get.call(valueOf("nested.json")).checktable()!!

        assertEquals(123, result["id"].toint())
        val resultNested = result["position"].checktable()!!
        assertEquals(10, resultNested["x"].toint())
        assertEquals(20, resultNested["y"].toint())
    }

    @Test
    fun putAndGetArray() {
        val put = lib.put()
        val get = lib.get()

        val array = LuaTable()
        array[1] = valueOf(10)
        array[2] = valueOf(20)
        array[3] = valueOf(30)

        put.call(valueOf("array.json"), array)
        val result = get.call(valueOf("array.json")).checktable()!!

        assertEquals(10, result[1].toint())
        assertEquals(20, result[2].toint())
        assertEquals(30, result[3].toint())
        assertEquals(3, result.length())
    }

    @Test
    fun putAndGetMixedStructure() {
        val put = lib.put()
        val get = lib.get()

        val items = LuaTable()
        items[1] = valueOf("apple")
        items[2] = valueOf("banana")
        items[3] = valueOf("cherry")

        val config = LuaTable()
        config["enabled"] = valueOf(true)
        config["level"] = valueOf(5)

        val table = LuaTable()
        table["items"] = items
        table["config"] = config
        table["name"] = valueOf("test game")

        put.call(valueOf("game.json"), table)
        val result = get.call(valueOf("game.json")).checktable()!!

        assertEquals("test game", result["name"].tojstring())

        val resultItems = result["items"].checktable()!!
        assertEquals("apple", resultItems[1].tojstring())
        assertEquals("banana", resultItems[2].tojstring())
        assertEquals("cherry", resultItems[3].tojstring())

        val resultConfig = result["config"].checktable()!!
        assertTrue(resultConfig["enabled"].toboolean())
        assertEquals(5, resultConfig["level"].toint())
    }

    @Test
    fun putAndGetWithNilValues() {
        val put = lib.put()
        val get = lib.get()

        val table = LuaTable()
        table["exists"] = valueOf("yes")
        table["missing"] = LuaValue.NIL

        put.call(valueOf("nil.json"), table)
        val result = get.call(valueOf("nil.json")).checktable()!!

        assertEquals("yes", result["exists"].tojstring())
        assertTrue(result["missing"].isnil())
    }

    @Test
    fun putAndGetWithDifferentNumberTypes() {
        val put = lib.put()
        val get = lib.get()

        val table = LuaTable()
        table["integer"] = valueOf(42)
        table["long"] = valueOf(1234567890123.0)
        table["double"] = valueOf(3.14159)

        put.call(valueOf("numbers.json"), table)
        val result = get.call(valueOf("numbers.json")).checktable()!!

        assertEquals(42, result["integer"].toint())
        assertEquals(1234567890123.0, result["long"].todouble())
        assertEquals(3.14159, result["double"].todouble(), 0.00001)
    }

    @Test
    fun getNonExistentFile() {
        val get = lib.get()
        val result = get.call(valueOf("nonexistent.json"))
        assertTrue(result.isnil())
    }

    @Test
    fun putWithCircularReference() {
        val put = lib.put()
        val get = lib.get()

        val tableA = LuaTable()
        val tableB = LuaTable()

        tableA["name"] = valueOf("A")
        tableB["name"] = valueOf("B")

        // Create circular reference: A -> B -> A
        tableA["ref"] = tableB
        tableB["ref"] = tableA

        put.call(valueOf("circular.json"), tableA)
        val table = get.call(valueOf("circular.json")).checktable()!!

        assertEquals(LuaValue.NIL, table["ref"]["ref"])
    }

    @Test
    fun putWithFunction() {
        val put = lib.put()

        val table = LuaTable()
        table["name"] = valueOf("test")
        table["func"] = object : org.luaj.vm2.lib.ZeroArgFunction() {
            override fun call(): LuaValue {
                return valueOf("hello")
            }
        }

        put.call(valueOf("function.json"), table)
        // If it succeeds, verify we can read it back
        val get = lib.get()
        val result = get.call(valueOf("function.json")).checktable()!!
        // The function should be converted to a string representation
        assertTrue(result.get("func").isstring())
    }
}
