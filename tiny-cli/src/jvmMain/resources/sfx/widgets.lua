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

local TabManager = {
    x = 0,
    y = 0,
    width = 0,
    height = 0,
    on_new_tab = nil,
    tabs = {},
    active_tab = nil
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

local factory = {
    tabs = {},
    widgets = {}
}

function inside_widget(w, x, y)
    return w.x <= x and x <= w.x + w.width and w.y <= y and y <= w.y + w.height
end

factory.create_button = function(self, value)
    local result = new(Button, value)
    result.help = result.customFields.Help
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
        if touched and self.on_changed ~= nil then
            self:on_changed()
        end
    else
        self.status = 0
    end

end

Button._draw = function(self)
    local background = 0
    if self.status > 0 then
        background = 1
    end

    spr.draw(28 + background, self.x, self.y)

    if self.overlay ~= nil then
        spr.draw(self.overlay, self.x, self.y)
    end
end

factory.create_fader = function(self, value)
    local result = new(Fader, value)
    result.help = result.customFields.Help
    result.label = result.customFields.Label
    return result
end

factory.create_envelop = function(self, data)
    local result = new(Envelop, data)
    result.attack_start_x = result.x
    result.attack_start_y = result.y + result.height

    return result
end

factory.create_knob = function(self, value)
    local result = new(Knob, value)
    result.label = result.customFields.Label
    result.help = result.customFields.Help
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

    local target_x = math.cos(angle) * 3 + self.x + 4
    local target_y = math.sin(angle) * 3 + self.y + 3

    spr.draw(30, self.x, self.y)
    shape.line(self.x + 4, self.y + 3, target_x, target_y, 9)
    print(self.label, self.x - 1, self.y + 10)
end

Knob._update = function(self)

    local touching = ctrl.touching(0)

    -- the click started in the widget?
    if touching ~= nil and inside_widget(self, touching.x, touching.y) then
        if self.start_value == nil then
            self.start_value = self.value
        end
        local touch = ctrl.touch()

        local dst = self.y + 4 - touch.y
        local percent = math.max(math.min(1, dst / 32), -1)
        self.value = math.min(math.max(0, self.start_value + percent), 1)
        if self.on_update ~= nil then
            self:on_update(self.value)
        end
    end

    local pos = ctrl.touch()
    if inside_widget(self, pos.x, pos.y) then
        if self.on_hover ~= nil then
            self:on_hover()
        end
    end

    if touching == nil then
        self.start_value = nil
    end
end


factory._init = function(self)

end

factory._update = function(mouse)

end

Fader._update = function(self)
    local pos = ctrl.touch()
    if inside_widget(self, pos.x, pos.y) then
        if self.on_hover ~= nil then
            self:on_hover()
        end

        if ctrl.touching(0) then
            local percent = math.max(0.0, 1.0 - ((pos.y - self.y) / self.height))
            self.value = percent

            if self.on_value_update then
                self:on_value_update(self.value)
            end
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

    self.attack_fader.value = self.attack
    self.decay_fader.value = self.decay
    self.release_fader.value = self.release
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

factory.create_checkbox = function(self, data)
    local result = new(Checkbox, data)
    result.help = result.customFields.Help
    result.label = result.customFields.Label
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
            if self.on_change then
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
    end
}

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
            self:on_click()
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
        print(self.label, self.x + 5, self.y + 2)
    end
end

factory.create_menu_item = function(self, data)
    local menu = new(MenuItem, data)

    local item = data.customFields.Item
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
    end
    menu.item = item
    menu.help = data.customFields.Help

    table.insert(menuItems, menu)
    return menu
end

factory._draw = function(self)
    for w in all(self.widgets) do
        w:_draw()
    end
end

return factory
