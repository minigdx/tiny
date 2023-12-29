package com.github.minigdx.tiny.lua

//language=Lua
const val VECTOR2_ADD = """
function _init()
    gfx.camera(-128, -128)

    local v1 = vec2.create(32, 38)
    local v2 = vec2.create(20, 2)

    gfx.cls()
    print("v1", 2, 15, 10)
    shape.line(0, 0, v1.x, v1.y, 10)
    print("v2", 23, 3, 9)
    shape.line(0, 0, v2.x, v2.y, 9)

    local v3 = vec2.add(v1, v2)

    print("v1 + v2", 30, 15, 11)
    gfx.dither(0xAAAA)
    shape.line(0, 0, v3.x, v3.y, 11)
    gfx.dither()
end
"""

//language=Lua
const val VECTOR2_SUB = """
function _init()
    gfx.camera(-128, -128)

    local v1 = vec2.create(32, 38)
    local v2 = vec2.create(20, 2)

    gfx.cls()
    print("v1", 18, 15, 10)
    shape.line(0, 0, v1.x, v1.y, 10)
    print("v2", 23, 3, 9)
    shape.line(0, 0, v2.x, v2.y, 9)

    local v3 = vec2.sub(v1, v2)

    print("v1 - v2", 0, 40, 11)
    gfx.dither(0xAAAA)
    shape.line(0, 0, v3.x, v3.y, 11)
    gfx.dither()
end
"""

//language=Lua
const val VECTOR2_SCL = """
function _init()
    gfx.camera(-128, -128)

    local v1 = vec2.create(32, 38)
    local v2 = vec2.scl(v1, 2)

    gfx.cls()
    print("v1 scaled", 8, 60, 11)
    gfx.dither(0xAAAA)
    shape.line(0, 0, v2.x, v2.y, 11)
    gfx.dither()
    print("v1", 18, 15, 10)
    shape.line(0, 0, v1.x, v1.y, 10)
end
"""
