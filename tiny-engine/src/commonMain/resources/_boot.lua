function _init()
    ready = false
    dt = 0

    prefix_str = "Built with"
    txt_str = "TINY"
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

function letter(lx, l, index, move)
    local offset = 3
    if move then
        offset = math.abs(math.cos(dt * math.pi * 2 + index) * 6)
    end
    local w = text.width(l)
    if ((not ready) and (l ~= " ")) then
        shape.rectf(lx + 1, 1 + 8 + offset, w, 12, "#5f574f")
        shape.rectf(lx, 8 + offset, w, 12, "#FFFFFF")
    else
        print(l, lx + 1, 1 + 8 + offset, "#5f574f")
        print(l, lx, 8 + offset, "#FFFFFF")
    end
end

function _draw()
    gfx.cls("#000000")

    local px = 2
    for i = 1, #prefix_str do
        local l = prefix_str:sub(i, i)
        letter(px, l, i, false)
        px = px + text.width(l)
    end

    for i = 1, #txt_str do
        local l = txt_str:sub(i, i)
        letter(px, l, i, true)
        px = px + text.width(l)
    end
end

function _resources()
    ready = true
end
