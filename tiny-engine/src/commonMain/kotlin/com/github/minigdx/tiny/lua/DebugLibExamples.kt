package com.github.minigdx.tiny.lua

//language=Lua
const val DEBUG_EXAMPLE = """
function _update()
    local pos = ctrl.touch()

    debug.table(pos)
    debug.log("frame "..tiny.frame)
end
  
function _draw()
    gfx.cls()
    -- draw the mouse position.
    local pos = ctrl.touch()

    shape.line(pos.x - 2, pos.y, pos.x + 2, pos.y, 3)
    shape.line(pos.x, pos.y - 2, pos.x, pos.y + 2, 3) 
end
"""

//language=Lua
const val DEBUG_ENABLED_EXAMPLE = """
function _init()
   switch = true
end

function _update()
    local pos = ctrl.touch()

    debug.rect(pos.x, pos.y, 16, 16)

    if ctrl.touched(0) then
       switch = not switch
    
       debug.enabled(switch)
    end   

    debug.log("debug ".. tostring(switch))
end
  
function _draw()
    gfx.cls()
    -- draw the mouse position.
    local pos = ctrl.touch()

    shape.line(pos.x - 2, pos.y, pos.x + 2, pos.y, 3)
    shape.line(pos.x, pos.y - 2, pos.x, pos.y + 2, 3) 

    if switch then
      print("debug enabled", 10, 40)
    else
     print("debug disabled", 10, 40)
    end
end
"""
