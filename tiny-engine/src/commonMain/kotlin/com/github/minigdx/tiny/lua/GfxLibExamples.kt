package com.github.minigdx.tiny.lua

//language=Lua
const val GFX_PAL_EXAMPLE = """
function _draw()
    gfx.cls()
    print("example", 10, 10, 2) -- print using the color index 2
    gfx.pal(2, 3) -- switch the text color to another color
    print("example", 10, 20, 2) -- print using the color index 2
    gfx.pal() -- reset the palette
    print("example", 10, 30, 2) -- print using the color index 2
end
"""
//language=Lua
const val GFX_DITHER_EXAMPLE = """
function _draw()
    gfx.cls()
    gfx.dither()
    shape.circlef(30, 30, 30, 2)
    gfx.dither(0xA5A5) -- set a dithering pattern
    shape.circlef(50, 50, 30, 3)
    gfx.dither(0x0842) -- set another dithering pattern
    shape.circlef(70, 70, 30, 2)
end
"""

//language=Lua
const val GFX_CLIP_EXAMPLE = """
function _init()
  c = {}
  for i=1,100 do
    table.insert(c, {x=math.rnd(256), y=math.rnd(256), c = math.rnd(1,12)})
end

end
function _draw()
  gfx.cls()
  local pos = ctrl.touch()
  -- set a clip area to crop circles
  gfx.clip(pos.x - 20, pos.y - 20, 40, 40)
  for circle in all(c) do
    shape.circlef(circle.x, circle.y, 10, circle.c)
  end
end
"""

//language=Lua
const val GFX_TO_SHEET_EXAMPLE = """
function _draw()
    gfx.cls(1)
    -- draw a transparent circle (like a hole)
    shape.circlef(64, 128, 20, 0)
    -- keep the result as spritesheet 0
    gfx.to_sheet(0)

    gfx.cls(1)
    -- draw some circles
    shape.circlef(64, 108, 20, 8)
    shape.circlef(44, 128, 20, 9)
    shape.circlef(64, 148, 20, 10)
    shape.circlef(84, 128, 20, 11)

    -- draw over the circles
    -- the mask generated before.
    spr.sheet(0)
    spr.sdraw()
end"""
