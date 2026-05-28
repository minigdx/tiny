package com.github.minigdx.tiny.lua

//language=Lua
const val FULLSCREEN_EXAMPLE = """
local message = "hit the key f to toggle fullscreen"

function _draw()

    if ctrl.pressed(keys.f) then
        tiny.fullscreen()
    end
    gfx.cls()
    
    local x = 128 - text.width(message) * 0.5
    text.print(message, x, 128)
end
"""
