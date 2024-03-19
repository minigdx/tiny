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

//language=Lua
const val GFX_PSET_EXAMPLE = """
function _draw()
   local pos = ctrl.touching(0)
   if pos ~= nil then
      -- set the pixel with the color 9 when the mouse is pressed
      gfx.pset(pos.x, pos.y, 9)
   end
end"""

//language=Lua
const val GFX_PGET_EXAMPLE = """
function _draw()
   gfx.cls()
   local index = 0
   for x=0, 240, 16 do
     for y=0, 240, 16 do
        shape.rectf(x, y, 16, 16, index)
        index = index + 1
     end
   end

   local pos = ctrl.touch()
   local color = gfx.pget(pos.x, pos.y)
   if color ~= nil then 
     shape.rectf(0, 0, 80, 6, 13)
     print("color index: "..color)
   end


   shape.circlef(pos.x - 2, pos.y - 2, 4, 0)
end"""

//language=Lua
const val GFX_CLS_EXAMPLE = """
function _draw()
    if ctrl.pressed(keys.space) then
       gfx.cls()
    end

    print("Press space to clear the screen") 
    local pos = ctrl.touch()
    shape.circlef(pos.x, pos.y, 4, math.rnd())
end"""

//language=Lua
const val GFX_CAMERA_EXAMPLE = """
local x = 0
local y = 0

function _update()
    if ctrl.pressing(keys.left) then
        x = x - 0.5
    elseif ctrl.pressing(keys.right) then
        x = x + 0.5
    end

    if ctrl.pressing(keys.up) then
        y = y - 0.5
    elseif ctrl.pressing(keys.down) then
        y = y + 0.5
    end
    gfx.camera(math.floor(x), math.floor(y))
end

function _draw()
    gfx.cls(2)
    for x = 0 - 64, 256 + 64, 16 do
        for y = 0 - 64, 256 + 64, 16 do
            shape.line(x - 2, y, x + 2, y, 9)
            shape.line(x, y - 2, x, y + 2, 9)
        end
    end
    print("camera: ("..x..", "..y..")", 6, 6)
    
    shape.rect(0, 0, 256, 256, 1)
end"""
