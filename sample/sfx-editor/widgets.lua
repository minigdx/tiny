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





local ModeSwitch = require("widgets.ModeSwitch")
local Envelop = require("widgets.Envelop")
local Knob = require("widgets.Knob")
local Checkbox = require("widgets.Checkbox")
local Fader = require("widgets.Fader")
local MenuItemModule = require("widgets.MenuItem")
local MenuItem = MenuItemModule.MenuItem
local menuItems = MenuItemModule.menuItems
local Keyboard = require("widgets.Keyboard")
local Help = require("widgets.Help")
local Button = require("widgets.Button")

factory.create_mode_switch_component = function(self, value)
    local result = new(ModeSwitch, value)
    return result
end

factory.create_envelop = function(self, data)
    local result = new(Envelop, data)
    result.attack_start_x = result.x
    result.attack_start_y = result.y + result.height

    result:_init()

    return result
end

factory.create_knob = function(self, value)
    local result = new(Knob, value)
    result.label = result.fields.Label
    result.help = result.fields.Help
    result:_init()
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

factory.create_button = function(self, value)
    local result = new(Button, value)
    result.help = result.fields.Help
    return result
end

factory.create_help = function(self, data)
    local help = new(Help, data)
    return help
end

factory.create_keyboard = function(self, data)
    local keyboard = new(Keyboard, data)
    return keyboard
end

factory.create_menu_item = function(self, data)
    local menu = new(MenuItem, data)

    local item = data.fields.Item
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
