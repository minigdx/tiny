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
    elseif (value.fields.Type == "SAVE") then
        result.overlay = { x = 48, y = 32 }
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



factory.create_tabs = function(self)
    local tabs = new(TabManager)
    tabs:_init()
    table.insert(self.tabs, tabs)
    table.insert(self.widgets, tabs)
    return tabs
end



function draw_counter(counter)
    spr.draw(counter.spr + counter.status, counter.x, counter.y)

    print(counter.label, counter.x + 1, counter.y - 4)
    print(string.sub(counter.value, 1, 4), counter.x + 3, counter.y + 2)
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

local ModeSwitch = require("widgets.ModeSwitch")
local Envelop = require("widgets.Envelop")
local Knob = require("widgets.Knob")
local Checkbox = require("widgets.Checkbox")
local Fader = require("widgets.Fader")

factory.create_mode_switch_component = function(self, value)
    local result = new(ModeSwitch, value)
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
    result.label = result.fields.Label
    result.help = result.fields.Help
    return result
end

factory.create_checkbox = function(self, data)
    local result = new(Checkbox, data)
    result.help = result.fields.Help
    result.label = result.fields.Label
    return result
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

factory._draw = function(self)
    for w in all(self.widgets) do
        w:_draw()
    end
end

return factory
