local mouse = require("mouse")
local widgets = require("widgets")
local ModeSwitch = require("widgets/ModeSwitch")
local Dropdown = require("widgets/Dropdown")
local Counter = require("widgets/Counter")
local Fader = require("widgets/Fader")
local TextButton = require("widgets/TextButton")
local Panel = require("widgets/Panel")
local Speaker = require("widgets/Speaker")
local music_templates = require("music-templates")
local utils = require("widgets.utils")

local all_widgets = {}
local overlay_widget = nil
local speaker_widgets = {}

local yellow = 9
local orange = 8
local light_green = 6
local pink = 7
local purple = 4

local state = {
    seq = nil,
    seq_index = 0,
    playing = false,
    handler = nil,
}

local config = {
    root = "C",
    scale_name = "Major",
    progression_name = "Classic",
    lead_style = "Stepwise",
    drum_pattern = "Rock",
    chord_instrument = 0,
    bass_instrument = 4,
    lead_instrument = 5,
    drum_instrument = 3,
    bpm = 120,
    chord_volume = 0.6,
    bass_volume = 0.8,
    lead_volume = 0.5,
    drum_volume = 0.7,
}

local play_button = nil

local function make_entity(x, y, w, h, fields)
    return {
        x = x,
        y = y,
        width = w,
        height = h,
        fields = fields or {},
        iid = tostring(math.random(100000, 999999)),
    }
end

local function make_dropdown(x, y, w, options, selected, label)
    local d = new(Dropdown, make_entity(x, y, w, 16, {}))
    d.options = options
    d.selected = selected or 1
    d:_init()

    local original_update = d._update
    d._update = function(self)
        local was_open = self.open
        original_update(self)
        if self.open and not was_open then
            overlay_widget = self
        elseif not self.open and was_open then
            if overlay_widget == self then
                overlay_widget = nil
            end
        end
    end

    return d
end

local function make_fader(x, y, h, value, label)
    local f = new(Fader, make_entity(x, y, 8, h, { Label = label }))
    f.label = label or ""
    f.value = value or 0.5
    return f
end

local function make_counter(x, y, w, value, min_v, max_v)
    local c = new(Counter, make_entity(x, y, w, 16, {}))
    c.value = value or 0
    c.min = min_v or 0
    c.max = max_v or 7
    return c
end

local function make_text_button(x, y, w, label, variant)
    local vmap = utils.variant_mapping[variant] or 0
    local b = new(TextButton, make_entity(x, y, w, 16, {
        Label = label,
        Variant = variant,
        IsActive = false,
    }))
    b.label = label
    b.variant = vmap
    return b
end

local function make_panel(x, y, w, h, label, variant)
    local vmap = utils.variant_mapping[variant or "LigthBlue"] or 0
    local p = new(Panel, make_entity(x, y, w, h, {
        Label = label,
        Variant = variant or "LigthBlue",
    }))
    p.label = label
    p.variant = vmap
    return p
end

local function load_sequence()
    state.seq = sfx.sequence(state.seq_index)
end

local function get_instrument_options()
    local opts = {}
    for i = 0, 15 do
        local inst = sfx.instrument(i)
        if inst then
            table.insert(opts, "[" .. i .. "] " .. (inst.name or ("Inst " .. i)))
        else
            table.insert(opts, "[" .. i .. "] ---")
        end
    end
    return opts
end

local function stop_playback()
    if state.handler then
        state.handler.stop()
        state.handler = nil
    end
    state.playing = false
    for _, s in ipairs(speaker_widgets) do
        s.playing = false
    end
    if play_button then
        play_button.label = "Play"
    end
end

local function start_playback()
    stop_playback()
    if state.seq then
        state.handler = state.seq:play()
        state.playing = true
        for _, s in ipairs(speaker_widgets) do
            s.playing = true
        end
        if play_button then
            play_button.label = "Stop"
        end
    end
end

local function toggle_playback()
    if state.playing then
        stop_playback()
    else
        start_playback()
    end
end

function _init()
    all_widgets = {}
    overlay_widget = nil
    speaker_widgets = {}

    load_sequence()

    -- Panels
    table.insert(all_widgets, make_panel(0, 0, 384, 24, nil, "LigthBlue"))
    table.insert(all_widgets, make_panel(4, 28, 164, 118, "Generation", "LigthBlue"))
    table.insert(all_widgets, make_panel(4, 148, 164, 100, "Instruments", "LigthBlue"))
    table.insert(all_widgets, make_panel(172, 28, 208, 120, "Tracks", "LigthBlue"))
    table.insert(all_widgets, make_panel(172, 152, 208, 96, nil, "LigthBlue"))

    -- Mode Switch
    local mode = new(ModeSwitch, make_entity(176, 8, 32, 8, { Active = "Music" }))
    table.insert(all_widgets, mode)

    -- Save button
    local save_btn = make_text_button(4, 4, 44, "Save", "Red")
    save_btn.on_change = function()
        sfx.save()
    end
    table.insert(all_widgets, save_btn)

    -- Generate button
    local gen_btn = make_text_button(52, 4, 64, "Generate", "Green")
    gen_btn.on_change = function()
        if state.seq then
            music_templates.generate(state.seq, config)
        end
    end
    table.insert(all_widgets, gen_btn)

    -- Play/Stop button
    play_button = make_text_button(120, 4, 44, "Play", "HardBlue")
    play_button.on_change = function()
        toggle_playback()
    end
    table.insert(all_widgets, play_button)

    -- Root note dropdown
    local root_dd = make_dropdown(8, 36, 76, music_templates.root_notes, 1, "Root")
    root_dd.on_change = function(self)
        config.root = music_templates.root_notes[self.selected]
    end
    table.insert(all_widgets, root_dd)

    -- Scale dropdown
    local scale_dd = make_dropdown(88, 36, 76, music_templates.scale_names, 1, "Scale")
    scale_dd.on_change = function(self)
        config.scale_name = music_templates.scale_names[self.selected]
    end
    table.insert(all_widgets, scale_dd)

    -- Chord progression dropdown
    local prog_dd = make_dropdown(8, 56, 156, music_templates.progression_names, 1, "Chords")
    prog_dd.on_change = function(self)
        config.progression_name = music_templates.progression_names[self.selected]
    end
    table.insert(all_widgets, prog_dd)

    -- Lead style dropdown
    local lead_dd = make_dropdown(8, 76, 156, music_templates.lead_styles, 1, "Lead")
    lead_dd.on_change = function(self)
        config.lead_style = music_templates.lead_styles[self.selected]
    end
    table.insert(all_widgets, lead_dd)

    -- Drum pattern dropdown
    local drum_dd = make_dropdown(8, 96, 156, music_templates.drum_pattern_names, 1, "Drums")
    drum_dd.on_change = function(self)
        config.drum_pattern = music_templates.drum_pattern_names[self.selected]
    end
    table.insert(all_widgets, drum_dd)

    -- BPM counter
    local bpm_ctr = make_counter(8, 120, 72, 120, 40, 300)
    bpm_ctr.on_change = function(self)
        config.bpm = self.value
    end
    table.insert(all_widgets, bpm_ctr)

    -- Sequence index counter
    local seq_ctr = make_counter(88, 120, 72, 0, 0, 7)
    seq_ctr.on_change = function(self)
        stop_playback()
        state.seq_index = self.value
        load_sequence()
    end
    table.insert(all_widgets, seq_ctr)

    -- Instrument dropdowns
    local inst_opts = get_instrument_options()

    local chord_inst_dd = make_dropdown(8, 156, 156, inst_opts, config.chord_instrument + 1, "Chord Inst")
    chord_inst_dd.on_change = function(self)
        config.chord_instrument = self.selected - 1
    end
    table.insert(all_widgets, chord_inst_dd)

    local bass_inst_dd = make_dropdown(8, 176, 156, inst_opts, config.bass_instrument + 1, "Bass Inst")
    bass_inst_dd.on_change = function(self)
        config.bass_instrument = self.selected - 1
    end
    table.insert(all_widgets, bass_inst_dd)

    local lead_inst_dd = make_dropdown(8, 196, 156, inst_opts, config.lead_instrument + 1, "Lead Inst")
    lead_inst_dd.on_change = function(self)
        config.lead_instrument = self.selected - 1
    end
    table.insert(all_widgets, lead_inst_dd)

    local drum_inst_dd = make_dropdown(8, 216, 156, inst_opts, config.drum_instrument + 1, "Drum Inst")
    drum_inst_dd.on_change = function(self)
        config.drum_instrument = self.selected - 1
    end
    table.insert(all_widgets, drum_inst_dd)

    -- Track volume faders
    local track_names = { "Chord", "Bass", "Lead", "Drums" }
    local track_volumes = { config.chord_volume, config.bass_volume, config.lead_volume, config.drum_volume }
    local volume_keys = { "chord_volume", "bass_volume", "lead_volume", "drum_volume" }

    for i = 1, 4 do
        local fx = 184 + (i - 1) * 48
        local fader = make_fader(fx, 48, 40, track_volumes[i], track_names[i])
        fader.on_change = function(self)
            config[volume_keys[i]] = self.value
        end
        table.insert(all_widgets, fader)
    end

    -- Labels for tracks
    local label_y = 92
    for i = 1, 4 do
        local lx = 180 + (i - 1) * 48
        table.insert(all_widgets, make_panel(lx, label_y, 40, 16, track_names[i], "LigthBlue"))
    end

    -- Speakers
    local spk1 = new(Speaker, make_entity(180, 160, 98, 80, {}))
    table.insert(all_widgets, spk1)
    table.insert(speaker_widgets, spk1)

    local spk2 = new(Speaker, make_entity(282, 160, 98, 80, {}))
    table.insert(all_widgets, spk2)
    table.insert(speaker_widgets, spk2)

    -- Track beat visualizer (shows which beats have notes)
end

function _update()
    mouse._update(function() end, function() end, function() end)

    if state.playing and state.handler then
        if not state.handler.playing then
            stop_playback()
        end
    end

    if ctrl.pressed(keys.space) then
        toggle_playback()
    end

    if overlay_widget then
        overlay_widget:_update()
    else
        for w in all(all_widgets) do
            w:_update()
        end
    end
end

function _draw()
    gfx.cls(1)

    for w in all(all_widgets) do
        if w ~= overlay_widget then
            w:_draw()
        end
    end

    if overlay_widget then
        overlay_widget:_draw()
    end

    -- Draw labels
    gfx.to_sheet(0)
    print("Root", 10, 32, 12)
    print("Scale", 90, 32, 12)
    print("Progression", 10, 52, 12)
    print("Lead", 10, 72, 12)
    print("Drums", 10, 92, 12)
    print("BPM", 10, 116, 12)
    print("Seq", 90, 116, 12)
    print("Chord Inst", 10, 152, 12)
    print("Bass Inst", 10, 172, 12)
    print("Lead Inst", 10, 192, 12)
    print("Drum Inst", 10, 212, 12)

    mouse._draw()
end
