local widgets = require("widgets")
local mouse = require("mouse")
local wire = require("wire")
local MatrixSelector = require("widgets/MatrixSelector")

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

function _init_knob(entities)
    for k in all(entities["Knob"]) do
        local knob = widgets:create_knob(k)

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
        elseif knob.fields.Label == "Harm7" then
            wire.bind(state, "instrument.harmonics.7", knob, "value")
        end

        table.insert(m.widgets, knob)
    end
end

function _init_envelop(entities)
    for k in all(entities["Envelop"]) do
        local envelop = widgets:create_envelop(k)

        local widget = wire.find_widget(m.widgets, envelop.fields.Attack)
        wire.bind(state, "instrument.attack", widget, "value")
        wire.bind(state, "instrument.attack", envelop, "attack")

        widget = wire.find_widget(m.widgets, envelop.fields.Decay)
        wire.bind(state, "instrument.decay", widget, "value")
        wire.bind(state, "instrument.decay", envelop, "decay")

        widget = wire.find_widget(m.widgets, envelop.fields.Sustain)
        wire.bind(state, "instrument.sustain", widget, "value")
        wire.bind(state, "instrument.sustain", envelop, "sustain")

        widget = wire.find_widget(m.widgets, envelop.fields.Release)
        wire.bind(state, "instrument.release", widget, "value")
        wire.bind(state, "instrument.release", envelop, "release")
        table.insert(m.widgets, envelop)
    end
end

function _init_instrument_matrix(entities)
    for matrix in all(entities["SfxMatrix"]) do
        local widget = new(MatrixSelector, matrix)
        wire.sync(state, "instrument.index", widget, "value")
        wire.listen(widget, "value", function(source, value)
            state.instrument = sfx.instrument(value, true)
            debug.console(state.instrument)
        end)
        wire.sync(state, "instrument.all", widget, "active_indices")
        table.insert(m.widgets, widget)
    end
end

function _init_sweep(entities)
    for effect in all(entities["Sweep"]) do
        local active = wire.find_widget(m.widgets, effect.fields.Enabled)
        local acceleration = wire.find_widget(m.widgets, effect.fields.Acceleration)
        local sweep = wire.find_widget(m.widgets, effect.fields.Sweep)

        wire.bind(state, "instrument.sweep.active", active, "value")
        wire.bind(state, "instrument.sweep.acceleration", acceleration, "value")
        wire.bind(state, "instrument.sweep.frequency", sweep, "value", function(source, target, value)
            return (value - 200) / (2000 - 200)
        end, "update")
    end
end

function _init_keyboard(entities)
    local playNote = function(source, value)
        state.instrument.play(value)
    end

    for k in all(entities["Keyboard"]) do
        local label = widgets:create_keyboard(k)
        wire.listen(label, "value", playNote)
        table.insert(m.widgets, label)
    end
end

function _init_wave_type(entities)
    local buttonToWave = function(type)
        local result = {}
        result.from_widget = function(source, target, value)
            return type
        end
        result.to_widget = function(source, target, value)
            if value == type then
                return 2
            else
                return 0
            end
        end
    end

    local overlays = {
        Sine = {x = 16, y = 16},
        Pulse = {x = 32, y = 16},
        Noise = {x = 48, y = 16},
        Sawtooth = {x = 64, y = 16},
        Triangle = {x = 80, y = 16},
        Square = {x = 96, y = 16},
    }
    for b in all(entities["Button"]) do
        local button = widgets:create_button(b)
        button.overlay = overlays[button.fields.WaveType]
        table.insert(m.widgets, button)
    end

    for b in all(entities["WaveTypeSelector"]) do
        local sine = wire.find_widget(m.widgets, b.fields.Sine)
        wire.bind(sine, "status", state, "instrument.wave", buttonToWave("SINE"))
        local triangle = wire.find_widget(m.widgets, b.fields.Triangle)
        wire.bind(triangle, "status", state, "instrument.wave", buttonToWave("TRIANGLE"))
    end
end

function _init_editor_mode(entities)
    for mode in all(entities["EditorMode"]) do
        local modeSwitch = widgets:create_mode_switch_component(mode)
        modeSwitch.selected_index = 0
        table.insert(m.widgets, modeSwitch)
    end
end

function _init_checkbox(entities)
    for mode in all(entities["Checkbox"]) do
        local button = widgets:create_checkbox(mode)
        table.insert(m.widgets, button)
    end
end

function _init()
    map.level("InstrumentEditor")
    local entities = map.entities()
    state.instrument = sfx.instrument(1)

    _init_knob(entities)
    _init_envelop(entities)
    _init_instrument_matrix(entities)
    _init_wave_type(entities)
    _init_editor_mode(entities)
    _init_checkbox(entities)
    _init_sweep(entities)
    _init_keyboard(entities)

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