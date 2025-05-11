-- coucouc
function _init()
    ready = false
    dt = 0

    prefix = {"b", "u", "i", "l", "d"," ", "w", "i", "t", "h", " "}
    txt = { "t", "i", "n", "y" }
    waiting = 3
end

function _update()
    dt = dt + 1 / 60
    if ready then
        waiting = waiting - 1 / 60
    end

    if (waiting < 0) then
        gfx.cls("#000000")
        tiny.exit(0) -- start the first script in the game script stack
    end
end

function letter(x, l, index, move)
    local offset = 3
    if move then
        offset = math.abs(math.cos(dt * math.pi * 2 + index) * 4)
    end
    if ((not ready) and (l ~= " ")) then
        shape.rectf(
                x + index * 4 + 1,
                1 + 8 + offset,
                4, 4,
                "#5f574f"
        )
        shape.rectf(
                x + index * 4,
                8 + offset,
                4, 4,
                "#FFFFFF"
        )
    else
        print(l, x + index * 4 + 1, 1 + 8 + offset, "#5f574f")
        print(l, x + index * 4, 8 + offset, "#FFFFFF")
    end
end
function _draw()
    gfx.cls("#000000")

    for index = 1, #prefix do
        letter(2, prefix[index], index, false)
    end

    for index = 1, #txt do
        letter(46, txt[index], index, true)
    end


end

function _resources()
    ready = true
end
