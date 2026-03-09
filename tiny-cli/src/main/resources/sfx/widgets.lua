local utils = require("widgets.utils")
local icons = require("widgets.icons")



local factory = { }

-- Expose inside_widget as global for widgets that depend on it (e.g., ModeSwitch)
inside_widget = utils.inside_widget




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





local Envelop = require("widgets.Envelop")
local Knob = require("widgets.Knob")
local Checkbox = require("widgets.Checkbox")
local Fader = require("widgets.Fader")
local Keyboard = require("widgets.Keyboard")
local Button = require("widgets.Button")
local Dropdown = require("widgets.Dropdown")
local Modal = require("widgets.Modal")
local TextInput = require("widgets.TextInput")
local Panel = require("widgets.Panel")
local TextButton = require("widgets.TextButton")
local Speaker = require("widgets.Speaker")
local Counter = require("widgets.Counter")

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
    return result
end

factory.create_button = function(self, value)
    local result = new(Button, value)
    result.help = result.fields.Help
    if result.fields.IconName then
        result.overlay = icons[result.fields.IconName]
    end
    return result
end

factory.create_dropdown = function(self, value)
    local result = new(Dropdown, value)
    result.options = result.fields.Options or {}
    result.help = result.fields.Help
    result:_init()
    return result
end

factory.create_modal = function(self, data)
    local result = new(Modal, data)
    result:_init(self)
    return result
end

factory.create_keyboard = function(self, data)
    local keyboard = new(Keyboard, data)
    return keyboard
end

factory.create_text_input = function(self, data)
    local result = new(TextInput, data)
    result.help = result.fields.Help
    result.label = result.fields.Label
    result:_init()
    return result
end

factory.create_panel = function(self, value)
    local result = new(Panel, value)
    result.label = result.fields.Label
    result.variant = utils.variant_mapping[result.fields.Variant] or 0
    return result
end

factory.create_text_button = function(self, value)
    local result = new(TextButton, value)
    result.label = result.fields.Label or ""
    result.is_active = result.fields.IsActive or false
    result.variant = utils.variant_mapping[result.fields.Variant] or 0
    if result.fields.TinyExit then
        result.on_change = function(self)
            tiny.exit(self.fields.TinyExit)
        end
    end
    return result
end

factory.create_speaker = function(self, value)
    local result = new(Speaker, value)
    return result
end

factory.create_counter = function(self, data)
    local result = new(Counter, data)
    return result
end


factory._draw = function(self)
    for w in all(self.widgets) do
        w:_draw()
    end
end

return factory
