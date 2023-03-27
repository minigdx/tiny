
function init()
    index = 0
    dt = 0
    pos = {x= 64, y= 64}
end

function update()
    if(ctrl.down(0)) then
        pos.x = pos.x - 1
    elseif (ctrl.down(2)) then
        pos.x = pos.x + 1
    end

    if(ctrl.down(1)) then
        pos.y = pos.y - 1
    elseif (ctrl.down(3)) then
        pos.y = pos.y + 1
    end

end
function draw()
    dt = dt + 1/60
    cls(2)
    table.sort({1, 2, 3})
    w =  128 * (1 + cos(dt)) * 0.5
     map.draw(0 + 64 - w * 0.5, 0, 64 - w * 0.5, 0, w, 128)
    -- toto.draw()
    -- debug.traceback(cls(1))
    spr(4, pos.x, pos.y)
end

function _resources()
    print("LOADED")
end
