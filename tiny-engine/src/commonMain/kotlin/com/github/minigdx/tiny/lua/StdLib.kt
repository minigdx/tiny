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
import org.luaj.vm2.lib.TwoArgFunction
import org.luaj.vm2.lib.VarArgFunction

@TinyLib(description = "Standard library.")
class StdLib(
    val gameOptions: GameOptions,
    val resourceAccess: GameResourceAccess,
) : TwoArgFunction() {

    override fun call(arg1: LuaValue, arg2: LuaValue): LuaValue {
        val tiny = LuaTable()
        arg2["all"] = all()
        arg2["rpairs"] = rpairs()
        arg2["print"] = print()
        arg2["debug"] = debug()
        arg2["new"] = new()
        return tiny
    }

    @TinyFunction(
        "Create new instance of a class by creating a new table and setting the metatable. " +
            "It allow to create kind of Object Oriented Programming.\n\n ",
        example = STD_NEW_EXAMPLE,
    )
    inner class new : TwoArgFunction() {

        @TinyCall("Create new instance of class.")
        override fun call(@TinyArg("class") arg: LuaValue): LuaValue {
            return super.call(arg)
        }

        @TinyCall("Create new instance of class using default values.")
        override fun call(@TinyArg("class") arg1: LuaValue, @TinyArg("default") arg2: LuaValue): LuaValue {
            val default = if (arg2.istable()) {
                arg2.checktable()!!
            } else {
                LuaTable()
            }
            default.setmetatable(arg1)
            arg1.rawset("__index", arg1)
            return default
        }
    }

    @TinyFunction(
        "Iterate over values of a table.\n\n" +
            "- If you want to iterate over keys, use pairs(table).\n " +
            "- If you want to iterate over index, use ipairs(table).\n " +
            "- If you want to iterate in reverse, use rpairs(table).\n",
    )
    internal inner class all : VarArgFunction() {

        @TinyCall("Iterate over the values of the table")
        override fun invoke(@TinyArgs(["table"]) args: Varargs): Varargs {
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
            "iterating on it.",
        example = STD_RPAIRS_EXAMPLE,
    )
    internal inner class rpairs : VarArgFunction() {

        @TinyCall("Iterate over the values of the table")
        override fun invoke(@TinyArgs(["table"]) args: Varargs): Varargs {
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

    @TinyFunction("Print in the console a value to help debugging")
    internal inner class debug : OneArgFunction() {

        @TinyCall("Print in the console a value")
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
            @TinyArg("color") d: LuaValue,
        ): LuaValue {
            val str = a.checkjstring() ?: return NONE
            val x = b.checkint()
            val y = c.checkint()
            val color = d.checkColorIndex()

            val spritesheet = resourceAccess.bootSpritesheet ?: return NONE

            val space = 4
            var currentX = x
            var currentY = y
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
                } else if (char == '\n') {
                    currentY += 6
                    currentX = x - space // compensate the next space
                    null
                } else {
                    // Maybe it's an emoji: try EMOJI MAP conversion
                    EMOJI_MAP[char]
                }
                if (coord != null) {
                    val (indexX, indexY) = coord
                    resourceAccess.frameBuffer.copyFrom(
                        spritesheet.pixels,
                        currentX,
                        currentY,
                        indexX * 4,
                        indexY * 4,
                        4,
                        4,
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

    private fun LuaValue.checkColorIndex(): Int {
        return if (this.isnumber()) {
            this.checkint()
        } else {
            resourceAccess.frameBuffer.gamePalette.getColorIndex(this.checkjstring()!!)
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
            'ý' to 'y', 'ÿ' to 'y',
        )

        val EMOJI_MAP = mapOf(
            '⚠' to (0 to 0),
        )
    }
}
