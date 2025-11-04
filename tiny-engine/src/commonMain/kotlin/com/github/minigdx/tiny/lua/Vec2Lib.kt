package com.github.minigdx.tiny.lua

import com.github.mingdx.tiny.doc.LuaType
import com.github.mingdx.tiny.doc.TinyArg
import com.github.mingdx.tiny.doc.TinyCall
import com.github.mingdx.tiny.doc.TinyFunction
import com.github.mingdx.tiny.doc.TinyLib
import org.luaj.vm2.LuaTable
import org.luaj.vm2.LuaValue
import org.luaj.vm2.Varargs
import org.luaj.vm2.lib.LibFunction
import org.luaj.vm2.lib.TwoArgFunction
import org.luaj.vm2.lib.VarArgFunction

@TinyLib("vec2", "Vector2 manipulation library.")
class Vec2Lib : TwoArgFunction() {
    override fun call(
        arg1: LuaValue,
        arg2: LuaValue,
    ): LuaValue {
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
        override fun call(
            @TinyArg("x", type = LuaType.NUMBER) arg1: LuaValue,
            @TinyArg("y", type = LuaType.NUMBER) arg2: LuaValue,
        ): LuaValue {
            val (x, y) = extract(arg1, arg2)
            val defaultX =
                if (x.isnil()) {
                    ZERO
                } else {
                    x
                }

            val defaultY =
                if (y.isnil()) {
                    ZERO
                } else {
                    y
                }
            return v2(defaultX, defaultY)
        }

        @TinyCall("Create a vector 2 as a table { x, y } using another vector 2.")
        override fun call(
            @TinyArg("vec2", type = LuaType.TABLE) arg: LuaValue,
        ): LuaValue = super.call(arg)
    }

    @TinyFunction("Add vector2 to another vector2", example = VECTOR2_ADD)
    class add : VarArgFunction() {
        override fun invoke(args: Varargs): Varargs {
            val (x1, y1, x2, y2) = extract(args)
            return v2(
                valueOf(x1.todouble() + x2.todouble()),
                valueOf(y1.todouble() + y2.todouble()),
            )
        }

        @TinyCall("Add a vector 2 {x, y} to another vector 2 {x, y}")
        override fun call(
            @TinyArg("v1", "vector 2 as a table {x, y}", type = LuaType.TABLE) arg1: LuaValue,
            @TinyArg("v2", "vector 2 as a table {x, y}", type = LuaType.TABLE) arg2: LuaValue,
        ): LuaValue = super.call(arg1, arg2)

        @TinyCall("Add a destructured vector 2 to another destructured vector 2")
        override fun call(
            @TinyArg("x1", type = LuaType.NUMBER) a: LuaValue,
            @TinyArg("y1", type = LuaType.NUMBER) b: LuaValue,
            @TinyArg("x2", type = LuaType.NUMBER) c: LuaValue,
            @TinyArg("y2", type = LuaType.NUMBER) d: LuaValue,
        ): LuaValue = super.call(a, b, c, d)
    }

    @TinyFunction("Subtract another vector from another vector", example = VECTOR2_SUB)
    class sub : VarArgFunction() {
        override fun invoke(args: Varargs): Varargs {
            val (x1, y1, x2, y2) = extract(args)
            return v2(
                valueOf(x1.todouble() - x2.todouble()),
                valueOf(y1.todouble() - y2.todouble()),
            )
        }

        @TinyCall("Subtract a vector 2 {x, y} from another vector 2 {x, y}")
        override fun call(
            @TinyArg("v1", "vector 2 as a table {x, y}", type = LuaType.TABLE) arg1: LuaValue,
            @TinyArg("v2", "vector 2 as a table {x, y}", type = LuaType.TABLE) arg2: LuaValue,
        ): LuaValue = super.call(arg1, arg2)

        @TinyCall("Subtract a destructured vector 2 from another destructured vector 2")
        override fun call(
            @TinyArg("x1", type = LuaType.NUMBER) a: LuaValue,
            @TinyArg("y1", type = LuaType.NUMBER) b: LuaValue,
            @TinyArg("x2", type = LuaType.NUMBER) c: LuaValue,
            @TinyArg("y2", type = LuaType.NUMBER) d: LuaValue,
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
            @TinyArg("v1", "vector 2 as a table {x, y}", type = LuaType.TABLE) a: LuaValue,
            @TinyArg("v2", "vector 2 as a table {x, y}", type = LuaType.TABLE) b: LuaValue,
        ): LuaValue = super.call(a, b)

        @TinyCall("Dot product between a destructured vector 2 and another destructured vector 2")
        override fun call(
            @TinyArg("x1", type = LuaType.NUMBER) a: LuaValue,
            @TinyArg("y1", type = LuaType.NUMBER) b: LuaValue,
            @TinyArg("x2", type = LuaType.NUMBER) c: LuaValue,
            @TinyArg("y2", type = LuaType.NUMBER) d: LuaValue,
        ): LuaValue = super.call(a, b, c, d)
    }

    @TinyFunction("Calculate the magnitude (length) of a vector")
    class mag : VarArgFunction() {
        override fun invoke(args: Varargs): Varargs {
            val (x1, y1) = extract(args)
            return valueOf(kotlin.math.sqrt(x1.todouble() * x1.todouble() + y1.todouble() * y1.todouble()))
        }

        @TinyCall("Calculate the magnitude (length) of a vector 2 {x, y}")
        override fun call(
            @TinyArg("x", type = LuaType.NUMBER) arg1: LuaValue,
            @TinyArg("y", type = LuaType.NUMBER) arg2: LuaValue,
        ): LuaValue = super.call(arg1, arg2)

        @TinyCall("Calculate the magnitude (length) of a vector 2 {x, y}")
        override fun call(
            @TinyArg("v1", "vector 2 as a table {x, y}", type = LuaType.TABLE) arg: LuaValue,
        ): LuaValue = super.call(arg)
    }

    @TinyFunction("Normalize a vector", example = VECTOR_NOR)
    class nor : VarArgFunction() {
        override fun invoke(args: Varargs): Varargs {
            val (x1, y1) = extract(args)
            val len = kotlin.math.sqrt(x1.todouble() * x1.todouble() + y1.todouble() * y1.todouble())
            return if (len != 0.0) {
                v2(valueOf(x1.todouble() / len), valueOf(y1.todouble() / len))
            } else {
                v2(valueOf(0), valueOf(0))
            }
        }

        @TinyCall("Normalize a vector 2 {x, y}")
        override fun call(
            @TinyArg("x", type = LuaType.NUMBER) arg1: LuaValue,
            @TinyArg("y", type = LuaType.NUMBER) arg2: LuaValue,
        ): LuaValue = super.call(arg1, arg2)

        @TinyCall("Normalize a vector 2 {x, y}")
        override fun call(
            @TinyArg("v1", "vector 2 as a table {x, y}", type = LuaType.TABLE) arg: LuaValue,
        ): LuaValue = super.call(arg)
    }

    @TinyFunction("Cross product")
    class crs : VarArgFunction() {
        override fun invoke(args: Varargs): Varargs {
            val (x1, y1, x2, y2) = extract(args)
            return valueOf(
                x1.todouble() * y2.todouble() - y1.todouble() * x2.todouble(),
            )
        }

        @TinyCall("Cross product between a vector 2 {x, y} and another vector 2 {x, y}")
        override fun call(
            @TinyArg("v1", "vector 2 as a table {x, y}", type = LuaType.TABLE) arg1: LuaValue,
            @TinyArg("v2", "vector 2 as a table {x, y}", type = LuaType.TABLE) arg2: LuaValue,
        ): LuaValue = super.call(arg1, arg2)

        @TinyCall("Cross product between a destructured vector 2 and another destructured vector 2")
        override fun call(
            @TinyArg("x1", type = LuaType.NUMBER) a: LuaValue,
            @TinyArg("y1", type = LuaType.NUMBER) b: LuaValue,
            @TinyArg("x2", type = LuaType.NUMBER) c: LuaValue,
            @TinyArg("y2", type = LuaType.NUMBER) d: LuaValue,
        ): LuaValue = super.call(a, b, c, d)
    }

    @TinyFunction("Scale a vector", example = VECTOR2_SCL)
    class scl : VarArgFunction() {
        override fun invoke(args: Varargs): Varargs {
            val (x1, y1, scale) = extract(args)
            return v2(valueOf(x1.todouble() * scale.todouble()), valueOf(y1.todouble() * scale.todouble()))
        }

        @TinyCall("Scale a vector 2 {x, y} using the factor scl")
        override fun call(
            @TinyArg("x", type = LuaType.NUMBER) arg1: LuaValue,
            @TinyArg("y", type = LuaType.NUMBER) arg2: LuaValue,
            @TinyArg("scl", type = LuaType.NUMBER) arg3: LuaValue,
        ): LuaValue = super.call(arg1, arg2, arg3)

        @TinyCall("Scale a vector 2 {x, y} using the factor scl")
        override fun call(
            @TinyArg("v1", "vector 2 as a table {x, y}", type = LuaType.TABLE) arg1: LuaValue,
            @TinyArg("scl", type = LuaType.NUMBER) arg2: LuaValue,
        ): LuaValue = super.call(arg1, arg2)
    }

    companion object {
        fun extract(
            arg1: LuaValue,
            arg2: LuaValue,
        ): List<LuaValue> {
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

        fun v2(
            arg1: LuaValue,
            arg2: LuaValue,
        ): LuaTable {
            return LuaTable().apply {
                set("x", arg1)
                set("y", arg2)
            }
        }
    }
}
