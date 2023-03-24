
function init()
    index = 0
    dt = 0
end
function draw()
    cls(2)
    table.sort({1, 2, 3})
     map.draw()
    -- toto.draw()
    -- debug.traceback(cls(1))
end

function _resources()
    print("LOADED")
end
