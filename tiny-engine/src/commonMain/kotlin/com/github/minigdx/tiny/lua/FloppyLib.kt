package com.github.minigdx.tiny.lua

import com.github.mingdx.tiny.doc.TinyArg
import com.github.mingdx.tiny.doc.TinyCall
import com.github.mingdx.tiny.doc.TinyFunction
import com.github.mingdx.tiny.doc.TinyLib
import com.github.minigdx.tiny.log.Logger
import com.github.minigdx.tiny.platform.Platform
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.boolean
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.double
import kotlinx.serialization.json.doubleOrNull
import kotlinx.serialization.json.int
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.long
import kotlinx.serialization.json.longOrNull
import org.luaj.vm2.LuaTable
import org.luaj.vm2.LuaValue
import org.luaj.vm2.lib.OneArgFunction
import org.luaj.vm2.lib.TwoArgFunction

@TinyLib("floppy", "Floppy allow you to get or save user Lua structure.")
class FloppyLib(
    private val platform: Platform,
    private val logger: Logger,
) : TwoArgFunction() {
    override fun call(
        arg1: LuaValue,
        arg2: LuaValue,
    ): LuaValue {
        val ws = LuaTable()
        ws["put"] = put()
        ws["get"] = get()

        arg2.set("ws", ws)
        arg2.get("package").get("loaded").set("ws", ws)
        return ws
    }

    @TinyFunction(
        "Save the content into a local file, " +
            "on desktop or in the local storage on the web platform.",
    )
    internal inner class put : TwoArgFunction() {
        @TinyCall("Save the content into the file name.")
        override fun call(
            @TinyArg("name") arg1: LuaValue,
            @TinyArg("content") arg2: LuaValue,
        ): LuaValue {
            val filename = arg1.checkjstring() ?: return NIL
            val jsonElement = luaValueToJson(arg2, mutableSetOf())
            val json = Json.encodeToString(JsonElement.serializer(), jsonElement)
            platform.saveIntoHome(filename, json)
            return NIL
        }
    }

    @TinyFunction("Load and get the content of the file name")
    internal inner class get : OneArgFunction() {
        @TinyCall("Load and get the content of the file name")
        override fun call(
            @TinyArg("name") arg: LuaValue,
        ): LuaValue {
            val filename = arg.checkjstring() ?: return NIL
            val content = platform.getFromHome(filename) ?: return NIL

            return try {
                val jsonElement = Json.parseToJsonElement(content)
                jsonToLuaValue(jsonElement)
            } catch (_: Exception) {
                NIL
            }
        }
    }

    private fun jsonToLuaValue(element: JsonElement): LuaValue {
        return when (element) {
            is JsonObject -> {
                val table = LuaTable()
                element.entries.forEach { (key, value) ->
                    table.set(key, jsonToLuaValue(value))
                }
                table
            }
            is JsonArray -> {
                val table = LuaTable()
                element.forEachIndexed { index, value ->
                    table.set(index + 1, jsonToLuaValue(value)) // Lua arrays are 1-indexed
                }
                table
            }
            is JsonPrimitive -> {
                when {
                    element.isString -> valueOf(element.content)
                    element.booleanOrNull != null -> valueOf(element.boolean)
                    element.intOrNull != null -> valueOf(element.int)
                    element.longOrNull != null -> valueOf(element.long.toDouble())
                    element.doubleOrNull != null -> valueOf(element.double)
                    else -> NIL
                }
            }
        }
    }

    private fun luaValueToJson(
        value: LuaValue,
        visited: MutableSet<LuaValue>,
    ): JsonElement {
        return when {
            value.isnil() -> JsonPrimitive(null as String?)
            value.isboolean() -> JsonPrimitive(value.toboolean())
            value.isint() -> JsonPrimitive(value.toint())
            value.islong() -> JsonPrimitive(value.tolong())
            value.isnumber() -> JsonPrimitive(value.todouble())
            value.isstring() -> JsonPrimitive(value.tojstring())
            value.istable() -> {
                if (value in visited) {
                    logger.warn("FLOPPY") {
                        "Circular reference found in the data used by floppy.put. " +
                            "The circular reference will be removed from the object and " +
                            "will not be present using floppy.get. " +
                            "Please fix it in your game to avoid unexpected behaviour."
                    }
                    JsonNull
                } else {
                    val table = value.checktable() ?: return JsonPrimitive(value.tojstring())
                    // Check if it's an array (consecutive integer keys starting from 1)
                    val keys = table.keys()
                    val isArray = keys.all { it.isint() } &&
                        keys.map { it.toint() }.sorted() == (1..keys.size).toList()

                    // Save the actual value as visited to avoid circular dependencies.
                    visited.add(value)

                    if (isArray) {
                        // Convert as JSON array
                        val array = mutableListOf<JsonElement>()
                        for (i in 1..table.length()) {
                            array.add(luaValueToJson(table.get(i), visited))
                        }
                        JsonArray(array)
                    } else {
                        // Convert as JSON object
                        val map = mutableMapOf<String, JsonElement>()
                        keys.forEach { key ->
                            val keyString = key.tojstring()
                            map[keyString] = luaValueToJson(table.get(key), visited)
                        }
                        JsonObject(map)
                    }
                }
            }
            else -> JsonPrimitive(value.tojstring())
        }
    }
}
