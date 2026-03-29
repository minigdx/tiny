package com.github.minigdx.tiny.lua

//language=Lua
const val TEXT_FONT_EXAMPLE = """
function _draw()
    gfx.cls()
    -- Use the default boot font
    text.font()
    text.print("default font", 10, 10)

    -- Switch to the first custom font (index 0)
    text.font(0)
    text.print("custom font", 10, 30)

    -- Switch to a custom font by name
    text.font("big_font")
    text.print("big font", 10, 50)

    -- Reset to default
    text.font()
    text.print("back to default", 10, 70)
end
"""

//language=Lua
const val TEXT_PRINT_EXAMPLE = """
function _draw()
    gfx.cls()
    -- Print with default color (white)
    text.print("hello world", 10, 10)

    -- Print with a color index
    text.print("colored text", 10, 20, 9)

    -- Print multiline text
    text.print("line 1\nline 2\nline 3", 10, 40)
end
"""

//language=Lua
const val TEXT_WIDTH_EXAMPLE = """
function _draw()
    gfx.cls()
    local msg = "hello"
    local w = text.width(msg)
    -- Center the text on screen
    local x = (256 - w) / 2
    text.print(msg, x, 120)
end
"""
