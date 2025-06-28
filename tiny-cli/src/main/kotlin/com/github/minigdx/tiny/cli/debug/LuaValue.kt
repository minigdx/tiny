package com.github.minigdx.tiny.cli.debug

import kotlinx.serialization.Serializable

/**
 * Represents a Lua value in a structured way for serialization.
 * This is used to represent Lua values in the debugger UI.
 */
@Serializable
sealed class LuaValue {
    /**
     * Represents a primitive Lua value (string, number, boolean, nil).
     */
    @Serializable
    data class Primitive(val value: String) : LuaValue()

    /**
     * Represents a Lua dictionary (table).
     */
    @Serializable
    data class Dictionary(val entries: Map<String, LuaValue>) : LuaValue()
}
