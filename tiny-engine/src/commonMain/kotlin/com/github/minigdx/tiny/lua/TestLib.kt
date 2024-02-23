package com.github.minigdx.tiny.lua

import com.github.mingdx.tiny.doc.TinyArg
import com.github.mingdx.tiny.doc.TinyCall
import com.github.mingdx.tiny.doc.TinyFunction
import com.github.mingdx.tiny.doc.TinyLib
import com.github.minigdx.tiny.resources.GameScript
import org.luaj.vm2.LuaTable
import org.luaj.vm2.LuaValue
import org.luaj.vm2.lib.OneArgFunction
import org.luaj.vm2.lib.TwoArgFunction

class TestResult(val script: String, val test: String, val passed: Boolean, val reason: String)

@TinyLib(
    "test",
    "Test method utilities used when tests are run. " +
        "See link:#_the_tiny_cli_run_command[Run command]",
)
class TestLib(private val script: GameScript) : TwoArgFunction() {

    private val currentScript = script.name

    private var currentTest: String = ""

    private var firstFailure: String? = null

    open inner class Assertor(
        private val default: LuaValue? = null,
        private val invert: Boolean = false,
        private val message: String = "#1 expected to be equals to #2 but is not!",
    ) : TwoArgFunction() {
        override fun call(arg1: LuaValue, arg2: LuaValue): LuaValue {
            val luaValue = default ?: arg2
            val result = arg1.eq_b(luaValue)
            return if (invert && !result) {
                BTRUE
            } else if (result) {
                BTRUE
            } else {
                if (firstFailure == null) {
                    val msg = message
                        .replace("#1", arg1.tojstring())
                        .replace("#2", luaValue.tojstring())
                    firstFailure = msg
                }
                BFALSE
            }
        }
    }

    @TinyFunction(name = "eq", description = "Assert that `expected` and `actual` are equals")
    inner class isEqual : Assertor() {

        @TinyCall("Assert that `expected` and `actual` are equals")
        override fun call(@TinyArg("expected") arg1: LuaValue, @TinyArg("actual") arg2: LuaValue): LuaValue {
            return super.call(arg1, arg2)
        }
    }

    @TinyFunction(name = "neq", description = "Assert that `expected` and `actual` are __not__ equals")
    inner class isNotEquals : Assertor(invert = true) {
        @TinyCall("Assert that `expected` and `actual` are not equals")
        override fun call(@TinyArg("expected") arg1: LuaValue, @TinyArg("actual") arg2: LuaValue): LuaValue {
            return super.call(arg1, arg2)
        }
    }

    @TinyFunction(name = "t", description = "Assert that `actual` is true")
    inner class isTrue : Assertor(default = BTRUE) {
        @TinyCall("Assert that `actual` is true")
        override fun call(@TinyArg("actual") arg: LuaValue): LuaValue {
            return super.call(arg)
        }
    }

    @TinyFunction(name = "t", description = "Assert that `actual` is false")
    inner class isFalse : Assertor(default = BFALSE) {

        @TinyCall("Assert that `actual` is false")
        override fun call(@TinyArg("actual") arg: LuaValue): LuaValue {
            return super.call(arg)
        }
    }
    override fun call(arg1: LuaValue, arg2: LuaValue): LuaValue {
        val test = LuaTable()
        test["create"] = create()
        test["eq"] = isEqual()
        test["neq"] = isNotEquals()
        test["t"] = isTrue()
        test["f"] = isFalse()
        // test["advance"] = ...
        test["record"] = record()
        test["screen"] = screen()

        arg2.set("test", test)
        arg2.get("package").get("loaded").set("test", test)
        return test
    }

    @TinyFunction("Create a new `test` named `name`")
    inner class create : TwoArgFunction() {
        @TinyCall("Create a new `test` named `name`")
        override fun call(
            @TinyArg("name", description = "The name of the test") arg1: LuaValue,
            @TinyArg("test", description = "The test: it has to be a function") arg2: LuaValue,
        ): LuaValue {
            currentTest = arg1.tojstring()

            firstFailure = null
            arg2.call()
            firstFailure
            if (firstFailure == null) {
                script.testResults.add(TestResult(currentScript, currentTest, true, ""))
            } else {
                script.testResults.add(TestResult(currentScript, currentTest, false, firstFailure!!))
            }

            return NIL
        }
    }

    inner class record : OneArgFunction() {
        override fun call(arg: LuaValue): LuaValue {
            // TODO: get the test current name
            TODO("Not yet implemented")
        }
    }

    inner class screen : OneArgFunction() {
        override fun call(arg: LuaValue): LuaValue {
            TODO("Not yet implemented")
        }
    }
}
