package com.github.minigdx.tiny.lua

//language=Lua
const val GFX_PAL_EXAMPLE = """
function _draw()
    cls()
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
    cls()
    gfx.dither()
    shp.circlef(30, 30, 30, 2)
    gfx.dither(0xA5A5) -- set a dithering pattern
    shp.circlef(50, 50, 30, 3)
    gfx.dither(0x0842) -- set another dithering pattern
    shp.circlef(70, 70, 30, 2)
end
"""

//language=Lua
const val GFX_CLIP_EXAMPLE = """
function _draw()
  cls()
  -- set a clip area to crop circles
  gfx.clip(20, 20, 80, 80)
  shp.circlef(20, 20, 20, 2)
  shp.circlef(100, 20, 20, 2)

  -- reset the clip area
  gfx.clip()
  shp.circlef(20, 20, 10, 3)
  shp.circlef(100, 20, 10, 3)

end
"""
