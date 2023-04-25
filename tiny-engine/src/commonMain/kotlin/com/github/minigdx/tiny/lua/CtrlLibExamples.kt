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
