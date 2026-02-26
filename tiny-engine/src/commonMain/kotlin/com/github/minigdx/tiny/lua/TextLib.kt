package com.github.minigdx.tiny.lua

import com.github.mingdx.tiny.doc.LuaType
import com.github.mingdx.tiny.doc.TinyArg
import com.github.mingdx.tiny.doc.TinyCall
import com.github.mingdx.tiny.doc.TinyFunction
import com.github.mingdx.tiny.doc.TinyLib
import com.github.minigdx.tiny.engine.FontDescriptor
import com.github.minigdx.tiny.engine.GameOptions
import com.github.minigdx.tiny.engine.GameResourceAccess
import com.github.minigdx.tiny.engine.forEachCodepoint
import com.github.minigdx.tiny.render.VirtualFrameBuffer
import org.luaj.vm2.LuaTable
import org.luaj.vm2.LuaValue
import org.luaj.vm2.Varargs
import org.luaj.vm2.lib.TwoArgFunction
import org.luaj.vm2.lib.VarArgFunction
import kotlin.math.max

@TinyLib(
    "text",
    "Text rendering library for custom fonts. " +
        "Allows selecting fonts configured in `_tiny.json` and rendering text with them. " +
        "When no font is selected, uses the default boot font (same as `print()`).",
)
class TextLib(
    private val gameOptions: GameOptions,
    private val resourceAccess: GameResourceAccess,
    private val virtualFrameBuffer: VirtualFrameBuffer,
) : TwoArgFunction() {
    private var currentFontIndex: Int? = null

    override fun call(
        arg1: LuaValue,
        arg2: LuaValue,
    ): LuaValue {
        val func = LuaTable()
        func["font"] = font()
        func["print"] = print()
        func["width"] = width()

        arg2["text"] = func
        arg2["package"]["loaded"]["text"] = func
        return func
    }

    private fun currentFont(): FontDescriptor? {
        val index = currentFontIndex ?: return null
        return gameOptions.fonts.getOrNull(index)
    }

    @TinyFunction(
        "Select a font to use for text rendering. " +
            "Call without arguments to reset to the default boot font.",
        example = TEXT_FONT_EXAMPLE,
    )
    inner class font : VarArgFunction() {
        @TinyCall("Reset to the default boot font.")
        override fun call(): LuaValue {
            currentFontIndex = null
            return NIL
        }

        @TinyCall("Select a font by index.")
        override fun call(
            @TinyArg("index", type = LuaType.NUMBER) arg: LuaValue,
        ): LuaValue {
            if (arg.isnil()) {
                currentFontIndex = null
            } else if (arg.isnumber()) {
                currentFontIndex = arg.checkint()
            } else {
                val name = arg.checkjstring()!!
                val index = gameOptions.fonts.indexOfFirst { it.name == name }
                if (index >= 0) {
                    currentFontIndex = index
                }
            }
            return NIL
        }

        override fun invoke(args: Varargs): Varargs {
            if (args.narg() == 0 || args.isnil(1)) {
                currentFontIndex = null
            } else if (args.isnumber(1)) {
                currentFontIndex = args.checkint(1)
            } else {
                val name = args.checkjstring(1)!!
                val index = gameOptions.fonts.indexOfFirst { it.name == name }
                if (index >= 0) {
                    currentFontIndex = index
                }
            }
            return NIL
        }
    }

    @TinyFunction(
        "Print text on the screen using the currently selected font.",
        example = TEXT_PRINT_EXAMPLE,
    )
    inner class print : VarArgFunction() {
        @TinyCall("Print text at the given position with an optional color.")
        override fun invoke(args: Varargs): Varargs {
            val str = args.checkjstring(1) ?: return NIL
            val x = args.optint(2, 0)
            val y = args.optint(3, 0)

            val color = if (args.narg() >= 4 && !args.isnil(4)) {
                checkColorIndex(args.arg(4))
            } else {
                gameOptions.colors().getColorIndex("#FFFFFF")
            }

            val font = currentFont()
            if (font != null) {
                renderCustomFont(str, x, y, color, font)
            } else {
                renderBootFont(str, x, y, color)
            }

            return NIL
        }
    }

    @TinyFunction(
        "Measure the width in pixels of a string using the currently selected font.",
        example = TEXT_WIDTH_EXAMPLE,
    )
    inner class width : VarArgFunction() {
        @TinyCall("Returns the width in pixels of the text.")
        override fun invoke(
            @TinyArg("str", type = LuaType.STRING) args: Varargs,
        ): Varargs {
            val str = args.checkjstring(1) ?: return valueOf(0)
            val font = currentFont()

            var maxWidth = 0
            var currentWidth = 0

            if (font != null) {
                str.forEachCodepoint { codepoint ->
                    if (codepoint == '\n'.code) {
                        maxWidth = max(maxWidth, currentWidth)
                        currentWidth = 0
                    } else if (codepoint == ' '.code) {
                        currentWidth += font.spaceWidth
                    } else {
                        currentWidth += font.resolve(codepoint)?.charWidth ?: 0
                    }
                }
            } else {
                str.forEachCodepoint { codepoint ->
                    if (codepoint == '\n'.code) {
                        maxWidth = max(maxWidth, currentWidth)
                        currentWidth = 0
                    } else if (codepoint == ' '.code) {
                        currentWidth += BOOT_SPACE_WIDTH
                    } else {
                        currentWidth += BOOT_CHAR_WIDTH
                    }
                }
            }
            maxWidth = max(maxWidth, currentWidth)

            return valueOf(maxWidth)
        }
    }

    private fun renderCustomFont(
        str: String,
        x: Int,
        y: Int,
        color: Int,
        font: FontDescriptor,
    ) {
        val spritesheet = resourceAccess.findFontSpritesheet(currentFontIndex!!) ?: return
        var currentX = x
        var currentY = y

        str.forEachCodepoint { codepoint ->
            when (codepoint) {
                '\n'.code -> {
                    currentY += font.lineHeight
                    currentX = x
                }
                ' '.code -> {
                    currentX += font.spaceWidth
                }
                else -> {
                    val resolved = font.resolve(codepoint)
                    if (resolved != null) {
                        virtualFrameBuffer.drawMonocolor(
                            spritesheet,
                            color,
                            resolved.sourceX,
                            resolved.sourceY,
                            resolved.charWidth,
                            resolved.charHeight,
                            currentX,
                            currentY,
                            flipX = false,
                            flipY = false,
                        )
                        currentX += resolved.charWidth
                    }
                }
            }
        }
    }

    private fun renderBootFont(
        str: String,
        x: Int,
        y: Int,
        color: Int,
    ) {
        val spritesheet = resourceAccess.bootSpritesheet ?: return
        var currentX = x
        var currentY = y

        str.forEach { char ->
            val coord = resolveBootChar(char)
            if (coord != null) {
                virtualFrameBuffer.drawMonocolor(
                    spritesheet,
                    color,
                    coord.first * BOOT_CHAR_WIDTH,
                    coord.second * BOOT_CHAR_HEIGHT,
                    BOOT_CHAR_WIDTH,
                    BOOT_CHAR_HEIGHT,
                    currentX,
                    currentY,
                    flipX = false,
                    flipY = false,
                )
            } else if (char == '\n') {
                currentY += BOOT_LINE_HEIGHT
                currentX = x - BOOT_SPACE_WIDTH
            }
            currentX += BOOT_SPACE_WIDTH
        }
    }

    private fun resolveBootChar(char: Char): Pair<Int, Int>? {
        return if (char.isLetter()) {
            val l = if (char.hasAccent) {
                StdLib.ACCENT_MAP[char.lowercaseChar()] ?: char.lowercaseChar()
            } else {
                char.lowercaseChar()
            }
            val index = l - 'a'
            index to 0
        } else if (char.isDigit()) {
            val index = char - '0'
            index to 1
        } else if (char in '!'..'/') {
            val index = char - '!'
            index to 2
        } else if (char in '['..'`') {
            val index = char - '['
            index to 3
        } else if (char in '{'..'~') {
            val index = char - '{'
            index to 4
        } else if (char in ':'..'@') {
            val index = char - ':'
            index to 5
        } else {
            StdLib.EMOJI_MAP[char]
        }
    }

    private val Char.hasAccent: Boolean
        get() = this.isLetter() && this.lowercaseChar() !in 'a'..'z'

    private fun checkColorIndex(value: LuaValue): Int {
        val colors = gameOptions.colors()
        return if (value.isnumber()) {
            colors.check(value.checkint())
        } else {
            colors.getColorIndex(value.checkjstring()!!)
        }
    }

    companion object {
        private const val BOOT_CHAR_WIDTH = 4
        private const val BOOT_CHAR_HEIGHT = 4
        private const val BOOT_SPACE_WIDTH = 4
        private const val BOOT_LINE_HEIGHT = 6

        private val bootCharMap: Map<Int, Pair<Int, Int>> by lazy {
            val map = mutableMapOf<Int, Pair<Int, Int>>()
            for (c in 'a'..'z') {
                map[c.code] = (c - 'a') to 0
            }
            for (c in '0'..'9') {
                map[c.code] = (c - '0') to 1
            }
            for (c in '!'..'/') {
                map[c.code] = (c - '!') to 2
            }
            for (c in '['..'`') {
                map[c.code] = (c - '[') to 3
            }
            for (c in '{'..'~') {
                map[c.code] = (c - '{') to 4
            }
            for (c in ':'..'@') {
                map[c.code] = (c - ':') to 5
            }
            map
        }
    }
}
