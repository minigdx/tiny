package com.github.minigdx.tiny.lua

//language=Lua
const val JUICE_EXAMPLE = """
function _init()
  t = 0
end

function _update()
  t = t + 1/60
end

function _draw()
  gfx.cls()
  progress = math.abs(math.cos(t))

  print("pow", 0, 0) 
  shape.circlef(juice.pow2(20, 236, progress), 10, 10, 2)
  shape.circlef(juice.pow3(20, 236, progress), 20, 10, 3)
  shape.circlef(juice.pow4(20, 236, progress), 30, 10, 4)


  print("bounce", 0, 50) 
  shape.circlef(juice.bounce(20, 236, progress), 60, 10, 5)

  print("exp", 0, 80) 
  shape.circlef(juice.exp10(20, 236, progress), 90, 10, 6)


  print("swing", 0, 110) 
  shape.circlef(juice.swing(20, 236, progress), 120, 10, 7)

  print("sine", 0, 140) 
  shape.circlef(juice.sine(20, 236, progress), 150, 10, 8)

  print("circle", 0, 170) 
  shape.circlef(juice.circle(20, 236, progress), 180, 10, 9)

  print("elastic", 0, 200) 
  shape.circlef(juice.elastic(20, 236, progress), 210, 10, 10)

  print("linear", 0, 230) 
  shape.circlef(juice.linear(20, 236, progress), 240, 10, 1)
end
"""
