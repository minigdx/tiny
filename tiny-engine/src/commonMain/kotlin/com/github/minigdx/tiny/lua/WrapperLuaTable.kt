package com.github.minigdx.tiny.lua

import org.luaj.vm2.LuaTable
import org.luaj.vm2.LuaValue
import org.luaj.vm2.lib.OneArgFunction
import org.luaj.vm2.lib.TwoArgFunction
import org.luaj.vm2.lib.ZeroArgFunction
import kotlin.collections.get

typealias Getter = () -> LuaValue
typealias Setter = (LuaValue) -> Unit

/**
 * LuaTable that give convenient methods to "wrap" a Kotlin object
 */
class WrapperLuaTable : LuaTable() {
    private val getters: MutableMap<String, Getter> = mutableMapOf()
    private val setters: MutableMap<String, Setter> = mutableMapOf()

    fun wrap(
        name: String,
        getter: Getter,
    ) {
        getters[name] = getter
    }

    fun wrap(
        name: String,
        getter: Getter,
        setter: Setter,
    ) {
        getters[name] = getter
        setters[name] = setter
    }

    fun function0(
        name: String,
        function: () -> LuaValue,
    ) {
        val zeroArgFunction =
            object : ZeroArgFunction() {
                override fun call(): LuaValue {
                    return function()
                }
            }
        getters[name] = { zeroArgFunction }
    }

    fun function1(
        name: String,
        function: (value: LuaValue) -> LuaValue,
    ) {
        val oneArgFunction =
            object : OneArgFunction() {
                override fun call(arg: LuaValue): LuaValue {
                    return function(arg)
                }
            }
        getters[name] = { oneArgFunction }
    }

    fun function2(
        name: String,
        function: (a: LuaValue, b: LuaValue) -> LuaValue,
    ) {
        val functionWrapper = object : TwoArgFunction() {
            override fun call(
                arg1: LuaValue,
                arg2: LuaValue,
            ): LuaValue {
                return function(arg1, arg2)
            }
        }
        getters[name] = { functionWrapper }
    }

    override fun get(key: LuaValue): LuaValue {
        val name = key.checkjstring()
        return getters[name]?.invoke() ?: NIL
    }

    override fun set(
        key: LuaValue,
        value: LuaValue,
    ) {
        val name = key.checkjstring()
        setters[name]?.invoke(value)
    }
}
