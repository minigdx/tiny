package com.github.minigdx.tiny.lua

//language=Lua
const val STD_PRINT_EXAMPLE = """
function _draw()
    cls()
    -- every character is a sprite 4x4 pixels.
    print("hello")
    print("world", 10, 10)
    print("how", 10, 20, 4)
    print("are", 26, 20, 5)
    print("you", 42, 20, 6)
    print("...", 58, 20, math.rnd(10))
end
"""
