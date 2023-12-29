package com.github.minigdx.tiny.lua

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

    @TinyFunction("Create a vector 2 as a table { x, y }")
    class create : TwoArgFunction() {
        override fun call(arg1: LuaValue, arg2: LuaValue): LuaValue {
            val (x, y) = extract(arg1, arg2)
            return v2(x, y)
        }

        override fun call(arg: LuaValue): LuaValue = super.call(arg)
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
    }

    @TinyFunction("Dot product between two vectors")
    class dot : LibFunction() {
        override fun invoke(args: Varargs): Varargs {
            val (x1, y1, x2, y2) = extract(args)
            return valueOf(x1.todouble() * x2.todouble() + y1.todouble() * y2.todouble())
        }
    }

    @TinyFunction("Calculate the magnitude (length) of a vector")
    class mag : LibFunction() {
        override fun invoke(args: Varargs): Varargs {
            val (x1, y1) = extract(args)
            return valueOf(kotlin.math.sqrt(x1.todouble() * x1.todouble() + y1.todouble() * y1.todouble()))
        }
    }

    @TinyFunction("Normalize a vector")
    class nor : LibFunction() {
        override fun invoke(args: Varargs): Varargs {
            val (x1, y1) = extract(args)
            val len = kotlin.math.sqrt(x1.todouble() * x1.todouble() + y1.todouble() * y1.todouble())
            if (len != 0.0) {
                return v2(valueOf(x1.todouble() / len), valueOf(y1.todouble() / len))
            } else {
                return NIL
            }
        }
    }

    @TinyFunction("Cross product")
    class crs : LibFunction() {
        override fun invoke(args: Varargs): Varargs {
            val (x1, y1, x2, y2) = extract(args)
            return valueOf(
                x1.todouble() * y2.todouble() - y1.todouble() * x2.todouble(),
            )
        }
    }

    @TinyFunction("Scale a vector", example = VECTOR2_SCL)
    class scl : LibFunction() {
        override fun invoke(args: Varargs): Varargs {
            val (x1, y1, scale) = extract(args)
            return v2(valueOf(x1.todouble() * scale.todouble()), valueOf(y1.todouble() * scale.todouble()))
        }
    }

    companion object {
        fun extract(arg1: LuaValue, arg2: LuaValue): List<LuaValue> {
            return if (arg1.istable()) {
                listOf(arg1.get("x"), arg2.get("y"))
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
