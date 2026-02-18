local mouse = require("mouse")
local ModeSwitch = require("widgets/ModeSwitch")
local LayerManager = require("layers")

local all_widgets = {}
local layer_manager = nil

function _init_mode_switch(entities)
    for mode in all(entities["ModeSwitch"]) do
        local button = new(ModeSwitch, mode)
        table.insert(all_widgets, button)
    end
end

function _init()
    all_widgets = {}
    layer_manager = nil

    map.level("SfxEditor")
    local widget_entities = map.entities("Widgets")

    _init_mode_switch(widget_entities)

    layer_manager = LayerManager.create()
    layer_manager:register("Widgets", { tiles = "WidgetsTiles", widgets = all_widgets, always = true })
end

function _update()
    mouse._update(function() end, function() end, function() end)
    layer_manager:update_widgets()
end

function _draw()
    layer_manager:draw_base()
    layer_manager:draw_active()
    mouse._draw()
end
