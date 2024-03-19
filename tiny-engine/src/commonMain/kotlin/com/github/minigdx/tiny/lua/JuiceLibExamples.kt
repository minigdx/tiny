package com.github.minigdx.tiny.lua

//language=Lua
const val JUICE_EXAMPLE = """
local center_x = 256 * 0.5
local center_y = 256 * 0.5
local width = 128

function _update()
    gfx.cls()
    shape.line(center_x - 64, center_y + 64, center_x + 64, center_y + 64, 2)
    shape.line(center_x + 64, center_y - 64, center_x + 64, center_y + 64, 2)

    
    for x = 0, width, 2 do
        local y = juice.##function##(0, 128, x / width)
        gfx.pset(
            center_x - 64 + x, center_y + 64 - y, 3
        )
    end

    local percent = (tiny.frame % 100) / 100
    local x = width * percent
    local y = juice.##function##(0, 128, percent)
    shape.circlef(center_x - 64 + x, center_y + 64 - y, 4, 7)
    shape.rectf(center_x - 64 + x - 2, center_y + 64 + 8, 4, 4, 7)
    shape.rectf(center_x + 70, center_y + 64 - y, 4, 4, 7)
    local name = "##function##"
    print(name, center_x - #name * 4 * 0.5, center_y + 92)
end

"""
