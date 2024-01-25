


local Mouse = {
    x = 0,
    y = 0,
}

local mouse = new(Mouse)

mouse._update = function(on_update, on_click)
    local pos = ctrl.touch()
    mouse.x = pos.x
    mouse.y = pos.y

    on_update(pos.x, pos.y)

    local clicked = ctrl.touching(0)
    if clicked then
        on_click(pos.x, pos.y)
    end
end

mouse._draw = function(color)
    shape.circle(mouse.x, mouse.y, 2, color)
end

return mouse