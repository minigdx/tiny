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
//language=Lua
const val GFX_DITHER_EXAMPLE = """
function _draw()
    cls()
    gfx.dither()
    circlef(30, 30, 30, 2)
    gfx.dither(0xA5A5)
    circlef(50, 50, 30, 3)
    gfx.dither(0x0842)
    circlef(70, 70, 30, 2)
end
"""
