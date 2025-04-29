local widgets = require("widgets")
local mouse = require("mouse")

local instruments_screen = {
    widgets = {}
}

local m = instruments_screen

local find_widget = function(widgets_set, ref)
    for w in all(widgets_set) do
        if w.iid == ref.entityIid then
            return w
        end
    end
end

local on_menu_item_hover = function(self)
    -- help.label = self.help
    -- TODO: afficher le label quelque part.
end

local set_nested_value = function(target, path, value)
    local current_table = target

    for i = 1, #path - 1 do
        local key = path[i]
        current_table = current_table[key]
    end

    local final_key = path[#path]
    current_table[final_key] = value
end

local get_nested_value = function(source, path)
    local current_table = source

    for i = 1, #path - 1 do
        local key = path[i]
        current_table = current_table[key]
    end

    local final_key = path[#path]
    return current_table[final_key]
end

--- Get the value from the source.spath to target.tpath
--- when the value of the source.spath is updated.
--- Its the responsibility of the source to call `source:on_change`
--- to trigger changes
local produce_to = function(source, spath, target, tpath, conv)
    local old_on_change = source.on_change

    source.on_change = function(self)
        local value = get_nested_value(source, spath)
        if conv then
            value = conv(source, target, value)
        end
        set_nested_value(target, tpath, value)
        if old_on_change then
            old_on_change(self)
        end
    end
end

local listen_to = function(source, spath, conv)
    local old_on_change = source.on_change

    source.on_change = function(self)
        local value = get_nested_value(source, spath)
        if conv then
            conv(source, value)
        end

        if old_on_change then
            old_on_change(self)
        end
    end
end

--- Get the latest value from the source.spath in the _update() loop
--- and set in in target.tpath
local consume_on_update = function(target, tpath, source, spath, conv)
    local old_update = target._update
    target._update = function(self)
        local value = get_nested_value(source, spath)
        if conv then
            value = conv(source, target, value)
        end
        set_nested_value(self, tpath, value)
        old_update(target)
    end
end

local state = {
    instrument = nil
}

function _init()
    map.level("InstrumentEditor")
    local entities = map.entities()
    for k in all(entities["Knob"]) do
        local knob = widgets:create_knob(k)
        knob.on_hover = on_menu_item_hover
        table.insert(m.widgets, knob)

        if knob.fields.Label == "Harm1" then
            produce_to(knob, { "value" }, state, { "instrument", "harmonics", "1" })
            produce_to(state, { "instrument", "harmonics", "1" }, knob, { "value" })
        elseif knob.fields.Label == "Harm2" then
            produce_to(knob, { "value" }, state, { "instrument", "harmonics", "2" })
            produce_to(state, { "instrument", "harmonics", "2" }, knob, { "value" })
        elseif knob.fields.Label == "Harm3" then
            produce_to(knob, { "value" }, state, { "instrument", "harmonics", "3" })
            produce_to(state, { "instrument", "harmonics", "3" }, knob, { "value" })
        elseif knob.fields.Label == "Harm4" then
            produce_to(knob, { "value" }, state, { "instrument", "harmonics", "4" })
            produce_to(state, { "instrument", "harmonics", "4" }, knob, { "value" })
        elseif knob.fields.Label == "Harm5" then
            produce_to(knob, { "value" }, state, { "instrument", "harmonics", "5" })
            produce_to(state, { "instrument", "harmonics", "5" }, knob, { "value" })
        elseif knob.fields.Label == "Harm6" then
            produce_to(knob, { "value" }, state, { "instrument", "harmonics", "6" })
            produce_to(state, { "instrument", "harmonics", "6" }, knob, { "value" })
        elseif knob.fields.Label == "Harm7" then
            produce_to(knob, { "value" }, state, { "instrument", "harmonics", "7" })
            produce_to(state, { "instrument", "harmonics", "7" }, knob, { "value" })
        end
    end

    state.instrument = sfx.instrument(1)

    for k in all(entities["Envelop"]) do
        local envelop = widgets:create_envelop(k)
        envelop.on_hover = on_menu_item_hover

        local f = find_widget(m.widgets, envelop.fields.Attack)
        produce_to(f, { "value" }, state, { "instrument", "attack" })
        produce_to(state, { "instrument", "attack" }, f, { "value" })
        consume_on_update(envelop, { "attack" }, f, { "value" })

        f = find_widget(m.widgets, envelop.fields.Decay)
        produce_to(f, { "value" }, state, { "instrument", "decay" })
        produce_to(state, { "instrument", "decay" }, f, { "value" })
        consume_on_update(envelop, { "decay" }, f, { "value" })

        f = find_widget(m.widgets, envelop.fields.Sustain)
        produce_to(f, { "value" }, state, { "instrument", "sustain" })
        produce_to(state, { "instrument", "sustain" }, f, { "value" })
        consume_on_update(envelop, { "sustain" }, f, { "value" })

        f = find_widget(m.widgets, envelop.fields.Release)
        produce_to(f, { "value" }, state, { "instrument", "release" })
        produce_to(state, { "instrument", "release" }, f, { "value" })
        consume_on_update(envelop, { "release" }, f, { "value" })

        table.insert(m.widgets, envelop)
    end

    local waveToButton = function(source, target, value)
        if value == target.fields.Type then
            return 2
        else
            return 0
        end
    end

    local buttonToWave = function(source, target, value)
        debug.console("button to wave ", source.fields.Type)
        return source.fields.Type
    end

    for b in all(entities["Button"]) do
        local button = widgets:create_button(b)
        produce_to(button, { "status" }, state, { "instrument", "wave" }, buttonToWave)
        consume_on_update(button, { "status" }, state, { "instrument", "wave" }, waveToButton)
        table.insert(m.widgets, button)
    end

    for h in all(entities["InstrumentName"]) do
        local label = widgets:create_help(h)
        consume_on_update(label, { "label" }, state, { "instrument", "name" })
        table.insert(m.widgets, label)
    end

    local playNote = function(source, value)
        state.instrument.play(value)
    end

    for k in all(entities["Keyboard"]) do
        local label = widgets:create_keyboard(k)
        listen_to(label, { "value" }, playNote)
        table.insert(m.widgets, label)
    end

    for b in all(entities["MenuItem"]) do
        local button = widgets:create_menu_item(b)
        if(button.fields.Item == "Prev") then
            listen_to(button, { "status" }, function(source, value)
                state.instrument = sfx.instrument((state.instrument.index - 1 + 4) % 4)
                if (state.on_change) then
                    state:on_change()
                end
            end)
        elseif button.fields.Item == "Next" then
            listen_to(button, { "status" }, function(source, value)
                state.instrument = sfx.instrument((state.instrument.index + 1) % 4)
                if (state.on_change) then
                    state:on_change()
                end
            end)
        end
        table.insert(m.widgets, button)
    end

    for mode in all(entities["EditorMode"]) do
        local button = widgets:create_mode_switch(mode)
        table.insert(m.widgets, button)
    end

    -- force setting correct values
    if (state.on_change) then
        state:on_change()
    end
end

function _update()
    mouse._update(function()
    end, function()
    end, function()
    end)

    if (ctrl.pressed(keys.space)) then
        state.instrument = sfx.instrument((state.instrument.index + 1) % 4)
        if (state.on_change) then
            state:on_change()
        end
    end

    for w in all(m.widgets) do
        w:_update()
    end
end

function _draw()
    map.draw()

    for w in all(m.widgets) do
        w:_draw()
    end
    mouse._draw()
end