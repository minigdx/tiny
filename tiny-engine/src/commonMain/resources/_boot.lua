function _init()
    ready = false
    dt = 0
    txt = {"l", "o", "a", "d", "i", "n", "g"}
    waiting = 3
end

function _update()
    dt = dt + 1/60
    if ready then
        waiting = waiting - 1/60
    end

    if(waiting < 0) then
        cls(1)
        exit()
    end
end

function _draw()
    cls(1)
    for index=1,#txt do

        print(txt[index], 2 + index * 4 + 1, 1 + 8 + abs(cos(dt * PI * 2 + index) * 4), "#5f574f")
        print(txt[index], 2 + index * 4, 8 + abs(cos(dt * PI * 2 + index) * 4), "#FFFFFF")
    end
    

end

function _resources()
    -- current = loaded
    debug("loaded")
    ready = true
end 
