function init()
    index = 0
    dt = 0
end
function draw()
    dt = dt + 1/60

    if(dt > 2) then
        dt = dt - 2
        index = index + 1
    end
    cls(2)
    spr(index, 2, 3)


    spr(5*16+5, 16, 8)
    spr(5*16+5, 8, 24)
    spr(5*16+5, 24, 16)
    spr(5*16+5, 112, 32)
    spr(5*16+5, 76, 96)
    spr(5*16+5, 100, 128 - 16)
    spr(5*16+5, 64, 8)
    spr(5*16+5, 16, 64)

end

function _resources()
    print("LOADED")
end
