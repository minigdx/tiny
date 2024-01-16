package com.github.minigdx.tiny.lua

//language=Lua
const val MATH_PERLIN_EXAMPLE = """
function _draw()
    gfx.cls(8)

    gfx.dither(0x0001)
    local x = math.perlin(0.1, 0.2, tiny.frame / 100)
    local y = math.perlin(0.4, 0.5, tiny.frame / 100)
    shape.circlef(x * 256, y * 256, 64, 7)
end
"""

//language=Lua
const val MATH_RND_EXAMPLE = """
local value = {}
function _update()
    table.insert(value, math.rnd(126))

    if(#value > 50) then
        table.remove(value, #value)
    end
end

function _draw()
    gfx.cls()
    local y = 0
    for v in all(value) do
        print("rnd: "..v, 4, y)
        y = y + 6
    end
end
"""

//language=Lua
const val MATH_SIGN_EXAMPLE = """
function _draw()
    gfx.cls()
    local cos = math.cos(tiny.t)
    print("cos: "..cos)
    print("sign: "..math.sign(cos), 0, 8)

    shape.line(128, 128, 128 + cos * 128, 128, 9)
end
"""
