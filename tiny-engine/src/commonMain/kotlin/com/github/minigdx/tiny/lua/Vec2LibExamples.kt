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

//language=Lua
const val VECTOR_DOT = """
function _init()
    angle = 0
end
function _update()

    if ctrl.pressing(keys.space) then
        angle = angle + 0.1
    end

    gfx.camera(-128, -128)

    local v1 = vec2.create(1, 0)
    local v2 = vec2.create(math.cos(angle), math.sin(angle))
    local dot = vec2.dot(v1, v2)
    
    local scl = vec2.scl(v1, dot)
    
    gfx.cls()
    
    local scaledv1 = vec2.scl(v1, 64)
    local scaledv2 = vec2.scl(v2, 64)
    local scaledv3 = vec2.scl(scl, 64)

    print("v1", 18, 5, 10)
    shape.line(0, 0, scaledv1.x, scaledv1.y, 10)
    print("v2", scaledv2.x + 5, scaledv2.y + 5, 11)
    shape.line(0, 0, scaledv2.x, scaledv2.y, 11)
    
    print("dot", scaledv3.x, 5, 9)
    shape.line(0, 0, scaledv3.x, scaledv3.y, 9)
end    
"""

//language=Lua
const val VECTOR_NOR = """
function _update()
    gfx.cls()

    local v0 = vec2.create(43, 64)
    local v1 = vec2.nor(v0)
    
    
    print("vector     x: " .. v0.x .. " y: " .. v0.y)
    print("normalized x: " .. v1.x .. " y: " .. v1.y, 0, 8)
end 
"""
