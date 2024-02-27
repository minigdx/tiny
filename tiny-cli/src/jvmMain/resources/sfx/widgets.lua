local Fader = {
    x = 0,
    y = 0,
    width = 11,
    height = 80,
    enabled = true,
    min_value = 0,
    max_value = 10,
    value = nil,
    tip_color = 9,
    disabled_color = 7,
    label = "",
    type = "fader",
    data = nil,
    index = 0,
    on_value_update = function(fader, value)
    end
}

local Button = {
    x = 0,
    y = 0,
    width = 16,
    height = 16,
    enabled = true,
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
    enabled = true,
    label = "+",
    content = nil,
    status = 0, -- 0 : inactive ; 1 : active
    new_tab = false,
    on_active_tab = nil,
    on_new_tab = nil
}

local Counter = {
    label = "",
    value = 0,
    x = 0,
    y = 0,
    width = 16,
    height = 16,
    enabled = true,
    status = 0, -- 0 : iddle ; 1 : over left ; 2 : over right
    on_left = function(counter)
    end,
    on_right = function(counter)
    end,
    spr = 32
}

local Envelop = {
    label = "",
    value = 0,
    x = 0,
    y = 0,
    width = 128,
    height = 64,
    enabled = true,
    attack = 0,
    decay = 0.2,
    sustain = 0.5,
    release = 0,

    attack_end_x = 0,
    attack_end_y = 0,
    decay_end_x = 0,
    decay_end_y = 0,
    release_start_x = 0,
    release_start_y = 0
}

local Checkbox = {
    label = "",
    value = false,
    x = 0,
    y = 0,
    width = 8,
    height = 8,
    enabled = true
}

local Knob = {
    label = "",
    value = 0,
    x = 0,
    y = 0,
    width = 16,
    height = 16,
    enabled = true,
    on_update = nil
}

local buttons = {}
local tabs = {}
local faders = {}
local widgets = {}
local counters = {}
local envelops = {}
local checkboxes = {}
local knobs = {}

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

factory.createFader = function(value)
    local result = new(Fader, value)
    table.insert(widgets, result)
    table.insert(faders, result)

    result.index = #faders

    return result
end

factory.createEnvelop = function(value)
    local result = new(Envelop, value)
    result.attack_start_x = result.x
    result.attack_start_y = result.y + result.height

    table.insert(widgets, result)
    table.insert(envelops, result)

    return result
end

factory.createCheckbox = function(value)
    local result = new(Checkbox, value)

    table.insert(widgets, result)
    table.insert(checkboxes, result)
    return result
end

factory.createKnob = function(value)
    local result = new(Knob, value)

    table.insert(widgets, result)
    table.insert(knobs, result)
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

    for e in all(envelops) do
        if e.enabled then
            e.attack_end_x = e.attack_start_x + e.width * e.attack
            e.attack_end_y = e.attack_start_y - e.height

            e.decay_end_x = e.attack_end_x + e.width * e.decay
            e.decay_end_y = e.y + (1 - e.sustain) * e.height

            e.release_start_x = e.x + e.width - e.width * e.release
            e.release_start_y = e.y + (1 - e.sustain) * e.height
        end
    end


    if ctrl.touching(0) == nil then
        for k in all(knobs) do
            k.update_in_progress = false
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
        if f.enabled and inside_widget(box, x, y) then
            local percent = math.max(0.0, 1.0 - ((y - f.y) / f.height))
            local value = percent * (f.max_value - f.min_value) + f.min_value
            f.on_value_update(f, value)
        end
    end

    for k in all(knobs) do
        if inside_widget(k, x, y) or k.update_in_progress then
            local dst = k.y + 8 - y
            local percent = math.max(math.min(1, dst / 32), 0)
            k.value = percent
            k.update_in_progress = true
            if k.on_update ~= nil then
                k.on_update(k)
            end
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
    if current ~= nil and current.enabled and current.grouped then
        if prec ~= nil then
            prec.status = 0
        end
        current.status = 2
    end

    if current ~= nil and current.enabled then
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
        -- create a new tab.
        if new_active.new_tab then
            new_active.width = 2 * 16 + 8
            new_active.label = ""
            new_active.new_tab = false

            if #tabs < 12 then
                factory.createTab({
                    width = 24,
                    new_tab = true,
                    x = new_active.x + new_active.width,
                    on_active_tab = new_active.on_active_tab,
                    on_new_tab = new_active.on_new_tab
                })
            end

            new_active.on_new_tab(new_active)
        end
        current_active.status = 0
        new_active.status = 1
        if new_active.on_active_tab ~= nil then
            new_active.on_active_tab(new_active, current_active)
        end
    end

    for c in all(counters) do
        if c.enabled and c.status == 1 then
            c.on_left(c)
        elseif c.enabled and c.status == 2 then
            c.on_right(c)
        end
    end

    for c in all(checkboxes) do
        if c.enabled and inside_widget(c, x, y) then
            c.value = not c.value
            if c.on_update ~= nil then
                c.on_update(c)
            end
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
    if tab == nil then
        return
    end
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
    if f.value ~= nil and f.value > 0 then
        local y = f.height - ((f.value - f.min_value) / (f.max_value - f.min_value) * f.height)
        local tipy = f.y + y
        shape.rectf(f.x, tipy, f.width, 4, f.tip_color)
    else
        -- fader value = 0
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

function draw_envelop(envelop)

    shape.rect(envelop.x, envelop.y, envelop.width, envelop.height, 9)

    -- attack
    print("attack", envelop.attack_end_x, envelop.attack_end_y - 8)
    shape.line(envelop.x, envelop.y + envelop.height, envelop.attack_end_x, envelop.attack_end_y, 9)
    if envelop.is_over_attack then
        shape.circlef(envelop.attack_end_x, envelop.attack_end_y, 2, 9)
    else
        shape.circle(envelop.attack_end_x, envelop.attack_end_y, 2, 9)
    end

    print("decay", envelop.decay_end_x, envelop.decay_end_y - 8)
    shape.line(envelop.attack_end_x, envelop.attack_end_y, envelop.decay_end_x, envelop.decay_end_y, 9)
    if envelop.is_over_decay then
        shape.circlef(envelop.decay_end_x, envelop.decay_end_y, 2, 9)
    else
        shape.circle(envelop.decay_end_x, envelop.decay_end_y, 2, 9)
    end

    print("release", envelop.release_start_x, envelop.release_start_y - 8)
    shape.line(envelop.release_start_x, envelop.release_start_y, envelop.x + envelop.width, envelop.y + envelop.height,
        9)
    if envelop.is_over_release then
        shape.circlef(envelop.release_start_x, envelop.release_start_y, 2, 9)
    else
        shape.circle(envelop.release_start_x, envelop.release_start_y, 2, 9)
    end

    shape.line(envelop.decay_end_x, envelop.decay_end_y, envelop.release_start_x, envelop.release_start_y, 9)
    local width = 8
    local height = 4
    if envelop.is_over_sustain then
        shape.rectf(envelop.decay_end_x + (envelop.release_start_x - envelop.decay_end_x - width) * 0.5,
            envelop.y + (1 - envelop.sustain) * envelop.height - height * 0.5, width, height, 8)
    else
        shape.rect(envelop.decay_end_x + (envelop.release_start_x - envelop.decay_end_x - width) * 0.5,
            envelop.y + (1 - envelop.sustain) * envelop.height - height * 0.5, width, height, 8)
    end
end

function draw_checkbox(c)
    if c.value then
        spr.sdraw(c.x, c.y, 8, 48, 8, 8)
    else
        spr.sdraw(c.x, c.y, 0, 48, 8, 8)
    end
    print(c.label, c.x + 10, c.y + 2)
end

function draw_knob(k)
    local angle = (1.8 * math.pi) * k.value + math.pi * 0.6

    local target_x = math.cos(angle) * 6 + k.x + 8
    local target_y = math.sin(angle) * 6 + k.y + 8

    spr.sdraw(k.x, k.y, 0, 64, 16, 16)
    shape.line(k.x + 8, k.y + 8, target_x, target_y, 9)
    print(k.label, k.x, k.y + 18)
end
factory._draw = function()
    for c in all(counters) do
        if c.enabled then
            draw_counter(c)
        end
    end

    for f in all(faders) do
        if f.enabled then
            draw_fader(f)
        end
    end

    for b in all(buttons) do
        if b.enabled then
            draw_button(b)
        end
    end

    for e in all(envelops) do
        if e.is_over_attack then
           
        end
        if e.enabled then
            draw_envelop(e)
        end
    end

    for c in all(checkboxes) do
        if c.enabled then
            draw_checkbox(c)
        end
    end

    for k in all(knobs) do
        if k.enabled then
            draw_knob(k)
        end
    end

    draw_tabs()
end

return factory
