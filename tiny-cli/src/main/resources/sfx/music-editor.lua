local mouse = require("mouse")
local widgets = require("widgets")
local ModeSwitch = require("widgets/ModeSwitch")

local m = {
    widgets = {}
}


function _init_mode_switch(entities)
    for mode in all(entities["ModeButton"]) do
        local button = new(ModeSwitch, mode)
        table.insert(m.widgets, button)
    end
end


function _init()
    map.level("MusicEditor")
    local entities = map.entities()

    _init_mode_switch(entities)

end

function _update()
    mouse._update(function()
    end, function()
    end, function()
    end)

    for w in all(m.widgets) do
        w:_update()
    end

end

function _draw()
    gfx.cls(1)
    map.draw()
    for w in all(m.widgets) do
        w:_draw()
    end
    mouse._draw()
end