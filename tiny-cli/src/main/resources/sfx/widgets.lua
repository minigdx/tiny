local on_update = function(self, listener)
    table.insert(self.listeners, listener)
end

local fire_on_update = function(self, value)
    for l in all(self.listeners) do
        l(self, value)
    end
end

local set_value = function(self, value)
    self.value = value
    if (self.on_change) then
        self:on_change()
    end
    self:fire_on_update(value)
end

local Fader = {
    x = 0,
    y = 0,
    width = 11,
    height = 80,
    enabled = true,
    min_value = 0,
    max_value = 10,
    value = 0,
    tip_color = 9,
    disabled_color = 7,
    label = "",
    type = "fader",
    data = nil,
    index = 0,
    on_value_update = function(fader, value)
    end,
    listeners = {},
    on_update = on_update,
    fire_on_update = fire_on_update,
    set_value = set_value,
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
    end,
    listeners = {},
    on_update = on_update,
    fire_on_update = fire_on_update,
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
    listeners = {},
    on_update = on_update,
    fire_on_update = fire_on_update,
    set_value = set_value,
}

local MenuItem = {
    _type = "MenuItem",
    spr = nil,
    hold = false,
    status = 0,
    active = 0,
    help = "",
    on_click = function()
    end,
    on_hover = function()
    end,
    listeners = {},
    on_update = on_update,
    fire_on_update = fire_on_update,
    set_value = set_value,
}

local Keyboard = {

}

local factory = { }

function inside_widget(w, x, y, offset)
    local off = 0
    if (offset) then
        off = offset
    end

    return w.x - off <= x and
            x <= w.x + w.width + off and
            w.y - off <= y and
            y <= w.y + w.height + off
end

factory.create_button = function(self, value)
    local result = new(Button, value)
    result.help = result.fields.Help
    if (value.fields.Type == "SINE") then
        result.overlay = { x = 0, y = 16 }
    elseif (value.fields.Type == "NOISE") then
        result.overlay = { x = 16, y = 16 }
    elseif (value.fields.Type == "PULSE") then
        result.overlay = { x = 32, y = 16 }
    elseif (value.fields.Type == "TRIANGLE") then
        result.overlay = { x = 48, y = 16 }
    elseif (value.fields.Type == "SAW_TOOTH") then
        result.overlay = { x = 64, y = 16 }
    elseif (value.fields.Type == "SQUARE") then
        result.overlay = { x = 80, y = 16 }
    end
    return result
end

factory.create_mode_switch = function(self, value)
    local result = new(Button, value)
    result.help = result.fields.Help
    if (value.fields.EditorType == "InstrumentEditor") then
        result.overlay = { x = 0, y = 9*16 }
        result.on_change = function()
            tiny.exit("sfx-editor.lua")
        end
    elseif (value.fields.EditorType == "MusicalBarEditor") then
        result.overlay = { x = 16, y = 9*16 }
        result.on_change = function()
            tiny.exit("bar-editor.lua")
        end
    end
    return result
end

Button._update = function(self)
    if self.status == 2 then
        return
    end

    local pos = ctrl.touch()

    if inside_widget(self, pos.x, pos.y) then
        self.status = 1
        if self.on_hover ~= nil then
            self:on_hover()
        end
        local touched = ctrl.touched(0)
        if touched then
            self:fire_on_update(self.status)
            if (self.on_change) then
                self:on_change()
            end
        end
    else
        self.status = 0
    end

end

Button._draw = function(self)
    local background = 0
    if self.status > 0 then
        background = 16
    end

    spr.sdraw(self.x, self.y, 0 + background, 0, self.width, self.height)

    if self.overlay ~= nil then
        spr.sdraw(self.x, self.y, self.overlay.x, self.overlay.y, self.width, self.height)
    end
end

factory.create_envelop = function(self, data)
    local result = new(Envelop, data)
    result.attack_start_x = result.x
    result.attack_start_y = result.y + result.height

    return result
end

factory.create_knob = function(self, value)
    local result = new(Knob, value)
    result.label = result.fields.Label
    result.help = result.fields.Help
    return result
end

factory.create_tabs = function(self)
    local tabs = new(TabManager)
    tabs:_init()
    table.insert(self.tabs, tabs)
    table.insert(self.widgets, tabs)
    return tabs
end

Knob._draw = function(self)
    local angle = (1.8 * math.pi) * self.value + math.pi * 0.6

    local target_x = math.cos(angle) * 8 + self.x + 8
    local target_y = math.sin(angle) * 8 + self.y + 8

    spr.sdraw(self.x, self.y, 0, 64, 16, 16)
    shape.line(self.x + 8, self.y + 8, target_x, target_y, 9)
    print(self.label, self.x - 1, self.y + 18)

    if self.is_hover or self.active_color then
        local c = 9
        if (self.active_color) then
            c = self.active_color
        end
        shape.rect(self.x, self.y, self.width, self.height, c)
    end
end

Knob._update = function(self)

    local touching = ctrl.touching(0)

    -- the click started in the widget?
    if touching ~= nil and inside_widget(self, touching.x, touching.y) then
        if self.start_value == nil then
            self.start_value = self.value
        end
        local touch = ctrl.touch()

        self.active_color = 11

        local dst = self.y + 4 - touch.y
        local percent = math.max(math.min(1, dst / 32), -1)
        local value = math.min(math.max(0, self.start_value + percent), 1)
        self:set_value(value)

    end

    local pos = ctrl.touch()
    if inside_widget(self, pos.x, pos.y) then
        if self.on_hover ~= nil then
            self:on_hover()
        end
        self.is_hover = true
    else
        self.is_hover = false
    end

    if touching == nil then
        self.start_value = nil
        self.active_color = nil
    end
end

factory.create_fader = function(self, value)
    local result = new(Fader, value)
    result.help = result.fields.Help
    result.label = result.fields.Label
    result.hitbox = {
        x = result.x,
        y = result.y,
        width = result.width,
        height = result.height + 4
    }
    return result
end

Fader._update = function(self)
    local pos = ctrl.touch()
    if inside_widget(self.hitbox, pos.x, pos.y) then
        if self.on_hover ~= nil then
            self:on_hover()
        end

        if ctrl.touching(0) then
            local percent = math.max(0.0, 1.0 - ((pos.y - self.y) / self.height))
            self.value = percent

            -- todo: to be removed as fire_on_update should be used instead
            if self.on_value_update then
                self:on_value_update(self.value)
            end

            self:fire_on_update(self.value)
        end
    end
end

Fader._draw = function(self)
    local color = self.disabled_color

    if self.value ~= nil and self.value > 0 then
        color = self.tip_color
    end
    local y = self.height - self.value * self.height
    local tipy = self.y + y
    shape.rectf(self.x + 1, tipy, self.width - 2, 2, self.tip_color)
end

function draw_counter(counter)
    spr.draw(counter.spr + counter.status, counter.x, counter.y)

    print(counter.label, counter.x + 1, counter.y - 4)
    print(string.sub(counter.value, 1, 4), counter.x + 3, counter.y + 2)
end

Envelop._update = function(self)
    self.decay = math.min(self.decay, 1 - self.attack)
    self.release = math.min(self.release, 1 - (self.decay + self.attack))

    self.attack_end_x = self.x + self.attack * self.width
    self.attack_end_y = self.y

    self.decay_end_x = self.attack_end_x + self.decay * self.width
    self.decay_end_y = self.y + self.height * (1 - self.sustain)

    self.release_start_x = self.x + self.width - self.release * self.width
    self.release_start_y = self.y + self.height * (1 - self.sustain)
end

Envelop._draw = function(self)
    shape.rect(self.x, self.y, self.width + 1, self.height + 1, 9)

    -- attack
    shape.line(self.x, self.y + self.height, self.attack_end_x, self.attack_end_y, 8)
    shape.circle(self.attack_end_x, self.attack_end_y, 2, 8)

    -- decay
    shape.line(self.attack_end_x, self.attack_end_y, self.decay_end_x, self.decay_end_y, 10)
    shape.circle(self.decay_end_x, self.decay_end_y, 2, 10)

    -- release
    shape.line(self.release_start_x, self.release_start_y, self.x + self.width, self.y + self.height, 9)
    shape.circle(self.release_start_x, self.release_start_y, 2, 9)

    shape.line(self.decay_end_x, self.decay_end_y, self.release_start_x, self.release_start_y, 9)

    -- sustain
    local width = 8
    local height = 4
    shape.rect(self.decay_end_x + (self.release_start_x - self.decay_end_x - width) * 0.5, self.y + (1 - self.sustain) * self.height - height * 0.5, width, height, 8)
end

Envelop.set_attack = function(self, widget)
    self.attack_fader = widget
    local on_value_update = function(nested, value)
        self.attack = value
    end
    -- set the initial value on the widget
    widget:on_update(on_value_update)
end

Envelop.set_decay = function(self, widget)
    self.decay_fader = widget
    local on_value_update = function(nested, value)
        self.decay = value
    end
    -- set the initial value on the widget
    widget:on_update(on_value_update)
end

Envelop.set_sustain = function(self, widget)
    self.sustain_fader = widget
    local on_value_update = function(nested, value)
        self.sustain = value
    end
    -- set the initial value on the widget
    widget:on_update(on_value_update)
end

Envelop.set_release = function(self, widget)
    self.release_fader = widget
    local on_value_update = function(nested, value)
        self.release = value
    end
    -- set the initial value on the widget
    widget:on_update(on_value_update)
end

factory.create_checkbox = function(self, data)
    local result = new(Checkbox, data)
    result.help = result.fields.Help
    result.label = result.fields.Label
    return result
end

Checkbox._update = function(self)
    local pos = ctrl.touched(0)
    if pos ~= nil then
        local w = {
            x = self.x,
            y = self.y,
            height = self.height,
            width = self.width + #self.label * 4
        }
        if inside_widget(w, pos.x, pos.y) then
            self.value = not self.value
            if self.on_changed then
                self:on_changed(self.value)
            end
        end
    end

    pos = ctrl.touch()
    if self.on_hover and inside_widget(self, pos.x, pos.y) then
        self:on_hover()
    end
end

Checkbox._draw = function(self)
    if self.value then
        spr.sdraw(self.x, self.y, 8, 48, 8, 8)
    else
        spr.sdraw(self.x, self.y, 0, 48, 8, 8)
    end
    print(self.label, self.x + 10, self.y + 2)
end

local Help = {
    _type = "Help",
    label = ""
}

Help._update = function(self)

end

Help._draw = function(self)
    print(self.label, self.x, self.y + 2)
    shape.rect(self.x, self.y, self.width, self.height)
end

factory.create_help = function(self, data)
    local help = new(Help, data)
    return help
end

factory.create_keyboard = function(self, data)
    local keyboard = new(Keyboard, data)
    return keyboard
end

Keyboard._update = function(self)

    local spr_x = 16
    local spr_y = 112

    local color_to_note = {
        [16] = "C4",
        [15] = "Cs4",
        [14] = "D4",
        [13] = "Ds4",
        [12] = "E4",
        [11] = "F4",
        [10] = "Fs4",
        [9] = "G4",
        [7] = "Gs4",
        [6] = "A4",
        [5] = "As4",
        [4] = "B4"
    }
    local pos = ctrl.touch()
    if (ctrl.touched(0) and inside_widget(self, pos.x, pos.y)) then
        local relative_x = pos.x - self.x
        local relative_y = pos.y - self.y

        local color = spr.pget(relative_x + spr_x, relative_y + spr_y)
        local value = color_to_note[color]
        if value then
            self.value = value
            if(self.on_change) then
                self:on_change()
            end
        else
            self.value = nil
        end
    else
        self.value = nil
    end
end

Keyboard._draw = function(self)
    spr.sdraw(self.x, self.y, 16, 80, self.width, self.height)
end

local menuItems = {}

MenuItem._update = function(self)
    local pos = ctrl.touch()
    if not self.hold then
        self.active = 0
    end

    if inside_widget(self, pos.x, pos.y) then
        if self.active == 0 then
            self.status = 1
        end
        if ctrl.touched(0) then
            self:fire_on_update(self.status)
            if(self.on_change) then
                self:on_change()
            end
            if self.hold then
                for i in all(menuItems) do
                    i.active = 0
                end
            end
            self.active = 1
            self.status = 0
        end
        self:on_hover()
    else
        self.status = 0
    end

end

MenuItem._draw = function(self)
    if self.spr ~= nil then
        spr.draw(self.spr + self.status * 128 + self.active * (128 + 32), self.x, self.y)
    end

    if self.label ~= nil then
        print(self.value, self.x + 5, self.y + 2)
    end
end

factory.create_menu_item = function(self, data)
    local menu = new(MenuItem, data)

    local item = data.fields.Item
    -- todo: move outside the widgets factory this configuration
    if item == "Wave" then
        menu.spr = 14
        menu.hold = true
    elseif item == "Fx" then
        menu.spr = 15
        menu.hold = true
    elseif item == "Music" then
        menu.spr = 16
        menu.hold = true
    elseif item == "Save" then
        menu.spr = 17
    elseif item == "Prev" then
        menu.spr = 21
    elseif item == "Next" then
        menu.spr = 22
    elseif item == "NewFile" then
        menu.spr = 13
    end
    menu.item = item
    menu.help = data.fields.Help

    table.insert(menuItems, menu)
    return menu
end

factory._draw = function(self)
    for w in all(self.widgets) do
        w:_draw()
    end
end

return factory
