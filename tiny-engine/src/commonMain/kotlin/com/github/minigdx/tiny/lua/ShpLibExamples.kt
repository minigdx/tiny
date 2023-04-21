package com.github.minigdx.tiny.lua

//language=Lua
const val SHP_RECTF_EXAMPLE = """
function _draw()
  cls()

  -- filled rectangle
  print("filled rectangle", 20, 10)
  shp.rectf(20, 20, 20, 20, 4)
  shp.rectf(30, 30, 20, 20, 5)
  shp.rectf(40, 40, 20, 20, 6)

  print("non filled rectangle", 20, 65)
  -- non filled rectangle
  shp.rect(50, 70, 20, 20, 7)
  shp.rect(60, 80, 20, 20, 8)
  shp.rect(70, 90, 20, 20, 9)

  print("rectangle with different width", 20, 115)
  shp.rect(20, 120, 30, 20, 10)
  shp.rect(20, 140, 40, 20, 12)
  shp.rect(20, 160, 60, 20, 13)

end
"""

//language=Lua
const val SHP_CIRCLEF_EXAMPLE = """
function _draw()
    cls()

    -- filled circle
    shp.circlef(20, 20, 20, 4)
    shp.circlef(30, 30, 20, 5)
    shp.circlef(40, 40, 20, 6)
    print("filled circle", 20, 10)

    -- non filled circle
    shp.circle(50, 70, 10, 7)
    shp.circle(60, 80, 10, 8)
    shp.circle(70, 90, 10, 9)
    print("non filled circle", 20, 65)

    shp.circle(80, 120, 15, 10)
    shp.circle(80, 140, 20, 16)
    shp.circle(80, 160, 30, 14)
    print("circle with different radius", 20, 115)

end
"""
