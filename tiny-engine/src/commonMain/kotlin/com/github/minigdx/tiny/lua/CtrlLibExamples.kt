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

const val CTRL_TOUCHING_EXAMPLE = """
function _init()
   t = {}
end

function _update()
    local touch = ctrl.touching(0)
    if(touch) then
       table.insert(t, {str = " touching "..touch.x.."-"..touch.y, ttl = 3})
    end

for i,s in rpairs(t) do
    s.ttl = s.ttl - 1/60
    if s.ttl < 0 then
      table.remove(t, i)
    end
end
end

function _draw()
    gfx.cls()

    local p = 0
    for i, s in rpairs(t) do
      print(i..s.str, 10, 6 * p, 2)
      p = p + 1
    end

    local touch = ctrl.touch()
    shape.circlef(touch.x, touch.y, 2, 8)
end
"""
