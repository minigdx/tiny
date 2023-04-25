package com.github.minigdx.tiny.lua

//language=Lua
const val SHAPE_RECTF_EXAMPLE = """
function _draw()
  gfx.cls()

  -- filled rectangle
  print("filled rectangle", 20, 10)
  shape.rectf(20, 20, 20, 20, 4)
  shape.rectf(30, 30, 20, 20, 5)
  shape.rectf(40, 40, 20, 20, 6)

  print("non filled rectangle", 20, 65)
  -- non filled rectangle
  shape.rect(50, 70, 20, 20, 7)
  shape.rect(60, 80, 20, 20, 8)
  shape.rect(70, 90, 20, 20, 9)

  print("rectangle with different width", 20, 115)
  shape.rect(20, 120, 30, 20, 10)
  shape.rect(20, 140, 40, 20, 12)
  shape.rect(20, 160, 60, 20, 13)

end
"""

//language=Lua
const val SHAPE_CIRCLEF_EXAMPLE = """
function _draw()
    gfx.cls()

    -- filled circle
    shape.circlef(20, 20, 20, 4)
    shape.circlef(30, 30, 20, 5)
    shape.circlef(40, 40, 20, 6)
    print("filled circle", 20, 10)

    -- non filled circle
    shape.circle(50, 70, 10, 7)
    shape.circle(60, 80, 10, 8)
    shape.circle(70, 90, 10, 9)
    print("non filled circle", 20, 65)

    shape.circle(80, 120, 15, 10)
    shape.circle(80, 140, 20, 16)
    shape.circle(80, 160, 30, 14)
    print("circle with different radius", 20, 115)

end
"""
