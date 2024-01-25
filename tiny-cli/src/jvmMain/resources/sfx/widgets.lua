local Fader = {
    x = 0,
    y = 0,
    width = 11,
    height = 80,
    min_value = 0,
    max_value = 10,
    value = nil,
    tip_color = 9,
    disabled_color = 7,
    label = "",
    type = "fader",
    data = nil,
    on_value_update = function(fader, value)
    end
}

local Button = {
    x = 0,
    y = 0,
    width = 16,
    height = 16,
    status = 0, -- 0 : idle ; 1 : over ; 2 : active
    overlay = 0, -- sprite index,
    on_active_button = function(current, prec)
    end
}

local buttons = {}
local faders = {}
local widgets = {}

local factory = {}

factory.createButton = function(value)
    local result = new(Button, value)
    table.insert(widgets, result)
    table.insert(buttons, result)
    return result
end

factory.createFader = function(value)

    local result = new(Fader, value)
    table.insert(widgets, result)
    table.insert(faders, result)
    return result
end

function inside_widget(w, x, y)
    return w.x <= x and w.x + w.width >= x and w.y <= y and w.y + w.height + 12 >= y
end

factory.on_update = function(x, y)
    for f in all(buttons) do
        if f.status == 1 then
            f.status = 0
        end

        if f.status == 0 and inside_widget(f, x, y) then
            f.status = 1
        end
    end
end
factory.on_click = function(x, y)
    for f in all(faders) do
        if inside_widget(f, x, y) then
            local percent = math.max(0.0, 1.0 - ((y - f.y) / f.height))
            local value = percent * (f.max_value - f.min_value) + f.min_value
            f.on_value_update(f, value)
        end
    end

    local prec = nil
    local current = nil
    for f in all(buttons) do
        if f.status == 2 then
            prec = f
        elseif f.status == 1 and inside_widget(f, x, y) then
            current = f
        end
    end
    -- active the current button and deactive the previous activated
    if current ~= nil then
        if prec ~= nil then
            prec.status = 0
        end
        current.status = 2
        current.on_active_button(current, prec)
    end
end

factory._update = function(mouse)

end

function draw_fader(f)
    local y = f.height - 4

    if f.value then
        y = f.height - ((f.value - f.min_value) / (f.max_value - f.min_value) * f.height)
    end
    local tipy = f.y + y

    if f.value > f.min_value then
        local linex = f.x + f.width * 0.5
        gfx.dither(0xA5A5)
        shape.line(linex, tipy, linex, f.y + f.height, 7)
        gfx.dither()
        shape.rectf(f.x, tipy, f.width, 4, f.tip_color)
    else
        shape.rectf(f.x, tipy, f.width, 4, f.disabled_color)
    end

    print(f.label, f.x, f.y + f.height + 5)
end

function draw_button(button)
    local background = 0
    if button.status > 0 then
        background = 1
    end

    spr.draw(background, button.x, button.y)

    if button.overlay ~= nil then

        spr.draw(button.overlay, button.x, button.y)
    end
end
factory._draw = function()
    for f in all(faders) do
        draw_fader(f)
    end

    for b in all(buttons) do
        draw_button(b)
    end
end

return factory
