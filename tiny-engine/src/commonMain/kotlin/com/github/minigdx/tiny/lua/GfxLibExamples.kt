package com.github.minigdx.tiny.lua

//language=Lua
const val GFX_PAL_EXAMPLE = """
function _draw()
    cls()
    print("example", 10, 10, 2) -- print using the color index 2
    gfx.pal(2, 3) -- switch the text color to another color
    print("example", 10, 20, 2) -- print using the color index 2
    gfx.pal() -- reset the palette
    print("example", 10, 30, 2) -- print using the color index 2
end
"""
