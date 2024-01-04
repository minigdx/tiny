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

//language=Lua
const val SHAPE_OVALF_EXAMPLE = """
function _draw()
    gfx.cls()

    local pos = ctrl.touch()
    local w = math.max(0, pos.x)
    local h = math.max(0, pos.y)
   
    shape.ovalf(w, h, w, h, 5)
    shape.oval(256 - w, 256 - h, w, h, 1)
    

    print("size w: "..w.." h: "..h)
end
"""

//language=Lua
const val SHAPE_GRADIENT_EXAMPLE = """
function _draw()
    
 local c1 = 2
 local c2 = 3   

    gfx.cls(c1)
    shape.rectf(0, 256 - 16, 256, 16, c2)
    for x=0,240,16 do
        shape.gradient(x, 16 * math.cos(2 * 3.14 * (x / 256) + tiny.t * 2), 16, 256, c1, c2, false)
    end
      
end
"""

//language=Lua
const val SHAPE_LINE_EXAMPLE = """
function _draw()
  gfx.cls()
  
  local i =0
  for x =16, 240, 16 do
    for y =16, 240, 16 do
      shape.line(x, y, 256 - x, 256 - y, i)
      i = i + 1
    end
  end
end
"""

//language=Lua
const val SHAPE_TRIANGLEF_EXAMPLE = """
function tri(f, fill)
  local x1 = 128 + math.cos(f) * 64
  local y1 = 128 + math.sin(f) * 64

  local x2 = 128 + math.cos(f + math.pi * 1/3) * 64
  local y2 = 128 + math.sin(f + math.pi * 1/3) * 64

  local x3 = 128 + math.cos(f+ math.pi * 2/3) * 64
  local y3 = 128 + math.sin(f +math.pi * 2/3) * 64

  if fill then
    shape.trianglef(x3, y3, x2, y2, x1, y1, 8)
  else
    shape.triangle(x1, y1, x2, y2, x3, y3, 8)
  end
end

function _draw()
  gfx.cls()
  local f = tiny.frame * 0.01

  tri(f, false)
  tri(f*2, true)
end
"""
