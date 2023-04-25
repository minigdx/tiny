package com.github.minigdx.tiny.lua

import com.github.mingdx.tiny.doc.TinyArg
import com.github.mingdx.tiny.doc.TinyArgs
import com.github.mingdx.tiny.doc.TinyCall
import com.github.mingdx.tiny.doc.TinyFunction
import com.github.mingdx.tiny.doc.TinyLib
import com.github.minigdx.tiny.engine.GameOptions
import com.github.minigdx.tiny.engine.GameResourceAccess
import org.luaj.vm2.LuaTable
import org.luaj.vm2.LuaValue
import org.luaj.vm2.Varargs
import org.luaj.vm2.lib.LibFunction
import org.luaj.vm2.lib.OneArgFunction
import org.luaj.vm2.lib.ThreeArgFunction
import org.luaj.vm2.lib.TwoArgFunction
import org.luaj.vm2.lib.VarArgFunction
import kotlin.math.PI

interface StdLibListener {
    fun exit(nextScriptIndex: Int)
}

@TinyLib
class StdLib(
    val gameOptions: GameOptions,
    val resourceAccess: GameResourceAccess,
    val listener: StdLibListener
) : TwoArgFunction() {

    override fun call(arg1: LuaValue, arg2: LuaValue): LuaValue {
        val tiny = LuaTable()
        arg2["exit"] = exit()
        arg2["pset"] = pset() // move to gfx
        arg2["pget"] = pget() // move to gfx
        arg2["cls"] = cls() // move to gfx
        arg2["all"] = all()
        arg2["rpairs"] = rpairs()
        arg2["print"] = print()
        arg2["debug"] = debug()
        arg2["PI"] = valueOf(PI) // move to math
        return tiny
    }

    @TinyFunction(
        "Iterate over values of a table. " +
            "If you want to iterate over keys, use pairs(table). " +
            "If you want to iterate over index, use ipairs(table). "
    )
    internal inner class all : VarArgFunction() {

        @TinyCall("Iterate over the values of the table")
        override fun invoke(@TinyArgs(arrayOf("table")) args: Varargs): Varargs {
            val iterator = object : VarArgFunction() {

                var index = 0

                override fun invoke(args: Varargs): Varargs {
                    val table = args.checktable(1)!!
                    index += 1

                    val luaValue = table[index]
                    if (luaValue.isnil()) return NONE

                    // Return only the value.
                    return varargsOf(arrayOf(luaValue))
                }
            }
            val table = args.checktable(1)!!
            // iterator, object to iterate, seed value.
            return varargsOf(iterator, table, valueOf(0))
        }
    }

    @TinyFunction(
        "Iterate over values of a table in reverse order. " +
            "The iterator return an index and the value. " +
            "The method is useful to remove elements from a table while " +
            "iterating on it."
    )
    internal inner class rpairs : VarArgFunction() {

        @TinyCall("Iterate over the values of the table")
        override fun invoke(@TinyArgs(arrayOf("table")) args: Varargs): Varargs {
            val iterator = object : VarArgFunction() {

                override fun invoke(args: Varargs): Varargs {
                    val table = args.checktable(1)!!
                    val index = args.checkint(2) - 1

                    if (index < 1) return NONE

                    val luaValue = table[index]
                    if (luaValue.isnil()) return NONE

                    // Return only the value.
                    return varargsOf(arrayOf(valueOf(index), luaValue))
                }
            }
            val table = args.checktable(1)!!
            // iterator, object to iterate, seed value.
            return varargsOf(iterator, table, valueOf(table.length() + 1))
        }
    }

    internal inner class debug : OneArgFunction() {
        override fun call(arg: LuaValue): LuaValue {
            println("🐞 -> $arg")
            return NONE
        }
    }

    @TinyFunction("Print on the screen a string.", example = STD_PRINT_EXAMPLE)
    internal inner class print : LibFunction() {

        @TinyCall(
            description = "print on the screen a string at (0,0) with a default color.",
        )
        override fun call(@TinyArg("str") a: LuaValue): LuaValue {
            return call(a, valueOf(0), valueOf(0), valueOf("#FFFFFF"))
        }

        @TinyCall(
            description = "print on the screen a string with a default color.",
        )
        override fun call(@TinyArg("str") a: LuaValue, @TinyArg("x") b: LuaValue, @TinyArg("y") c: LuaValue): LuaValue {
            return call(a, b, c, valueOf("#FFFFFF"))
        }

        @TinyCall(
            description = "print on the screen a string with a specific color.",
        )
        override fun call(
            @TinyArg("str") a: LuaValue,
            @TinyArg("x") b: LuaValue,
            @TinyArg("y") c: LuaValue,
            @TinyArg("color") d: LuaValue
        ): LuaValue {
            val str = a.checkjstring() ?: return NONE
            val x = b.checkint()
            val y = c.checkint()
            val color = d.checkColorIndex()

            val spritesheet = resourceAccess.bootSpritesheet ?: return NONE

            val space = 4
            var currentX = x
            str.forEach { char ->

                val coord = if (char.isLetter()) {
                    // The character has an accent. Let's try to get rid of it
                    val l = if (char.hasAccent) {
                        ACCENT_MAP[char.lowercaseChar()] ?: char.lowercaseChar()
                    } else {
                        char.lowercaseChar()
                    }
                    val index = l - 'a'
                    index to 0
                } else if (char.isDigit()) {
                    val index = char.lowercaseChar() - '0'
                    index to 1
                } else if (char in '!'..'/') {
                    val index = char.lowercaseChar() - '!'
                    index to 2
                } else if (char in '['..'`') {
                    val index = char.lowercaseChar() - '['
                    index to 3
                } else if (char in '{'..'~') {
                    val index = char.lowercaseChar() - '{'
                    index to 4
                } else if (char in ':'..'@') {
                    val index = char.lowercaseChar() - ':'
                    index to 5
                } else {
                    // Maybe it's an emoji: try EMOJI MAP conversion
                    EMOJI_MAP[char]
                }
                if (coord != null) {
                    val (indexX, indexY) = coord
                    resourceAccess.frameBuffer.copyFrom(
                        spritesheet.pixels, currentX, y,
                        indexX * 4,
                        indexY * 4,
                        4, 4,
                    ) { pixel: Array<Int>, _, _ ->
                        if (pixel[0] == 0) {
                            pixel
                        } else {
                            pixel[0] = color
                            pixel
                        }
                    }
                }
                currentX += space
            }

            return NONE
        }

        val Char.hasAccent: Boolean
            get() = this.isLetter() && this.lowercaseChar() !in 'a'..'z'
    }

    internal inner class exit : OneArgFunction() {

        override fun call(arg: LuaValue): LuaValue {
            listener.exit(arg.checkint())
            return NONE
        }
    }

    private fun LuaValue.checkColorIndex(): Int {
        return if (this.isnumber()) {
            this.checkint()
        } else {
            resourceAccess.frameBuffer.gamePalette.getColorIndex(this.checkjstring()!!)
        }
    }

    @TinyFunction("clear the screen")
    internal inner class cls : OneArgFunction() {
        @TinyCall("Clear the screen with a default color.")
        override fun call(): LuaValue {
            return call(valueOf("#000000"))
        }

        @TinyCall("Clear the screen with a color.")
        override fun call(@TinyArg("color") arg: LuaValue): LuaValue {
            resourceAccess.frameBuffer.clear(arg.checkColorIndex())
            return NONE
        }
    }

    internal inner class pset : ThreeArgFunction() {
        // x, y, index
        override fun call(arg1: LuaValue, arg2: LuaValue, arg3: LuaValue): LuaValue {
            resourceAccess.frameBuffer.pixel(arg1.checkint(), arg2.checkint(), arg3.checkint())
            return NONE
        }
    }

    internal inner class pget : TwoArgFunction() {
        override fun call(arg1: LuaValue, arg2: LuaValue): LuaValue {
            val index = resourceAccess.frameBuffer.pixel(arg1.checkint(), arg2.checkint())
            return valueOf(index)
        }
    }

    companion object {
        val ACCENT_MAP = mapOf(
            'à' to 'a', 'á' to 'a', 'â' to 'a', 'ã' to 'a', 'ä' to 'a', 'å' to 'a',
            'ç' to 'c',
            'è' to 'e', 'é' to 'e', 'ê' to 'e', 'ë' to 'e',
            'ì' to 'i', 'í' to 'i', 'î' to 'i', 'ï' to 'i',
            'ñ' to 'n',
            'ò' to 'o', 'ó' to 'o', 'ô' to 'o', 'õ' to 'o', 'ö' to 'o',
            'ù' to 'u', 'ú' to 'u', 'û' to 'u', 'ü' to 'u',
            'ý' to 'y', 'ÿ' to 'y'
        )

        val EMOJI_MAP = mapOf(
            '⚠' to (0 to 0),
        )
    }
}
