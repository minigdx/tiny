package com.github.minigdx.tiny.lua

import com.github.mingdx.tiny.doc.TinyArg
import com.github.mingdx.tiny.doc.TinyCall
import com.github.mingdx.tiny.doc.TinyFunction
import com.github.mingdx.tiny.doc.TinyLib
import org.luaj.vm2.LuaTable
import org.luaj.vm2.LuaValue
import org.luaj.vm2.Varargs
import org.luaj.vm2.lib.LibFunction
import org.luaj.vm2.lib.TwoArgFunction

@TinyLib("vec2", "Vector2 manipulation library.")
class Vec2Lib : TwoArgFunction() {
    override fun call(arg1: LuaValue, arg2: LuaValue): LuaValue {
        val vector2 = LuaTable()
        vector2["create"] = create()
        vector2["add"] = add()
        vector2["sub"] = sub()
        vector2["dot"] = dot()
        vector2["crs"] = crs()
        vector2["mag"] = mag()
        vector2["nor"] = nor()
        vector2["scl"] = scl()
        arg2.set("vec2", vector2)
        arg2.get("package").get("loaded").set("vec2", vector2)
        return vector2
    }

    @TinyFunction("Create a vector 2 as a table { x, y }.")
    class create : TwoArgFunction() {

        @TinyCall("Create a vector 2 as a table { x, y }.")
        override fun call(@TinyArg("x") arg1: LuaValue, @TinyArg("y") arg2: LuaValue): LuaValue {
            val (x, y) = extract(arg1, arg2)
            return v2(x, y)
        }

        @TinyCall("Create a vector 2 as a table { x, y } using another vector 2.")
        override fun call(@TinyArg("vec2") arg: LuaValue): LuaValue = super.call(arg)
    }

    @TinyFunction("Add vector2 to another vector2", example = VECTOR2_ADD)
    class add : LibFunction() {
        override fun invoke(args: Varargs): Varargs {
            val (x1, y1, x2, y2) = extract(args)
            return v2(
                valueOf(x1.todouble() + x2.todouble()),
                valueOf(y1.todouble() + y2.todouble()),
            )
        }

        @TinyCall("Add a vector 2 {x, y} to another vector 2 {x, y}")
        override fun call(
            @TinyArg("v1", "vector 2 as a table {x, y}") a: LuaValue,
            @TinyArg("v2", "vector 2 as a table {x, y}") b: LuaValue,
        ): LuaValue = super.call(a, b)

        @TinyCall("Add a destructured vector 2 to another destructured vector 2")
        override fun call(
            @TinyArg("x1") a: LuaValue,
            @TinyArg("y1") b: LuaValue,
            @TinyArg("x2") c: LuaValue,
            @TinyArg("y2") d: LuaValue,
        ): LuaValue = super.call(a, b, c, d)
    }

    @TinyFunction("Subtract another vector to a vector", example = VECTOR2_SUB)
    class sub : LibFunction() {
        override fun invoke(args: Varargs): Varargs {
            val (x1, y1, x2, y2) = extract(args)
            return v2(
                valueOf(x1.todouble() - x2.todouble()),
                valueOf(y1.todouble() - y2.todouble()),
            )
        }

        @TinyCall("Subtract a vector 2 {x, y} from another vector 2 {x, y}")
        override fun call(
            @TinyArg("v1", "vector 2 as a table {x, y}") a: LuaValue,
            @TinyArg("v2", "vector 2 as a table {x, y}") b: LuaValue,
        ): LuaValue = super.call(a, b)

        @TinyCall("Subtract a destructured vector 2 from another destructured vector 2")
        override fun call(
            @TinyArg("x1") a: LuaValue,
            @TinyArg("y1") b: LuaValue,
            @TinyArg("x2") c: LuaValue,
            @TinyArg("y2") d: LuaValue,
        ): LuaValue = super.call(a, b, c, d)
    }

    @TinyFunction("Dot product between two vectors", example = VECTOR_DOT)
    class dot : LibFunction() {
        override fun invoke(args: Varargs): Varargs {
            val (x1, y1, x2, y2) = extract(args)
            return valueOf(x1.todouble() * x2.todouble() + y1.todouble() * y2.todouble())
        }

        @TinyCall("Dot product between a vector 2 {x, y} and another vector 2 {x, y}")
        override fun call(
            @TinyArg("v1", "vector 2 as a table {x, y}") a: LuaValue,
            @TinyArg("v2", "vector 2 as a table {x, y}") b: LuaValue,
        ): LuaValue = super.call(a, b)

        @TinyCall("Dot product between a destructured vector 2 and another destructured vector 2")
        override fun call(
            @TinyArg("x1") a: LuaValue,
            @TinyArg("y1") b: LuaValue,
            @TinyArg("x2") c: LuaValue,
            @TinyArg("y2") d: LuaValue,
        ): LuaValue = super.call(a, b, c, d)
    }

    @TinyFunction("Calculate the magnitude (length) of a vector")
    class mag : LibFunction() {
        override fun invoke(args: Varargs): Varargs {
            val (x1, y1) = extract(args)
            return valueOf(kotlin.math.sqrt(x1.todouble() * x1.todouble() + y1.todouble() * y1.todouble()))
        }

        @TinyCall("Calculate the magnitude (length) of a vector 2 {x, y}")
        override fun call(
            @TinyArg("x") a: LuaValue,
            @TinyArg("y") b: LuaValue,
        ): LuaValue = super.call(a, b)

        @TinyCall("Calculate the magnitude (length) of a vector 2 {x, y}")
        override fun call(
            @TinyArg("v1", "vector 2 as a table {x, y}") a: LuaValue,
        ): LuaValue = super.call(a)
    }

    @TinyFunction("Normalize a vector", example = VECTOR_NOR)
    class nor : LibFunction() {
        override fun invoke(args: Varargs): Varargs {
            val (x1, y1) = extract(args)
            val len = kotlin.math.sqrt(x1.todouble() * x1.todouble() + y1.todouble() * y1.todouble())
            return if (len != 0.0) {
                v2(valueOf(x1.todouble() / len), valueOf(y1.todouble() / len))
            } else {
                NIL
            }
        }

        @TinyCall("Normalize a vector 2 {x, y}")
        override fun call(
            @TinyArg("x") a: LuaValue,
            @TinyArg("y") b: LuaValue,
        ): LuaValue = super.call(a, b)

        @TinyCall("Normalize a vector 2 {x, y}")
        override fun call(@TinyArg("v1", "vector 2 as a table {x, y}") a: LuaValue): LuaValue = super.call(a)
    }

    @TinyFunction("Cross product")
    class crs : LibFunction() {
        override fun invoke(args: Varargs): Varargs {
            val (x1, y1, x2, y2) = extract(args)
            return valueOf(
                x1.todouble() * y2.todouble() - y1.todouble() * x2.todouble(),
            )
        }

        @TinyCall("Cross product between a vector 2 {x, y} and another vector 2 {x, y}")
        override fun call(
            @TinyArg("v1", "vector 2 as a table {x, y}") a: LuaValue,
            @TinyArg("v2", "vector 2 as a table {x, y}") b: LuaValue,
        ): LuaValue = super.call(a, b)

        @TinyCall("Cross product between a destructured vector 2 and another destructured vector 2")
        override fun call(
            @TinyArg("x1") a: LuaValue,
            @TinyArg("y1") b: LuaValue,
            @TinyArg("x2") c: LuaValue,
            @TinyArg("y2") d: LuaValue,
        ): LuaValue = super.call(a, b, c, d)
    }

    @TinyFunction("Scale a vector", example = VECTOR2_SCL)
    class scl : LibFunction() {
        override fun invoke(args: Varargs): Varargs {
            val (x1, y1, scale) = extract(args)
            return v2(valueOf(x1.todouble() * scale.todouble()), valueOf(y1.todouble() * scale.todouble()))
        }

        @TinyCall("Scale a vector 2 {x, y} using the factor scl")
        override fun call(
            @TinyArg("x") a: LuaValue,
            @TinyArg("y") b: LuaValue,
            @TinyArg("scl") c: LuaValue,
        ): LuaValue = super.call(a, b, c)

        @TinyCall("Scale a vector 2 {x, y} using the factor scl")
        override fun call(
            @TinyArg("v1", "vector 2 as a table {x, y}") a: LuaValue,
            @TinyArg("scl") b: LuaValue,
        ): LuaValue = super.call(a, b)
    }

    companion object {
        fun extract(arg1: LuaValue, arg2: LuaValue): List<LuaValue> {
            return if (arg1.istable()) {
                listOf(arg1.get("x"), arg1.get("y"))
            } else {
                listOf(arg1, arg2)
            }
        }

        fun extract(args: Varargs): List<LuaValue> {
            var index = 1
            val result = mutableListOf<LuaValue>()
            var current = args.arg(index)
            while (index <= args.narg()) {
                if (current.istable()) {
                    result.add(current.get("x"))
                    result.add(current.get("y"))
                    index++
                } else {
                    if (args.narg() - index >= 0) {
                        result.add(current)
                        result.add(args.arg(++index))
                        index++
                    }
                }
                current = args.arg(index)
            }
            return result
        }

        fun v2(arg1: LuaValue, arg2: LuaValue): LuaTable {
            return LuaTable().apply {
                set("x", arg1)
                set("y", arg2)
            }
        }
    }
}
