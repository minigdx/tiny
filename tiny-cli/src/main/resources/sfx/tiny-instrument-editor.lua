local widgets = require("widgets")
local wire = require("wire")
local EditorBase = require("editor-base")
local LayerManager = require("layers")

local all_widgets = {}
local modals_by_name = {}
local dropdown_widget = nil
local speaker_widgets = {}
local layer_manager = nil

local save_state = nil
local save_button_ref = nil

local state = {
    instrument = nil,
    next_note_on = nil,
    next_note_off = nil,
}

local set_speakers_playing = function(playing)
    for _, s in ipairs(speaker_widgets) do
        s.playing = playing
    end
end

local on_press = function()
    state.instrument.note_on("C4")
    set_speakers_playing(true)
end

local on_release = function()
    state.instrument.note_off("C4")
    set_speakers_playing(false)
end

local on_press_repeat = function()
    state.next_note_on = 0
    state.next_note_off = nil
    set_speakers_playing(true)
end

local on_release_repeat = function()
    state.next_note_on = nil
    set_speakers_playing(false)
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

function _init_knobs(entities)
    for k in all(entities["Knob"]) do
        local knob = widgets:create_knob(k)
        table.insert(all_widgets, knob)
    end
end

function _init_faders(entities)
    for f in all(entities["Fader"]) do
        local fader = widgets:create_fader(f)
        table.insert(all_widgets, fader)
    end
end

function _init_envelop(entities)
    for k in all(entities["Envelope"]) do
        local envelop = widgets:create_envelop(k)

        local widget = wire.find_widget(all_widgets, envelop.fields.Attack)
        widget.on_press = on_press_repeat
        widget.on_release = on_release_repeat
        wire.bind(state, "instrument.attack", widget, "value")
        wire.bind(state, "instrument.attack", envelop, "attack")

        widget = wire.find_widget(all_widgets, envelop.fields.Decay)
        widget.on_press = on_press_repeat
        widget.on_release = on_release_repeat
        wire.bind(state, "instrument.decay", widget, "value")
        wire.bind(state, "instrument.decay", envelop, "decay")

        widget = wire.find_widget(all_widgets, envelop.fields.Sustain)
        widget.on_press = on_press
        widget.on_release = on_release
        wire.bind(state, "instrument.sustain", widget, "value")
        wire.bind(state, "instrument.sustain", envelop, "sustain")

        widget = wire.find_widget(all_widgets, envelop.fields.Release)
        widget.on_press = on_press_repeat
        widget.on_release = on_release_repeat
        wire.bind(state, "instrument.release", widget, "value")
        wire.bind(state, "instrument.release", envelop, "release")

        table.insert(all_widgets, envelop)
    end
end

function _init_harmonics(entities)
    for mode in all(entities["Harmonics"]) do
        for index, harmonic in ipairs(mode.fields.Harmonics) do
            local fader = wire.find_widget(all_widgets, harmonic)
            fader.on_press = on_press
            fader.on_release = on_release
            wire.bind(state, "instrument.harmonics." .. index, fader, "value")
        end
    end
end

function _init_wave_type(entities)
    local buttonToWave = function(wave_type)
        return {
            from_widget = function(source, target, value)
                return wave_type
            end,
            to_widget = function(source, target, value)
                if value == wave_type then
                    return 2
                else
                    return 0
                end
            end,
        }
    end

    for b in all(entities["WaveTypeSelector"]) do
        local sine = wire.find_widget(all_widgets, b.fields.Sine)
        wire.bind(state, "instrument.wave", sine, "status", buttonToWave("SINE"))
        local square = wire.find_widget(all_widgets, b.fields.Square)
        wire.bind(state, "instrument.wave", square, "status", buttonToWave("SQUARE"))
        local pulse = wire.find_widget(all_widgets, b.fields.Pulse)
        wire.bind(state, "instrument.wave", pulse, "status", buttonToWave("PULSE"))
        local triangle = wire.find_widget(all_widgets, b.fields.Triangle)
        wire.bind(state, "instrument.wave", triangle, "status", buttonToWave("TRIANGLE"))
        local noise = wire.find_widget(all_widgets, b.fields.Noise)
        wire.bind(state, "instrument.wave", noise, "status", buttonToWave("NOISE"))
        local sawtooth = wire.find_widget(all_widgets, b.fields.Sawtooth)
        wire.bind(state, "instrument.wave", sawtooth, "status", buttonToWave("SAW_TOOTH"))
        local drum = wire.find_widget(all_widgets, b.fields.Drum)
        wire.bind(state, "instrument.wave", drum, "status", buttonToWave("DRUM"))
    end
end

function _init_keyboard(entities)
    local currentNote
    local playNote = function(_, value)
        if value and currentNote == nil then
            state.instrument.note_on(value)
            currentNote = value
            set_speakers_playing(true)
        elseif value and currentNote ~= nil then
            state.instrument.note_on(value)
            state.instrument.note_off(currentNote)
            currentNote = value
        elseif not value then
            state.instrument.note_off(currentNote)
            currentNote = nil
            set_speakers_playing(false)
        end
    end

    for k in all(entities["Keyboard"]) do
        local keyboard = widgets:create_keyboard(k)
        wire.listen(keyboard, "value", playNote)
        table.insert(all_widgets, keyboard)
    end
end

function _init()
    all_widgets = {}
    modals_by_name = {}
    dropdown_widget = nil
    speaker_widgets = {}
    layer_manager = nil
    save_state = nil
    save_button_ref = nil

    map.level("InstrumentEditor")
    state.instrument = sfx.instrument(0)

    -- Panels first (drawn behind everything)
    local panel_entities = map.entities("Panels")
    EditorBase.init_panels(panel_entities, all_widgets)

    -- Then all interactive widgets
    local widget_entities = map.entities("Widgets")

    local buttons_by_action = EditorBase.init_text_buttons(widget_entities, all_widgets)
    save_button_ref = buttons_by_action["Save"]

    EditorBase.init_speakers(widget_entities, all_widgets, speaker_widgets)
    _init_knobs(widget_entities)
    _init_faders(widget_entities)

    modals_by_name = EditorBase.init_buttons(widget_entities, all_widgets, {
        on_open = function() return state.instrument.name end,
        on_name_validate = function(value)
            if value and state.instrument then
                state.instrument.name = value
                EditorBase.update_dropdown_name(dropdown_widget, value)
            end
        end,
    })

    _init_wave_type(widget_entities)

    dropdown_widget = EditorBase.init_entity_dropdown(widget_entities, all_widgets, {
        count = 8,
        fetch = function(i) return sfx.instrument(i) end,
        label = "Instrument",
        on_select = function(index) state.instrument = sfx.instrument(index) end,
        layer_manager = nil, -- set after layer_manager is created
    })

    EditorBase.init_mode_switch(widget_entities, all_widgets)
    _init_keyboard(widget_entities)
    _init_envelop(widget_entities)
    _init_harmonics(widget_entities)

    layer_manager = LayerManager.create()
    layer_manager:register("Widgets", { tiles = nil, widgets = all_widgets, always = true })

    -- Wire dropdown overlay after layer_manager is created
    if dropdown_widget then
        local original_update = dropdown_widget._update
        dropdown_widget._update = function(self)
            local was_open = self.open
            original_update(self)
            if self.open and not was_open then
                layer_manager:set_overlay(self)
            elseif not self.open and was_open then
                layer_manager:set_overlay(nil)
            end
        end
    end

    save_state = EditorBase.init_save_reminder(all_widgets, save_button_ref, modals_by_name)
end

function _update()
    EditorBase.update(modals_by_name, function()
        layer_manager:update_widgets()
    end)

    EditorBase.update_save_reminder(save_button_ref, save_state)

    on_repeat_update()
end

function _draw()
    EditorBase.draw(function()
        layer_manager:draw_base()
        layer_manager:draw_active()
    end, modals_by_name)
end
