local widgets = require("widgets")
local mouse = require("mouse")
local wire = require("wire")
local ModeSwitch = require("widgets/ModeSwitch")
local LayerManager = require("layers")

local all_widgets = {}
local modals_by_name = {}
local dropdown_widget = nil

local layer_widgets = {}
local layer_buttons = {}
local active_layer = nil
local layer_manager = nil

local state = {
    instrument = nil,
    next_note_on = nil,
    next_note_off = nil,
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

function _switch_layer(layer_name)
    for name, button in pairs(layer_buttons) do
        if name == layer_name then
            button.status = 2
        else
            button.status = 0
        end
    end
    active_layer = layer_name
    if layer_manager then
        layer_manager:switch(layer_name)
    end
end

function _init_layer_buttons(entities)
    for b in all(entities["Button"]) do
        if b.fields.Layer ~= nil then
            local layer_name = b.fields.Layer
            local button = widgets:create_button(b)
            layer_buttons[layer_name] = button
            table.insert(all_widgets, button)

            button.on_change = function()
                _switch_layer(layer_name)
            end
        end
    end
end

function _init_buttons(entities)
    for b in all(entities["Button"]) do
        if b.fields.Layer == nil then
            local button = widgets:create_button(b)

            if button.fields.Modal then
                local modal_name = button.fields.Modal

                if not modals_by_name[modal_name] then
                    local modal = widgets:create_modal({
                        x = 96,
                        y = 64,
                        width = 192,
                        height = 128,
                        level_name = modal_name,
                        fields = {},
                    })
                    modals_by_name[modal_name] = modal
                end

                button.on_change = function()
                    local target = modals_by_name[modal_name]
                    if target then
                        target:open(state.instrument.name)
                    end
                end
            end

            table.insert(all_widgets, button)
        end
    end

    local name_modal = modals_by_name["NameModal"]
    if name_modal then
        name_modal.on_validate = function(self, value)
            if value and state.instrument then
                state.instrument.name = value
                if dropdown_widget then
                    local idx = dropdown_widget.selected
                    dropdown_widget.options[idx] = "[" .. (idx - 1) .. "] " .. value
                    dropdown_widget:_init()
                end
            end
        end
    end
end

function _init_dropdowns(entities)
    for d in all(entities["Dropdown"]) do
        local dropdown = widgets:create_dropdown(d)

        if #dropdown.options == 0 then
            for i = 0, 7 do
                local instr = sfx.instrument(i)
                local name = instr.name or ("Instrument " .. i)
                table.insert(dropdown.options, "[" .. i .. "] " .. name)
            end
            dropdown:_init()
        end

        dropdown.on_change = function(self)
            state.instrument = sfx.instrument(self.selected - 1)
        end

        dropdown_widget = dropdown

        local original_update = dropdown._update
        dropdown._update = function(self)
            local was_open = self.open
            original_update(self)
            if layer_manager then
                if self.open and not was_open then
                    layer_manager:set_overlay(self)
                elseif not self.open and was_open then
                    layer_manager:set_overlay(nil)
                end
            end
        end

        table.insert(all_widgets, dropdown)
    end
end

function _init_mode_switch(entities)
    for mode in all(entities["ModeButton"]) do
        local button = new(ModeSwitch, mode)
        table.insert(all_widgets, button)
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
        local keyboard = widgets:create_keyboard(k)
        wire.listen(keyboard, "value", playNote)
        table.insert(all_widgets, keyboard)
    end
end

function _init_waveform(waveform_entities)
    layer_widgets["Waveform"] = {}

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

    for b in all(waveform_entities["Button"]) do
        local button = widgets:create_button(b)
        table.insert(layer_widgets["Waveform"], button)
    end

    for b in all(waveform_entities["WaveTypeSelector"]) do
        local sine = wire.find_widget(layer_widgets["Waveform"], b.fields.Sine)
        wire.bind(state, "instrument.wave", sine, "status", buttonToWave("SINE"))
        local triangle = wire.find_widget(layer_widgets["Waveform"], b.fields.Triangle)
        wire.bind(state, "instrument.wave", triangle, "status", buttonToWave("TRIANGLE"))
        local pulse = wire.find_widget(layer_widgets["Waveform"], b.fields.Pulse)
        wire.bind(state, "instrument.wave", pulse, "status", buttonToWave("PULSE"))
        local noise = wire.find_widget(layer_widgets["Waveform"], b.fields.Noise)
        wire.bind(state, "instrument.wave", noise, "status", buttonToWave("NOISE"))
        local square = wire.find_widget(layer_widgets["Waveform"], b.fields.Square)
        wire.bind(state, "instrument.wave", square, "status", buttonToWave("SQUARE"))
        local sawtooth = wire.find_widget(layer_widgets["Waveform"], b.fields.Sawtooth)
        wire.bind(state, "instrument.wave", sawtooth, "status", buttonToWave("SAW_TOOTH"))
    end
end

function _init_envelope(envelope_entities)
    layer_widgets["Envelope"] = {}

    for k in all(envelope_entities["Knob"]) do
        local knob = widgets:create_knob(k)
        table.insert(layer_widgets["Envelope"], knob)
    end

    for k in all(envelope_entities["Envelope"]) do
        local envelop = widgets:create_envelop(k)

        local widget = wire.find_widget(layer_widgets["Envelope"], k.fields.Attack)
        widget.on_press = on_press_repeat
        widget.on_release = on_release_repeat
        wire.bind(state, "instrument.attack", widget, "value")
        wire.bind(state, "instrument.attack", envelop, "attack")

        widget = wire.find_widget(layer_widgets["Envelope"], k.fields.Decay)
        widget.on_press = on_press_repeat
        widget.on_release = on_release_repeat
        wire.bind(state, "instrument.decay", widget, "value")
        wire.bind(state, "instrument.decay", envelop, "decay")

        widget = wire.find_widget(layer_widgets["Envelope"], k.fields.Sustain)
        widget.on_press = on_press
        widget.on_release = on_release
        wire.bind(state, "instrument.sustain", widget, "value")
        wire.bind(state, "instrument.sustain", envelop, "sustain")

        widget = wire.find_widget(layer_widgets["Envelope"], k.fields.Release)
        widget.on_press = on_press_repeat
        widget.on_release = on_release_repeat
        wire.bind(state, "instrument.release", widget, "value")
        wire.bind(state, "instrument.release", envelop, "release")

        table.insert(layer_widgets["Envelope"], envelop)
    end
end

function _init_harmonics(harmonics_entities)
    layer_widgets["Harmonics"] = {}

    for k in all(harmonics_entities["Knob"]) do
        local knob = widgets:create_knob(k)
        table.insert(layer_widgets["Harmonics"], knob)
    end

    for mode in all(harmonics_entities["Harmonics"]) do
        for index, harmonic in ipairs(mode.fields.Harmonics) do
            local knob = wire.find_widget(layer_widgets["Harmonics"], harmonic)
            knob.on_press = on_press
            knob.on_release = on_release
            wire.bind(state, "instrument.harmonics." .. index, knob, "value")
        end
    end
end

function _init()
    all_widgets = {}
    modals_by_name = {}
    dropdown_widget = nil
    layer_widgets = {}
    layer_buttons = {}
    active_layer = nil
    layer_manager = nil

    map.level("InstrumentEditor")
    local widget_entities = map.entities("Widgets")
    local waveform_entities = map.entities("Waveform")
    local envelope_entities = map.entities("Envelope")
    local harmonics_entities = map.entities("Harmonics")

    state.instrument = sfx.instrument(0)

    _init_layer_buttons(widget_entities)
    _init_buttons(widget_entities)
    _init_dropdowns(widget_entities)
    _init_mode_switch(widget_entities)
    _init_keyboard(widget_entities)
    _init_waveform(waveform_entities)
    _init_envelope(envelope_entities)
    _init_harmonics(harmonics_entities)

    layer_widgets["Modulation"] = {}

    layer_manager = LayerManager.create()
    layer_manager:register("Widgets",    { tiles = "WidgetsTiles",   widgets = all_widgets,              always = true })
    layer_manager:register("Envelope",   { tiles = "EnvelopeTiles",  widgets = layer_widgets["Envelope"] })
    layer_manager:register("Waveform",   { tiles = "WaveformTiles",  widgets = layer_widgets["Waveform"] })
    layer_manager:register("Harmonics",  { tiles = "HarmonicsTiles", widgets = layer_widgets["Harmonics"] })
    layer_manager:register("Modulation", { tiles = nil,              widgets = layer_widgets["Modulation"] })

    _switch_layer("Waveform")
end

function _update()
    mouse._update(function() end, function() end, function() end)

    local active_modal
    for _, modal in pairs(modals_by_name) do
        if modal.visible then
            active_modal = modal
            break
        end
    end

    if active_modal then
        active_modal:_update()
    else
        layer_manager:update_widgets()
    end

    on_repeat_update()
end

function _draw()
    layer_manager:draw_base()
    layer_manager:draw_active()
    -- Draw modals last so they appear on top of everything
    for _, modal in pairs(modals_by_name) do
        modal:_draw()
    end
    mouse._draw()
end
