


local Mouse = {
    x = 0,
    y = 0,
}

local mouse = new(Mouse)

mouse._update = function(on_click)
    local pos = ctrl.touch()
    mouse.x = pos.x
    mouse.y = pos.y

    local clicked = ctrl.touched(0)
    if clicked then
        on_click(clicked.x, clicked.y)
    end
end

mouse._draw = function()
    shape.circle(mouse.x, mouse.y, 2, 9)
end

return mouse