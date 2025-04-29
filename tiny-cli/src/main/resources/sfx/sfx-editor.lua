local widgets = require("widgets")
local mouse = require("mouse")
local wire = require("wire")

local instruments_screen = {
    widgets = {}
}

local m = instruments_screen

local on_menu_item_hover = function(self)
    -- help.label = self.help
    -- TODO: afficher le label quelque part.
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
            wire.produce_to(knob, { "value" }, state, { "instrument", "harmonics", "1" })
            wire.produce_to(state, { "instrument", "harmonics", "1" }, knob, { "value" })
        elseif knob.fields.Label == "Harm2" then
            wire.produce_to(knob, { "value" }, state, { "instrument", "harmonics", "2" })
            wire.produce_to(state, { "instrument", "harmonics", "2" }, knob, { "value" })
        elseif knob.fields.Label == "Harm3" then
            wire.produce_to(knob, { "value" }, state, { "instrument", "harmonics", "3" })
            wire.produce_to(state, { "instrument", "harmonics", "3" }, knob, { "value" })
        elseif knob.fields.Label == "Harm4" then
            wire.produce_to(knob, { "value" }, state, { "instrument", "harmonics", "4" })
            wire.produce_to(state, { "instrument", "harmonics", "4" }, knob, { "value" })
        elseif knob.fields.Label == "Harm5" then
            wire.produce_to(knob, { "value" }, state, { "instrument", "harmonics", "5" })
            wire.produce_to(state, { "instrument", "harmonics", "5" }, knob, { "value" })
        elseif knob.fields.Label == "Harm6" then
            wire.produce_to(knob, { "value" }, state, { "instrument", "harmonics", "6" })
            wire.produce_to(state, { "instrument", "harmonics", "6" }, knob, { "value" })
        elseif knob.fields.Label == "Harm7" then
            wire.produce_to(knob, { "value" }, state, { "instrument", "harmonics", "7" })
            wire.produce_to(state, { "instrument", "harmonics", "7" }, knob, { "value" })
        end
    end

    state.instrument = sfx.instrument(1)

    for k in all(entities["Envelop"]) do
        local envelop = widgets:create_envelop(k)
        envelop.on_hover = on_menu_item_hover

        local f = wire.find_widget(m.widgets, envelop.fields.Attack)
        wire.produce_to(f, { "value" }, state, { "instrument", "attack" })
        wire.produce_to(state, { "instrument", "attack" }, f, { "value" })
        wire.consume_on_update(envelop, { "attack" }, f, { "value" })

        f = wire.find_widget(m.widgets, envelop.fields.Decay)
        wire.produce_to(f, { "value" }, state, { "instrument", "decay" })
        wire.produce_to(state, { "instrument", "decay" }, f, { "value" })
        wire.consume_on_update(envelop, { "decay" }, f, { "value" })

        f = wire.find_widget(m.widgets, envelop.fields.Sustain)
        wire.produce_to(f, { "value" }, state, { "instrument", "sustain" })
        wire.produce_to(state, { "instrument", "sustain" }, f, { "value" })
        wire.consume_on_update(envelop, { "sustain" }, f, { "value" })

        f = wire.find_widget(m.widgets, envelop.fields.Release)
        wire.produce_to(f, { "value" }, state, { "instrument", "release" })
        wire.produce_to(state, { "instrument", "release" }, f, { "value" })
        wire.consume_on_update(envelop, { "release" }, f, { "value" })

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
        return source.fields.Type
    end

    for b in all(entities["Button"]) do
        local button = widgets:create_button(b)
        if (button.fields.Type == "SAVE") then

        else
            wire.produce_to(button, { "status" }, state, { "instrument", "wave" }, buttonToWave)
            wire.consume_on_update(button, { "status" }, state, { "instrument", "wave" }, waveToButton)
        end
        table.insert(m.widgets, button)
    end

    for h in all(entities["InstrumentName"]) do
        local label = widgets:create_help(h)
        wire.consume_on_update(label, { "label" }, state, { "instrument", "name" })
        table.insert(m.widgets, label)
    end

    local playNote = function(source, value)
        state.instrument.play(value)
    end

    for k in all(entities["Keyboard"]) do
        local label = widgets:create_keyboard(k)
        wire.listen_to(label, { "value" }, playNote)
        table.insert(m.widgets, label)
    end

    for b in all(entities["MenuItem"]) do
        local button = widgets:create_menu_item(b)
        if (button.fields.Item == "Prev") then
            wire.listen_to(button, { "status" }, function(source, value)
                state.instrument = sfx.instrument((state.instrument.index - 1 + 4) % 4)
                if (state.on_change) then
                    state:on_change()
                end
            end)
        elseif button.fields.Item == "Next" then
            wire.listen_to(button, { "status" }, function(source, value)
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