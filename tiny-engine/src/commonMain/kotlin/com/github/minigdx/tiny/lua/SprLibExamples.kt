package com.github.minigdx.tiny.lua

//language=Lua
const val SPR_PGET_EXAMPLE = """
function _draw()
    local pos = ctrl.touch()
    
    gfx.cls()
    
    spr.sdraw()
    shape.circlef(pos.x - 4, pos.y - 4, 8, 3)
    local color = spr.pget(pos.x, pos.y)
    if(color ~= nil) then
        print("index color: "..color, 7, 0)
        shape.rectf(0, 0, 6, 6, color)
        shape.circlef(pos.x - 4, pos.y - 4, 6, color)
    end
end
"""

//language=Lua
const val SPR_PSET_EXAMPLE = """
function _draw()
    local pos = ctrl.touch()

    local touching = ctrl.touching(0)
    gfx.cls()

   spr.sdraw()
   
   if touching ~= nil then
      spr.pset(touching.x, touching.y, 9)
   end
   print("click to alter", 45, 96)
   shape.circle(64 + 8, 128 + 8, 32, 1)
   shape.circlef(128 + 8, 128 + 8, 32, 1)
   spr.draw(100, 128, 128)

   shape.circlef(pos.x, pos.y, 2, 3)
end
"""

//language=Lua
const val SPR_DRAW_EXAMPLE = """
function _init()
  id = 1
end

function _draw()
    if ctrl.pressed(keys.left) then
      id = id - 1
    elseif ctrl.pressed(keys.right) then
      id = id + 1
    end

    gfx.cls()
    print("sprite index "..id.. " (press left or right to change)", 50, 112)
    spr.draw(id, 120, 120)
end
"""
