local widgets = require("widgets")
local mouse = require("mouse")
local wire = require("wire")
local ModeSwitch = require("widgets/ModeSwitch")
local LayerManager = require("layers")

local all_widgets = {}
local modals_by_name = {}
local dropdown_widget = nil
local speaker_widgets = {}
local layer_manager = nil

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

function _init_panels(entities)
    for p in all(entities["Panel"]) do
        local panel = widgets:create_panel(p)
        table.insert(all_widgets, panel)
    end
end

function _init_text_buttons(entities)
    for tb in all(entities["TextButton"]) do
        local text_button = widgets:create_text_button(tb)
        table.insert(all_widgets, text_button)
    end
end

function _init_speakers(entities)
    for s in all(entities["Speaker"]) do
        local speaker = widgets:create_speaker(s)
        table.insert(all_widgets, speaker)
        table.insert(speaker_widgets, speaker)
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

function _init_buttons(entities)
    for b in all(entities["Button"]) do
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
    for mode in all(entities["ModeSwitch"]) do
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

    map.level("InstrumentEditor")
    state.instrument = sfx.instrument(0)

    -- Panels first (drawn behind everything)
    local panel_entities = map.entities("Panels")
    _init_panels(panel_entities)

    -- Then all interactive widgets
    local widget_entities = map.entities("Widgets")
    _init_text_buttons(widget_entities)
    _init_speakers(widget_entities)
    _init_knobs(widget_entities)
    _init_faders(widget_entities)
    _init_buttons(widget_entities)
    _init_dropdowns(widget_entities)
    _init_mode_switch(widget_entities)
    _init_keyboard(widget_entities)
    _init_envelop(widget_entities)
    _init_harmonics(widget_entities)

    layer_manager = LayerManager.create()
    layer_manager:register("Widgets", { tiles = nil, widgets = all_widgets, always = true })
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
    gfx.cls()
    map.draw("Background")
    layer_manager:draw_base()
    layer_manager:draw_active()
    for _, modal in pairs(modals_by_name) do
        modal:_draw()
    end
    mouse._draw()
end
