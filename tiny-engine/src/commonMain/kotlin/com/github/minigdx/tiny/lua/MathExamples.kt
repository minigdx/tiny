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

//language=Lua
const val MATH_DST_EXAMPLE = """
function _draw()
    gfx.cls()
    
    local pos = ctrl.touch()
    shape.line(pos.x, pos.y, 128, 128, 3)
    shape.circlef(128, 128, 2, 9)
    shape.circlef(pos.x, pos.y, 2, 9)

    -- midle of the line
    local x = (pos.x - 128) * 0.5
    local y = (pos.y - 128) * 0.5
    
    -- display dst
    local dst = math.dst(128, 128, pos.x, pos.y)
    print("dst: "..dst, 128 + x, 128 + y)
end
"""

//language=Lua
const val MATH_DST2_EXAMPLE = """
local a = {
    x = 10 + math.rnd(236),
    y = 10 + math.rnd(236)
}

local b = {
    x = 10 + math.rnd(236),
    y = 10 + math.rnd(236)
}

function _draw()
    gfx.cls()
    
    local pos = ctrl.touch()

    if math.dst2(pos.x, pos.y, a.x, a.y) > math.dst2(pos.x, pos.y, b.x, b.y) then
        -- b is closer
        shape.line(pos.x, pos.y, b.x, b.y, 3)
    else
        -- a is closer
        shape.line(pos.x, pos.y, a.x, a.y, 3)
    end
    shape.circlef(a.x, a.y, 2, 9)
    shape.circlef(b.x, b.y, 2, 9)
    shape.circlef(pos.x, pos.y, 2, 9)
end
"""

//language=Lua
const val MATH_CLAMP_EXAMPLE = """
function _draw()
    gfx.cls()
    
    local pos = ctrl.touch()

    local x = math.clamp(60, pos.x, 256 - 60)
    
    gfx.dither(0xA5A5)
    shape.line(64, 129, 256 - 60, 129, 9)
    gfx.dither()
    shape.rect(60, 128, 4, 4, 9)
    shape.rect(256 - 60, 128, 4, 4, 9)
    shape.circlef(pos.x, pos.y, 2, 9)
    shape.circle(x, 129, 2, 8)
end
"""
