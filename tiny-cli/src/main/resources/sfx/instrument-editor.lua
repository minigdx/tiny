local widgets = require("widgets")
local mouse = require("mouse")
local wire = require("wire")
local MatrixSelector = require("widgets/MatrixSelector")
local ModeSwitch = require("widgets/ModeSwitch") 

local m = {
    widgets = {}
}

local state = {
    instrument = nil,
    next_note_on = nil,
    next_note_off = nil
}

local on_press = function()
    state.instrument.note_on("C4")
end

local on_release = function()
    state.instrument.note_off("C4")
end

local on_press_repeat = function()
    state.next_note_on = 0
    state.next_note_off = nil
end

local on_release_repeat = function()
    state.next_note_on = nil
end

local on_repeat_update = function()
    if state.next_note_off then
        state.next_note_off = state.next_note_off - tiny.dt
        if state.next_note_off < 0 then
            state.instrument.note_off("C4")
            state.next_note_off = nil

            if state.next_note_on then
                state.next_note_on = state.instrument.release + tiny.dt
            end
        end
    end

    if state.next_note_on and state.next_note_on >= 0 then
        state.next_note_on = state.next_note_on - tiny.dt
        if state.next_note_on < 0 then
            state.instrument.note_on("C4")
            state.next_note_off = state.instrument.attack + state.instrument.decay
        end
    end
end

function _init_knob(entities)
    for k in all(entities["Knob"]) do
        local knob = widgets:create_knob(k)
        table.insert(m.widgets, knob)
    end
end

function _init_envelop(entities)
    for k in all(entities["Envelop"]) do
        local envelop = widgets:create_envelop(k)

        local widget = wire.find_widget(m.widgets, envelop.fields.Attack)
        widget.on_press = on_press_repeat
        widget.on_release = on_release_repeat
        wire.bind(state, "instrument.attack", widget, "value")
        wire.bind(state, "instrument.attack", envelop, "attack")

        widget = wire.find_widget(m.widgets, envelop.fields.Decay)
        widget.on_press = on_press_repeat
        widget.on_release = on_release_repeat
        wire.bind(state, "instrument.decay", widget, "value")
        wire.bind(state, "instrument.decay", envelop, "decay")

        widget = wire.find_widget(m.widgets, envelop.fields.Sustain)
        widget.on_press = on_press
        widget.on_release = on_release
        wire.bind(state, "instrument.sustain", widget, "value")
        wire.bind(state, "instrument.sustain", envelop, "sustain")

        widget = wire.find_widget(m.widgets, envelop.fields.Release)
        widget.on_press = on_press_repeat
        widget.on_release = on_release_repeat
        wire.bind(state, "instrument.release", widget, "value")
        wire.bind(state, "instrument.release", envelop, "release")
        table.insert(m.widgets, envelop)
    end
end

function _init_instrument_matrix(entities)
    for matrix in all(entities["MatrixSelector"]) do
        local widget = new(MatrixSelector, matrix)
        widget:_init()
        wire.sync(state, "instrument.index", widget, "value")
        wire.listen(widget, "value", function(source, value)
            state.instrument = sfx.instrument(value, true)
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

        acceleration.on_press = on_press
        acceleration.on_release = on_release
        sweep.on_press = on_press
        sweep.on_release = on_release

        -- Use manual sync with correct modes for checkboxes
        wire.bind(state, "instrument.sweep.active", active, "value")
        wire.bind(state, "instrument.sweep.acceleration", acceleration, "value")
        wire.bind(state, "instrument.sweep.sweep", sweep, "value")
    end
end

function _init_vibrato(entities)
    for effect in all(entities["Vibrato"]) do
        local active = wire.find_widget(m.widgets, effect.fields.Enabled)
        local frequency = wire.find_widget(m.widgets, effect.fields.Frequency)
        local depth = wire.find_widget(m.widgets, effect.fields.Depth)

        frequency.on_press = on_press
        frequency.on_release = on_release
        depth.on_press = on_press
        depth.on_release = on_release

        -- Use manual sync with correct modes for checkboxes
        wire.bind(state, "instrument.vibrato.active", active, "value")

        wire.bind(state, "instrument.vibrato.frequency", frequency, "value")
        wire.bind(state, "instrument.vibrato.depth", depth, "value")
    end
end

function _init_keyboard(entities)
    local currentNote
    local playNote = function(_, value)
        if value and currentNote == nil then
            state.instrument.note_on(value)
            currentNote = value
        elseif value and currentNote ~= nil then
            state.instrument.note_on(value)
            state.instrument.note_off(currentNote)
            currentNote = value
        elseif not value then
            state.instrument.note_off(currentNote)
            currentNote = nil
        end
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
        return result
    end

    for b in all(entities["WaveTypeSelector"]) do
        local sine = wire.find_widget(m.widgets, b.fields.Sine)
        wire.bind(state, "instrument.wave", sine, "status", buttonToWave("SINE"))
        local square = wire.find_widget(m.widgets, b.fields.Square)
        wire.bind(state, "instrument.wave", square, "status", buttonToWave("SQUARE"))
        local pulse = wire.find_widget(m.widgets, b.fields.Pulse)
        wire.bind(state, "instrument.wave", pulse, "status", buttonToWave("PULSE"))
        local triangle = wire.find_widget(m.widgets, b.fields.Triangle)
        wire.bind(state, "instrument.wave", triangle, "status", buttonToWave("TRIANGLE"))
        local noise = wire.find_widget(m.widgets, b.fields.Noise)
        wire.bind(state, "instrument.wave", noise, "status", buttonToWave("NOISE"))
        local sawtooth = wire.find_widget(m.widgets, b.fields.Sawtooth)
        wire.bind(state, "instrument.wave", sawtooth, "status", buttonToWave("SAW_TOOTH"))
    end
end

local overlays = {
    Sine = { x = 16, y = 16 },
    Pulse = { x = 32, y = 16 },
    Noise = { x = 48, y = 16 },
    Sawtooth = { x = 64, y = 16 },
    Triangle = { x = 80, y = 16 },
    Square = { x = 96, y = 16 },
}

function _init_buttons(entities)
    for b in all(entities["Button"]) do
        local button = widgets:create_button(b)
        button.overlay = overlays[button.fields.WaveType]
        table.insert(m.widgets, button)
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

function _init_harmonics(entities)

    for mode in all(entities["Harmonics"]) do
        for index, harmonic in ipairs(mode.fields.Harmonics) do
            local knob = wire.find_widget(m.widgets, harmonic)
            knob.on_press = on_press
            knob.on_release = on_release
            wire.bind(state, "instrument.harmonics." .. index, knob, "value")
        end
    end
end

function _init_mode_switch(entities)
    for mode in all(entities["ModeButton"]) do
        local button = new(ModeSwitch, mode)
        table.insert(m.widgets, button)
    end
end

function _init()
    map.level("InstrumentEditor")
    local entities = map.entities()
    state.instrument = sfx.instrument(1)

    _init_knob(entities)
    _init_checkbox(entities)
    _init_buttons(entities)
    _init_envelop(entities)
    _init_instrument_matrix(entities)
    _init_wave_type(entities)
    _init_editor_mode(entities)
    _init_sweep(entities)
    _init_vibrato(entities)
    _init_keyboard(entities)
    _init_harmonics(entities)
    _init_mode_switch(entities)

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


    on_repeat_update()
end

function _draw()
    map.draw()

    for w in all(m.widgets) do
        w:_draw()
    end
    mouse._draw()
end
