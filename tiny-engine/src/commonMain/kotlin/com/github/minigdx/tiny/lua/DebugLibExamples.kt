package com.github.minigdx.tiny.lua

//language=Lua
const val DEBUG_EXAMPLE = """
function _update()
    console.log("hello from the console")
    console.log("Log a value: ", tiny.frame)
    console.log("Log a table: ", ctrl.touch())
end
  
function _draw()
    gfx.cls()
    -- draw the mouse position.
    local pos = ctrl.touch()

    shape.line(pos.x - 2, pos.y, pos.x + 2, pos.y, 3)
    shape.line(pos.x, pos.y - 2, pos.x, pos.y + 2, 3) 
end
"""
