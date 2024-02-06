local Fader = {
    x = 0,
    y = 0,
    width = 11,
    height = 80,
    min_value = 0,
    max_value = 10,
    value = nil,
    values = nil,
    tip_color = 9,
    disabled_color = 7,
    label = "",
    type = "fader",
    data = nil,
    on_value_update = function(fader, value)
    end
}

local FaderValue = {
    value = 0,
    color = 0
}

local Button = {
    x = 0,
    y = 0,
    width = 16,
    height = 16,
    grouped = true,
    status = 0, -- 0 : idle ; 1 : over ; 2 : active
    overlay = 0, -- sprite index,
    on_active_button = function(current, prec)
    end
}

local Tab = {
    x = 0,
    y = 0,
    width = 0,
    height = 8,
    label = "+",
    content = nil,
    status = 0, -- 0 : inactive ; 1 : active
    new_tab = false
}

local Counter = {
    label = "",
    value = 0,
    x = 0,
    y = 0,
    width = 16,
    height = 16,
    status = 0, -- 0 : iddle ; 1 : over left ; 2 : over right
    on_left = function(counter)
    end,
    on_right = function(counter)
    end,
    spr = 32
}

local buttons = {}
local tabs = {}
local faders = {}
local widgets = {}
local counters = {}

local factory = {}

factory.createCounter = function(value)
    local result = new(Counter, value)
    table.insert(widgets, result)
    table.insert(counters, result)
    return result
end

factory.createTab = function(value)
    local result = new(Tab, value)
    table.insert(widgets, result)
    table.insert(tabs, result)
    return result
end

factory.createButton = function(value)
    local result = new(Button, value)
    table.insert(widgets, result)
    table.insert(buttons, result)
    return result
end

factory.setFaderValue = function(fader, index, value, color)
    if fader.values == nil then
        fader.values = {}
    end

    if value <= 0 then
        fader.values[index] = nil
    else
        fader.values[index] = {
            value = value,
            color = color
        }
    end
end

factory.resetFaderValue = function(fader)
    fader.values = {}
end

factory.createFader = function(value)
    local result = new(Fader, value)
    table.insert(widgets, result)
    table.insert(faders, result)
    return result
end

function inside_widget(w, x, y)
    return w.x <= x and x <= w.x + w.width and w.y <= y and y <= w.y + w.height
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

    for c in all(counters) do
        -- inside the widgets
        local left = {
            width = 8,
            height = 8,
            x = c.x,
            y = c.y + 8
        }

        local right = {
            width = 8,
            height = 8,
            x = c.x + 8,
            y = c.y + 8
        }
        if inside_widget(left, x, y) then
            c.status = 1
        elseif inside_widget(right, x, y) then
            c.status = 2
        else
            c.status = 0
        end
    end
end

factory.on_click = function(x, y)
    -- on click faders
    for f in all(faders) do
        local box = {
            x = f.x,
            y = f.y,
            width = f.width,
            height = f.height + 12
        }
        if inside_widget(box, x, y) then
            local percent = math.max(0.0, 1.0 - ((y - f.y) / f.height))
            local value = percent * (f.max_value - f.min_value) + f.min_value
            f.on_value_update(f, value)
        end
    end
end

factory.on_clicked = function(x, y)
    -- on click buttons
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
    if current ~= nil and current.grouped then
        if prec ~= nil then
            prec.status = 0
        end
        current.status = 2
    end

    if current ~= nil then
        current.on_active_button(current, prec)
    end

    -- on click tab
    local new_active = nil
    local current_active = nil
    for t in all(tabs) do
        if t.status == 1 then
            current_active = t
        elseif inside_widget(t, x, y) and t.status == 0 then
            new_active = t
        end

    end

    if new_active ~= nil then
        if new_active.new_tab then
            new_active.width = 2 * 16 + 8
            new_active.label = ""
            new_active.new_tab = false

            if #tabs < 12 then
                factory.createTab({
                    width = 24,
                    new_tab = true,
                    x = new_active.x + new_active.width,
                    on_active_tab = new_active.on_active_tab
                })

            end

            factory.on_new_tab(new_active)
        end
        current_active.status = 0
        new_active.status = 1
        new_active.on_active_tab(new_active, current_active)
    end

    for c in all(counters) do
        if c.status == 1 then
            c.on_left(c)
        elseif c.status == 2 then
            c.on_right(c)
        end
    end
end

factory._update = function(mouse)

end

function draw_tabs()
    local active_tab = tabs[1]
    for index = #tabs, 1, -1 do
        local v = tabs[index]
        if v.status == 0 then
            draw_tab(v)
        else
            active_tab = v
        end
    end

    draw_tab(active_tab)
end

function draw_tab(tab)
    local offset = tab.status * 8

    -- body
    local time = math.floor(tab.width / 16)
    local rest = tab.width % 16
    for i = 0, time - 1 do
        spr.sdraw(tab.x + (i) * 16, 0, 80, offset, 16, 8)

    end

    spr.sdraw(tab.x + (time) * 16, 0, 80, offset, rest, 8)

    -- right
    spr.sdraw(tab.x + tab.width, 0, 96, offset, 8, 8)

    local center = tab.width * 0.5 - #tab.label * 0.5 * 4

    print(tab.label, tab.x + center, tab.y + 2)

    -- left
    if tab.status == 1 then
        spr.sdraw(tab.x - 8, 0, 64, 8, 8, 8)
    end

end

function draw_fader(f)
    if f.values ~= nil and next(f.values) then
        for v in all(f.values) do
            local y = f.height - ((v.value - f.min_value) / (f.max_value - f.min_value) * f.height)
            local tipy = f.y + y
            shape.rectf(f.x, tipy, f.width, 4, v.color)
        end
    else
        local y = f.height - (0 / (f.max_value - f.min_value) * f.height)
        local tipy = f.y + y
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

function draw_counter(counter)

    spr.draw(counter.spr + counter.status, counter.x, counter.y)

    print(counter.label, counter.x + 1, counter.y - 4)
    print(string.sub(counter.value, 1, 4), counter.x + 3, counter.y + 2)
end

factory._draw = function()
    for c in all(counters) do
        draw_counter(c)
    end

    for f in all(faders) do
        draw_fader(f)
    end

    for b in all(buttons) do
        draw_button(b)
    end

    draw_tabs()
end

return factory
