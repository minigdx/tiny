package com.github.minigdx.tiny.lua

//language=Lua
const val CTRL_TOUCH_EXAMPLE = """
function _draw()
  gfx.cls(2)
  p = ctrl.touch()
  print("coordinates: "..p.x .. "x"..p.y, 1, 1, 4)
  shape.rectf(p.x, p.y, 5,5, p.x + p.y)
end
"""

//language=Lua
const val CTRL_PRESSING_EXAMPLE = """
local percent_a = 1
local percent_b = 1

function _update()
    percent_a = math.min(percent_a + 0.05, 1)
    percent_b = math.min(percent_b + 0.05, 1)

    if ctrl.pressed(keys.space) then
        percent_a = 0
    end
    
    if ctrl.pressing(keys.space) then
        percent_b = 0
    end

    local offset_a = juice.powIn2(0, 8, percent_a)
    local offset_b = juice.powIn2(0, 8, percent_b)

    gfx.cls()
    shape.rectf(64, 128 - 16, 32, 32, 7)
    shape.rectf(64, 128 - 32 + offset_a, 32, 32, 8)

    shape.rectf(32 + 128, 128 - 16, 32, 32, 7)
    shape.rectf(32 + 128, 128 - 32 + offset_b, 32, 32, 8)

    print("pressed", 64, 128 + 32)
    print("pressing", 32 + 128, 128 + 32)
end
"""

const val CTRL_TOUCHING_EXAMPLE = """
function _draw()
    gfx.cls()
    local start = ctrl.touching(0)
    if start ~= nil then
        local pos = ctrl.touch()
        shape.line(start.x, start.y, pos.x, pos.y, 9)
        print("("..start.x .. ", "..start.y..")", start.x, start.y)
        print("("..pos.x .. ", "..pos.y..")", pos.x, pos.y)
    end
end
"""

const val CTRL_TOUCHED_EXAMPLE = """
function _draw()
    local pos = ctrl.touched(0)
    if pos ~= nil then
        shape.circlef(pos.x, pos.y, 4, 9)
        print("("..pos.x .. ", "..pos.y..")", pos.x + 3, pos.y + 3)
    end
end
"""
