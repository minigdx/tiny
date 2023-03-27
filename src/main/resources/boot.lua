
function init()
    index = 0
    dt = 0
end
function draw()
    dt = dt + 1/60
    cls(2)
    table.sort({1, 2, 3})
    w =  128 * (1 + cos(dt)) * 0.5
     map.draw(0 + 64 - w * 0.5, 0, 64 - w * 0.5, 0, w, 128)
    -- toto.draw()
    -- debug.traceback(cls(1))
    spr(4, 64 + cos(dt) * 64, 0, false, cos(dt) > 0)
end

function _resources()
    print("LOADED")
end
