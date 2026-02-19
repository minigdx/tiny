package com.github.minigdx.tiny.lua

import com.github.mingdx.tiny.doc.LuaType
import com.github.mingdx.tiny.doc.TinyArg
import com.github.mingdx.tiny.doc.TinyArgs
import com.github.mingdx.tiny.doc.TinyCall
import com.github.mingdx.tiny.doc.TinyFunction
import com.github.mingdx.tiny.doc.TinyLib
import com.github.minigdx.tiny.engine.GameOptions
import com.github.minigdx.tiny.engine.GameResourceAccess
import com.github.minigdx.tiny.render.VirtualFrameBuffer
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
    val virtualFrameBuffer: VirtualFrameBuffer,
) : TwoArgFunction() {
    override fun call(
        arg1: LuaValue,
        arg2: LuaValue,
    ): LuaValue {
        val tiny = LuaTable()
        arg2["all"] = all()
        arg2["rpairs"] = rpairs()
        arg2["print"] = print()
        arg2["printf"] = printf()
        arg2["textw"] = textw()
        arg2["texth"] = texth()
        arg2["merge"] = merge()
        arg2["append"] = append()
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
        override fun call(
            @TinyArg("class", type = LuaType.TABLE) arg: LuaValue,
        ): LuaValue {
            return super.call(arg)
        }

        @TinyCall("Create new instance of class using default values.")
        override fun call(
            @TinyArg("class", type = LuaType.TABLE) arg1: LuaValue,
            @TinyArg("default", type = LuaType.TABLE) arg2: LuaValue,
        ): LuaValue {
            val default = if (arg2.istable()) {
                arg2.checktable()!!.deepCopy()
            } else {
                LuaTable()
            }
            val reference = arg1.checktable()!!.deepCopy()
            default.setmetatable(reference)
            reference.rawset("__index", reference)
            return default
        }

        private fun LuaTable.deepCopy(): LuaTable {
            val result = LuaTable()
            this.keys().forEach { key ->
                var value = this[key]
                value = if (value.istable()) {
                    value.checktable()!!.deepCopy()
                } else {
                    value
                }
                result[key] = value
            }
            // Preserve the metatable during deep copy
            val metatable = this.getmetatable()
            if (metatable != null) {
                result.setmetatable(metatable)
            }
            return result
        }
    }

    @TinyFunction(
        "Add *all key/value* from the table `source` to the table `dest`.",
        example = STD_MERGE_EXAMPLE,
    )
    inner class merge : TwoArgFunction() {
        @TinyCall("Merge source into dest.")
        override fun call(
            @TinyArg("source", type = LuaType.TABLE) arg1: LuaValue,
            @TinyArg("dest", type = LuaType.TABLE) arg2: LuaValue,
        ): LuaValue {
            return if (arg1.istable() and arg2.istable()) {
                arg1 as LuaTable
                arg2 as LuaTable

                val keys = arg1.keys()
                keys.forEach { k ->
                    val value = arg1.get(k)
                    arg2[k] = value
                }
                arg2
            } else {
                NIL
            }
        }
    }

    @TinyFunction(
        "Append *all values* from the table `source` to the table `dest`.",
        example = STD_APPEND_EXAMPLE,
    )
    inner class append : TwoArgFunction() {
        @TinyCall("Copy source into dest.")
        override fun call(
            @TinyArg("source", type = LuaType.TABLE) arg1: LuaValue,
            @TinyArg("dest", type = LuaType.TABLE) arg2: LuaValue,
        ): LuaValue {
            return if (arg1.istable() and arg2.istable()) {
                arg1 as LuaTable
                arg2 as LuaTable

                arg1.keys().forEach { key ->
                    val value = arg1[key]
                    arg2.insert(0, value)
                }
                arg2
            } else {
                NIL
            }
        }
    }

    @TinyFunction(
        "Iterate over values of a table.\n\n" +
            "- If you want to iterate over keys, use `pairs(table)`.\n " +
            "- If you want to iterate over index, use `ipairs(table)`.\n " +
            "- If you want to iterate in reverse, use `rpairs(table)`.\n",
    )
    internal inner class all : VarArgFunction() {
        @TinyCall("Iterate over the values of the table")
        override fun invoke(
            @TinyArgs(names = ["table"], types = [LuaType.TABLE]) args: Varargs,
        ): Varargs {
            val iterator =
                object : VarArgFunction() {
                    var index = 0

                    override fun invoke(args: Varargs): Varargs {
                        val table = args.checktable(1)!!
                        val keys = table.keys()
                        if (index >= keys.size) return NONE

                        val key = keys[index++]
                        val result = table.get(key)

                        return result
                    }
                }
            // If the expected table is nil, don't iterate.
            val table = if (args.isnil(1)) {
                LuaTable()
            } else {
                args.checktable(1)!!
            }
            // iterator, object to iterate, seed value.
            return varargsOf(iterator, table)
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
        override fun invoke(
            @TinyArgs(names = ["table"], types = [LuaType.TABLE]) args: Varargs,
        ): Varargs {
            val iterator =
                object : VarArgFunction() {
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

    @TinyFunction("Print on the screen a string.", example = STD_PRINT_EXAMPLE)
    internal inner class print : LibFunction() {
        @TinyCall(
            description = "print on the screen a string at (0,0) with a default color.",
        )
        override fun call(
            @TinyArg("str", type = LuaType.STRING) a: LuaValue,
        ): LuaValue {
            return call(a, valueOf(0), valueOf(0), valueOf("#FFFFFF"))
        }

        @TinyCall(
            description = "print on the screen a string with a default color.",
        )
        override fun call(
            @TinyArg("str", type = LuaType.STRING) a: LuaValue,
            @TinyArg("x", type = LuaType.NUMBER) b: LuaValue,
            @TinyArg("y", type = LuaType.NUMBER) c: LuaValue,
        ): LuaValue {
            return call(a, b, c, valueOf("#FFFFFF"))
        }

        @TinyCall(
            description = "print on the screen a string with a specific color.",
        )
        override fun call(
            @TinyArg("str", type = LuaType.STRING) a: LuaValue,
            @TinyArg("x", type = LuaType.NUMBER) b: LuaValue,
            @TinyArg("y", type = LuaType.NUMBER) c: LuaValue,
            @TinyArg("color", type = LuaType.ANY) d: LuaValue,
        ): LuaValue {
            val spritesheet = resourceAccess.bootSpritesheet ?: return NONE
            val str = a.tojstring()
            val x = b.checkint()
            val y = c.checkint()
            val color = d.checkColorIndex()

            drawText(spritesheet, str, x, y, color)

            return NONE
        }
    }

    @TinyFunction(
        "Print on the screen a string with word wrapping and optional alignment. " +
            "Alignment: 0 = left (default), 1 = center, 2 = right.",
        example = STD_PRINTF_EXAMPLE,
    )
    internal inner class printf : VarArgFunction() {
        @TinyCall(
            description = "Print a word-wrapped string with optional alignment.",
        )
        override fun invoke(
            @TinyArgs(
                names = ["str", "x", "y", "color", "width", "align"],
                types = [LuaType.STRING, LuaType.NUMBER, LuaType.NUMBER, LuaType.ANY, LuaType.NUMBER, LuaType.NUMBER],
            )
            args: Varargs,
        ): Varargs {
            val spritesheet = resourceAccess.bootSpritesheet ?: return NONE

            val str = args.checkjstring(1) ?: return NONE
            val x = args.checkint(2)
            val y = args.checkint(3)
            val color = args.arg(4).checkColorIndex()
            val maxWidth = args.checkint(5)
            val align = args.optint(6, 0) // 0=left, 1=center, 2=right

            val lines = wrapText(str, maxWidth)

            lines.forEachIndexed { lineIndex, line ->
                val lineWidth = line.length * CHAR_SPACING
                val offsetX = when (align) {
                    1 -> (maxWidth - lineWidth) / 2 // center
                    2 -> maxWidth - lineWidth // right
                    else -> 0 // left
                }

                val currentY = y + lineIndex * LINE_HEIGHT
                drawText(spritesheet, line, x + offsetX, currentY, color)
            }

            return NONE
        }
    }

    @TinyFunction(
        "Measure the width of a text string in pixels. " +
            "Each character is 4 pixels wide. " +
            "If the string contains newlines, it returns the width of the widest line.",
        example = STD_TEXTW_EXAMPLE,
    )
    internal inner class textw : OneArgFunction() {
        @TinyCall(
            description = "Return the width of the string in pixels.",
        )
        override fun call(
            @TinyArg("str", type = LuaType.STRING) arg: LuaValue,
        ): LuaValue {
            val str = arg.tojstring()
            return valueOf(measureWidth(str))
        }
    }

    @TinyFunction(
        "Measure the height of a text string in pixels. " +
            "Without a width parameter, it counts lines separated by newlines. " +
            "With a width parameter, the text is word-wrapped before measuring.",
        example = STD_TEXTH_EXAMPLE,
    )
    internal inner class texth : LibFunction() {
        @TinyCall(
            description = "Return the height of the string in pixels.",
        )
        override fun call(
            @TinyArg("str", type = LuaType.STRING) arg: LuaValue,
        ): LuaValue {
            val str = arg.tojstring()
            val lines = str.split('\n')
            return valueOf(lines.size * LINE_HEIGHT)
        }

        @TinyCall(
            description = "Return the height of the string in pixels with word wrapping.",
        )
        override fun call(
            @TinyArg("str", type = LuaType.STRING) arg1: LuaValue,
            @TinyArg("width", type = LuaType.NUMBER) arg2: LuaValue,
        ): LuaValue {
            val str = arg1.tojstring()
            val maxWidth = arg2.checkint()
            val lines = wrapText(str, maxWidth)
            return valueOf(lines.size * LINE_HEIGHT)
        }
    }

    private fun drawText(
        spritesheet: com.github.minigdx.tiny.resources.SpriteSheet,
        str: String,
        x: Int,
        y: Int,
        color: Int,
    ) {
        var currentX = x
        var currentY = y
        str.forEach { char ->
            if (char == '\n') {
                currentY += LINE_HEIGHT
                currentX = x - CHAR_SPACING // compensate the next advance
            } else {
                val coord = charToCoord(char)
                if (coord != null) {
                    val (indexX, indexY) = coord
                    virtualFrameBuffer.drawMonocolor(
                        spritesheet,
                        color,
                        indexX * CHAR_SPACING,
                        indexY * CHAR_SPACING,
                        CHAR_SPACING,
                        CHAR_SPACING,
                        currentX,
                        currentY,
                        flipX = false,
                        flipY = false,
                    )
                }
            }
            currentX += CHAR_SPACING
        }
    }

    private fun LuaValue.checkColorIndex(): Int {
        val colors = gameOptions.colors()
        return if (this.isnumber()) {
            colors.check(this.checkint())
        } else {
            colors.getColorIndex(this.checkjstring()!!)
        }
    }

    companion object {
        const val CHAR_SPACING = 4
        const val LINE_HEIGHT = 6

        fun charToCoord(char: Char): Pair<Int, Int>? {
            return if (char.isLetter()) {
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
                EMOJI_MAP[char]
            }
        }

        fun measureWidth(str: String): Int {
            if (str.isEmpty()) return 0
            return str.split('\n').maxOf { line -> line.length * CHAR_SPACING }
        }

        fun wrapText(str: String, maxWidth: Int): List<String> {
            val result = mutableListOf<String>()
            val maxCharsPerLine = maxWidth / CHAR_SPACING

            if (maxCharsPerLine <= 0) return listOf(str)

            str.split('\n').forEach { paragraph ->
                if (paragraph.isEmpty()) {
                    result.add("")
                    return@forEach
                }

                val words = paragraph.split(' ')
                val currentLine = StringBuilder()

                words.forEach { word ->
                    if (currentLine.isEmpty()) {
                        currentLine.append(word)
                    } else if (currentLine.length + 1 + word.length <= maxCharsPerLine) {
                        currentLine.append(' ').append(word)
                    } else {
                        result.add(currentLine.toString())
                        currentLine.clear()
                        currentLine.append(word)
                    }
                }

                if (currentLine.isNotEmpty()) {
                    result.add(currentLine.toString())
                }
            }

            if (result.isEmpty()) {
                result.add("")
            }

            return result
        }

        val Char.hasAccent: Boolean
            get() = this.isLetter() && this.lowercaseChar() !in 'a'..'z'

        val ACCENT_MAP =
            mapOf(
                'à' to 'a', 'á' to 'a', 'â' to 'a', 'ã' to 'a', 'ä' to 'a', 'å' to 'a',
                'ç' to 'c',
                'è' to 'e', 'é' to 'e', 'ê' to 'e', 'ë' to 'e',
                'ì' to 'i', 'í' to 'i', 'î' to 'i', 'ï' to 'i',
                'ñ' to 'n',
                'ò' to 'o', 'ó' to 'o', 'ô' to 'o', 'õ' to 'o', 'ö' to 'o',
                'ù' to 'u', 'ú' to 'u', 'û' to 'u', 'ü' to 'u',
                'ý' to 'y', 'ÿ' to 'y',
            )

        val EMOJI_MAP =
            mapOf(
                '⚠' to (0 to 0),
            )
    }
}
