
function _init()
  dt = 0
  action = false
end

function _update()
  dt = dt + 1/60

end


function _draw()
  cls(2)
  progress = abs(cos(dt))

  pos = ctrl.touch()
  print(pos.x .. "x" .. pos.y, 0, 0, 5)
  if(not action and pos.x > 126) then
    debug("test soun2rrd")
    action = true
  end
  --print(pos.x .. "x" .. pos.y, 0, 0, 2)
end
