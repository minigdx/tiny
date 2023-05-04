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
function _init()
    circle = {
        x = 256 * 0.5,
        y = 256 * 0.5,
        radius = 10
    }
end

function _update()
    -- check keys for horizontal move
    if (ctrl.pressing(keys.left)) then
        circle.x = math.max(circle.x - 1, 0)
    elseif (ctrl.pressing(keys.right)) then
        circle.x = math.min(circle.x + 1, 256)
    end

    -- check keys for vertical move
    if (ctrl.pressing(keys.up)) then
        circle.y = math.max(circle.y - 1, 0)
    elseif (ctrl.pressing(keys.down)) then
        circle.y = math.min(circle.y + 1, 256)
    end

    -- check keys for update circle size
    if (ctrl.pressing(keys.space)) then
        circle.radius = math.min(circle.radius + 1, 256)
    elseif (ctrl.pressing(keys.enter)) then
        circle.radius = math.max(circle.radius - 1, 0)
    end
end

function _draw()
    gfx.cls(1)
    shape.circlef(circle.x, circle.y, circle.radius, 8)
    shape.circle(circle.x, circle.y, 2, 9)
end
"""
