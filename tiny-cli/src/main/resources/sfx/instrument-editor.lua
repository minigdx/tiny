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

local InstrumentName = {
    index = 0
}

InstrumentName._update = function(self)

end

InstrumentName._draw = function(self)
    local x, y = 0, 160
    local ox = (self.index % 4) * 16
    local oy = math.floor(self.index / 4) * 16
    spr.sdraw(self.x, self.y, x + ox, y + oy, 16, 16)
end

local state = {
    instrument = nil
}

function _init()
    map.level("InstrumentEditor")
    local entities = map.entities()
    state.instrument = sfx.instrument(1)

    for k in all(entities["Knob"]) do
        local knob = widgets:create_knob(k)
        knob.on_hover = on_menu_item_hover
        table.insert(m.widgets, knob)

        if knob.fields.Label == "Harm1" then
            wire.bind(state, "instrument.harmonics.1", knob, "value")
        elseif knob.fields.Label == "Harm2" then
            wire.bind(state, "instrument.harmonics.2", knob, "value")
        elseif knob.fields.Label == "Harm3" then
            wire.bind(state, "instrument.harmonics.3", knob, "value")
        elseif knob.fields.Label == "Harm4" then
            wire.bind(state, "instrument.harmonics.4", knob, "value")
        elseif knob.fields.Label == "Harm5" then
            wire.bind(state, "instrument.harmonics.5", knob, "value")
        elseif knob.fields.Label == "Harm6" then
            wire.bind(state, "instrument.harmonics.6", knob, "value")
            wire.bind(knob, "value", state, "instrument.harmonics.6")
        elseif knob.fields.Label == "Harm7" then
            wire.bind(state, "instrument.harmonics.7", knob, "value")
        end
    end


    for k in all(entities["Envelop"]) do
        local envelop = widgets:create_envelop(k)
        envelop.on_hover = on_menu_item_hover

        local f = wire.find_widget(m.widgets, envelop.fields.Attack)
        wire.bind(state, "instrument.attack", f, "value")
        wire.bind(state, "instrument.attack", envelop, "attack")

        f = wire.find_widget(m.widgets, envelop.fields.Decay)
        wire.bind(state, "instrument.decay", f, "value")
        wire.bind(state, "instrument.decay", envelop, "decay")

        f = wire.find_widget(m.widgets, envelop.fields.Sustain)
        wire.bind(state, "instrument.sustain", f, "value")
        wire.bind(state, "instrument.sustain", envelop, "sustain")

        f = wire.find_widget(m.widgets, envelop.fields.Release)
        wire.bind(state, "instrument.release", f, "value")
        wire.bind(state, "instrument.release", envelop, "release")
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
            wire.sync(button, "status", state, "instrument.wave", buttonToWave)
            wire.sync(state, "instrument.wave", button, "status", waveToButton, "update")
        end
        table.insert(m.widgets, button)
    end

    for h in all(entities["InstrumentName"]) do
        local label = new(InstrumentName, h)
        wire.sync(state, "instrument.index", label, "index", nil, "update")
        table.insert(m.widgets, label)
    end

    local playNote = function(source, value)
        state.instrument.play(value)
    end

    for k in all(entities["Keyboard"]) do
        local label = widgets:create_keyboard(k)
        wire.listen(label, "value", playNote)
        table.insert(m.widgets, label)
    end

    for b in all(entities["MenuItem"]) do
        local button = widgets:create_menu_item(b)
        if (button.fields.Item == "Prev") then
            wire.listen(button, "status", function(source, value)
                state.instrument = sfx.instrument((state.instrument.index - 1 + 8) % 8)
                if (state.on_change) then
                    state:on_change()
                end
            end)
        elseif button.fields.Item == "Next" then
            wire.listen(button, "status", function(source, value)
                state.instrument = sfx.instrument((state.instrument.index + 1) % 8)
                if (state.on_change) then
                    state:on_change()
                end
            end)
        end
        table.insert(m.widgets, button)
    end

    for mode in all(entities["EditorMode"]) do
        local modeSwitch = widgets:create_mode_switch_component(mode)
        modeSwitch.selected_index = 0
        table.insert(m.widgets, modeSwitch)
    end

    for mode in all(entities["Checkbox"]) do
        local button = widgets:create_checkbox(mode)
        table.insert(m.widgets, button)
    end

    for effect in all(entities["Sweep"]) do
        local active = wire.find_widget(m.widgets, effect.fields.Enabled)
        local acceleration = wire.find_widget(m.widgets, effect.fields.Acceleration)
        local sweep = wire.find_widget(m.widgets, effect.fields.Sweep)

        wire.sync(active, "value", state, "instrument.sweep.active")
        wire.sync(state, "instrument.sweep.active", active, "value", nil, "update")

        wire.sync(acceleration, "value", state, "instrument.sweep.acceleration")
        wire.sync(state, "instrument.sweep.acceleration", acceleration, "value", nil, "update")

        wire.sync(sweep, "value", state, "instrument.sweep.frequency", function(source, target, value)
            return juice.pow2(200, 2000, value)
        end)
        wire.sync(state, "instrument.sweep.frequency", sweep, "value", function(source, target, value)
            return (value - 200) / (2000 - 200)
        end, "update")

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