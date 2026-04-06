local widgets = require("widgets")
local wire = require("wire")
local EditorBase = require("editor-base")
local icons = require("widgets.icons")

local all_widgets = {}
local modals_by_name = {}
local speaker_widgets = {}
local overlay_widget = nil

local save_state = nil
local save_button_ref = nil
local selector_dd_ref = nil
local play_button_ref = nil

local state = {
    seq = nil,
    seq_index = 0,
}

local playing = false
local play_handler = nil
local master_volume = 1.0

-- Track configuration: instruments, volumes, speed
local config = {
    chord_instrument = 0,
    bass_instrument = 1,
    lead_instrument = 2,
    drum_instrument = 3,
    chord_volume = 0.3,
    bass_volume = 0.4,
    lead_volume = 0.25,
    drum_volume = 0.35,
    bpm = 120,
}

local function wrap_dropdown_overlay(dropdown)
    local original_update = dropdown._update
    dropdown._update = function(self)
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
end

local function build_instrument_options()
    local options = {}
    for i = 0, 7 do
        local inst = sfx.instrument(i)
        local name = (inst and inst.name) or ("Instrument " .. i)
        table.insert(options, "[" .. i .. "] " .. name)
    end
    return options
end

local function populate_dropdown(dd, options, default_index)
    if not dd then return end
    dd.options = options
    dd.selected = default_index or 1
    dd:_init()
end

local function build_seq_label(index)
    local seq = sfx.sequence(index)
    local name = seq and seq.name
    if name and name ~= "" then
        return "[" .. index .. "] " .. name
    end
    return "Seq " .. index
end

-- Apply config to sequence tracks
local function apply_config_to_seq()
    if not state.seq then return end
    local instruments = { config.lead_instrument, config.chord_instrument, config.bass_instrument, config.drum_instrument }
    local volumes = { config.lead_volume, config.chord_volume, config.bass_volume, config.drum_volume }
    for i = 0, 3 do
        local track = state.seq.track(i)
        if track then
            track.instrument = instruments[i + 1]
            track.volume = volumes[i + 1] * master_volume
        end
    end
    state.seq.tempo = config.bpm
    state.seq.config = nil
    state.seq.invalidate()
end

-- Load config from sequence tracks (with backward compat for auto-gen sequences)
local function load_config_from_seq()
    if not state.seq then return end

    -- Try legacy saved config first (backward compat with auto-gen sequences)
    local saved_config = state.seq.config
    if saved_config then
        config.chord_instrument = saved_config.chord_instrument or config.chord_instrument
        config.bass_instrument = saved_config.bass_instrument or config.bass_instrument
        config.lead_instrument = saved_config.lead_instrument or config.lead_instrument
        config.drum_instrument = saved_config.drum_instrument or config.drum_instrument
        config.chord_volume = saved_config.chord_volume or config.chord_volume
        config.bass_volume = saved_config.bass_volume or config.bass_volume
        config.lead_volume = saved_config.lead_volume or config.lead_volume
        config.drum_volume = saved_config.drum_volume or config.drum_volume
        config.bpm = saved_config.bpm or config.bpm
        -- Clear config to use pre-computed path from now on
        state.seq.config = nil
    else
        -- Load from tracks directly
        local track0 = state.seq.track(0)
        local track1 = state.seq.track(1)
        local track2 = state.seq.track(2)
        local track3 = state.seq.track(3)
        if track0 then
            config.lead_instrument = track0.instrument
            config.lead_volume = track0.volume
        end
        if track1 then
            config.chord_instrument = track1.instrument
            config.chord_volume = track1.volume
        end
        if track2 then
            config.bass_instrument = track2.instrument
            config.bass_volume = track2.volume
        end
        if track3 then
            config.drum_instrument = track3.instrument
            config.drum_volume = track3.volume
        end
    end
    config.bpm = state.seq.tempo or config.bpm
end

-- Remove a widget from all_widgets
local function remove_widget(widget)
    if not widget then return end
    for i = #all_widgets, 1, -1 do
        if all_widgets[i] == widget then
            table.remove(all_widgets, i)
            break
        end
    end
end

-- Update UI controls to match current config
local function sync_ui(refs)
    if refs.chord_inst_dd then
        refs.chord_inst_dd:set_selected(config.chord_instrument + 1)
    end
    if refs.bass_inst_dd then
        refs.bass_inst_dd:set_selected(config.bass_instrument + 1)
    end
    if refs.lead_inst_dd then
        refs.lead_inst_dd:set_selected(config.lead_instrument + 1)
    end
    if refs.drum_inst_dd then
        refs.drum_inst_dd:set_selected(config.drum_instrument + 1)
    end
    if refs.chord_volume_fader then
        refs.chord_volume_fader.value = config.chord_volume
    end
    if refs.bass_volume_fader then
        refs.bass_volume_fader.value = config.bass_volume
    end
    if refs.lead_volume_fader then
        refs.lead_volume_fader.value = config.lead_volume
    end
    if refs.drum_volume_fader then
        refs.drum_volume_fader.value = config.drum_volume
    end
    if refs.speed_fader then
        refs.speed_fader.value = (config.bpm - 60) / 200
    end
end

local function _init_music_generator(widget_entities)
    local generators = widget_entities["MusicGenerator"]
    if not generators then return end

    local gen = nil
    for g in all(generators) do
        gen = g
        break
    end
    if not gen then return end

    -- Find referenced widgets
    local drum_pattern_dd = wire.find_widget(all_widgets, gen.fields.DrumPattern)
    local drum_volume_fader = wire.find_widget(all_widgets, gen.fields.DrumVolume)
    local theme_dd = wire.find_widget(all_widgets, gen.fields.MusicTheme)
    local scale_dd = wire.find_widget(all_widgets, gen.fields.MusicScale)
    local lead_inst_dd = wire.find_widget(all_widgets, gen.fields.LeadInstrument)
    local lead_volume_fader = wire.find_widget(all_widgets, gen.fields.LeadVolume)
    local bass_inst_dd = wire.find_widget(all_widgets, gen.fields.BassInstrument)
    local bass_volume_fader = wire.find_widget(all_widgets, gen.fields.BassVolume)
    local chord_volume_fader = wire.find_widget(all_widgets, gen.fields.RythmVolume)
    local progression_dd = wire.find_widget(all_widgets, gen.fields.RythmChordProgression)
    local chord_inst_dd = wire.find_widget(all_widgets, gen.fields.RythmInstrument)
    local play_button = wire.find_widget(all_widgets, gen.fields.Play)
    local master_volume_fader = wire.find_widget(all_widgets, gen.fields.Volume)
    local selector_dd = wire.find_widget(all_widgets, gen.fields.Selector)
    local export_button = wire.find_widget(all_widgets, gen.fields.Export)
    local speed_fader = gen.fields.Speed and wire.find_widget(all_widgets, gen.fields.Speed) or nil
    local rhythm_style_dd = gen.fields.RythmArpegiator and wire.find_widget(all_widgets, gen.fields.RythmArpegiator) or nil

    selector_dd_ref = selector_dd
    play_button_ref = play_button

    -- Find unreferenced dropdowns by position (lead style, root note)
    local referenced_iids = {}
    for _, field_name in ipairs({
        "DrumPattern", "DrumVolume", "MusicTheme", "MusicScale",
        "LeadInstrument", "LeadVolume", "BassInstrument", "BassVolume",
        "RythmVolume", "RythmChordProgression", "RythmInstrument",
        "RythmArpegiator",
        "Play", "Volume", "Selector", "Export", "Speed",
    }) do
        local ref = gen.fields[field_name]
        if ref then
            referenced_iids[ref.entityIid] = true
        end
    end
    local lead_style_dd = nil
    local root_dd = nil
    for w in all(all_widgets) do
        if w.options and not referenced_iids[w.iid] then
            if w.x >= 18 and w.x <= 24 and w.y >= 69 and w.y <= 75 then
                lead_style_dd = w
            elseif w.x >= 18 and w.x <= 24 and w.y >= 109 and w.y <= 115 then
                root_dd = w
            end
        end
    end

    -- Remove auto-generation widgets (no longer needed)
    remove_widget(theme_dd)
    remove_widget(scale_dd)
    remove_widget(progression_dd)
    remove_widget(lead_style_dd)
    remove_widget(rhythm_style_dd)
    remove_widget(root_dd)

    -- Repurpose DrumPattern dropdown as DrumInstrument
    local drum_inst_dd = drum_pattern_dd

    -- Build option lists
    local inst_options = build_instrument_options()

    local seq_options = {}
    for i = 0, 7 do
        table.insert(seq_options, build_seq_label(i))
    end

    -- Load config from current sequence
    load_config_from_seq()

    -- Widget refs for sync_ui
    local refs = {
        chord_inst_dd = chord_inst_dd,
        bass_inst_dd = bass_inst_dd,
        lead_inst_dd = lead_inst_dd,
        drum_inst_dd = drum_inst_dd,
        chord_volume_fader = chord_volume_fader,
        bass_volume_fader = bass_volume_fader,
        lead_volume_fader = lead_volume_fader,
        drum_volume_fader = drum_volume_fader,
        speed_fader = speed_fader,
    }

    -- Populate dropdowns
    populate_dropdown(chord_inst_dd, inst_options, config.chord_instrument + 1)
    populate_dropdown(bass_inst_dd, inst_options, config.bass_instrument + 1)
    populate_dropdown(lead_inst_dd, inst_options, config.lead_instrument + 1)
    populate_dropdown(drum_inst_dd, inst_options, config.drum_instrument + 1)
    populate_dropdown(selector_dd, seq_options, state.seq_index + 1)

    -- Set initial fader values
    if chord_volume_fader then chord_volume_fader.value = config.chord_volume end
    if bass_volume_fader then bass_volume_fader.value = config.bass_volume end
    if lead_volume_fader then lead_volume_fader.value = config.lead_volume end
    if drum_volume_fader then drum_volume_fader.value = config.drum_volume end
    if master_volume_fader then master_volume_fader.value = 1.0 end
    if speed_fader then speed_fader.value = (config.bpm - 60) / 200 end

    -- Instrument dropdown callbacks
    if chord_inst_dd then
        chord_inst_dd.on_change = function(self)
            config.chord_instrument = self.selected - 1
            apply_config_to_seq()
        end
    end

    if bass_inst_dd then
        bass_inst_dd.on_change = function(self)
            config.bass_instrument = self.selected - 1
            apply_config_to_seq()
        end
    end

    if lead_inst_dd then
        lead_inst_dd.on_change = function(self)
            config.lead_instrument = self.selected - 1
            apply_config_to_seq()
        end
    end

    if drum_inst_dd then
        drum_inst_dd.on_change = function(self)
            config.drum_instrument = self.selected - 1
            apply_config_to_seq()
        end
    end

    -- Volume fader callbacks
    if chord_volume_fader then
        chord_volume_fader.on_change = function(self)
            config.chord_volume = self.value
            apply_config_to_seq()
        end
    end

    if bass_volume_fader then
        bass_volume_fader.on_change = function(self)
            config.bass_volume = self.value
            apply_config_to_seq()
        end
    end

    if lead_volume_fader then
        lead_volume_fader.on_change = function(self)
            config.lead_volume = self.value
            apply_config_to_seq()
        end
    end

    if drum_volume_fader then
        drum_volume_fader.on_change = function(self)
            config.drum_volume = self.value
            apply_config_to_seq()
        end
    end

    -- Speed fader: control BPM (60-260)
    if speed_fader then
        speed_fader.on_change = function(self)
            config.bpm = math.floor(60 + self.value * 200)
            apply_config_to_seq()
        end
    end

    -- Master volume fader
    if master_volume_fader then
        master_volume_fader.on_change = function(self)
            master_volume = self.value
            apply_config_to_seq()
        end
    end

    -- Play button: play/stop the sequence (pre-computed path)
    if play_button then
        play_button.on_change = function()
            if playing then
                if play_handler then
                    play_handler.stop()
                end
                playing = false
                play_handler = nil
                play_button.overlay = icons.Play
                for s in all(speaker_widgets) do
                    s.playing = false
                end
            else
                apply_config_to_seq()
                play_handler = state.seq.play()
                playing = true
                play_button.overlay = icons.Stop
                for s in all(speaker_widgets) do
                    s.playing = true
                end
            end
        end
    end

    -- Export button
    if export_button then
        export_button.on_change = function()
            apply_config_to_seq()
            state.seq.export()
        end
    end

    -- Selector: switch active sequence
    if selector_dd then
        selector_dd.on_change = function(self)
            if playing and play_handler then
                play_handler.stop()
                playing = false
                play_handler = nil
                if play_button then
                    play_button.overlay = icons.Play
                end
                for s in all(speaker_widgets) do
                    s.playing = false
                end
            end
            state.seq_index = self.selected - 1
            state.seq = sfx.sequence(state.seq_index)
            _G._tiny_music_seq_index = state.seq_index

            load_config_from_seq()
            sync_ui(refs)
            apply_config_to_seq()
        end
    end

    -- Apply config to sequence on init
    apply_config_to_seq()
    _G._tiny_music_seq_index = state.seq_index
end

function _init_fader(entities)
    for f in all(entities["Fader"]) do
        local fader = widgets:create_fader(f)
        table.insert(all_widgets, fader)
    end
end

function _init_counter(entities)
    for c in all(entities["Counter"]) do
        local counter = widgets:create_counter(c)
        table.insert(all_widgets, counter)
    end
end

function _init()
    all_widgets = {}
    modals_by_name = {}
    speaker_widgets = {}
    overlay_widget = nil
    save_state = nil
    save_button_ref = nil
    selector_dd_ref = nil
    play_button_ref = nil
    playing = false
    play_handler = nil
    master_volume = 1.0

    map.level("MusicEditor")

    state.seq_index = _G._tiny_music_seq_index or 0
    state.seq = sfx.sequence(state.seq_index)

    -- Panels first (drawn behind everything)
    local panel_entities = map.entities("Panels")
    EditorBase.init_panels(panel_entities, all_widgets)

    -- Then all interactive widgets
    local widget_entities = map.entities("Widgets")

    local buttons_by_action = EditorBase.init_text_buttons(widget_entities, all_widgets)
    save_button_ref = buttons_by_action["Save"]

    EditorBase.init_speakers(widget_entities, all_widgets, speaker_widgets)

    modals_by_name = EditorBase.init_buttons(widget_entities, all_widgets, {
        on_open = function()
            return state.seq.name or ""
        end,
        on_name_validate = function(value)
            if value and state.seq then
                state.seq.name = value
                if selector_dd_ref then
                    selector_dd_ref.options[state.seq_index + 1] = build_seq_label(state.seq_index)
                    selector_dd_ref:_init()
                end
            end
        end,
    })

    -- Create all dropdowns and wrap with overlay
    for d in all(widget_entities["Dropdown"]) do
        local dropdown = widgets:create_dropdown(d)
        wrap_dropdown_overlay(dropdown)
        table.insert(all_widgets, dropdown)
    end

    _init_fader(widget_entities)
    _init_counter(widget_entities)

    -- Wire music generator widgets (removes unused auto-gen widgets)
    _init_music_generator(widget_entities)

    save_state = EditorBase.init_save_reminder(all_widgets, save_button_ref, modals_by_name)
end

function _update()
    -- TAB switches to tracker editor
    if ctrl.pressed(keys.tab) then
        if playing and play_handler then
            play_handler.stop()
            playing = false
            play_handler = nil
        end
        tiny.exit("tiny-tracker-editor.lua")
        return
    end

    -- Auto-stop: detect when playback finishes
    if playing and play_handler then
        if not play_handler.playing then
            playing = false
            play_handler = nil
            if play_button_ref then
                play_button_ref.overlay = icons.Play
            end
            for s in all(speaker_widgets) do
                s.playing = false
            end
        end
    end

    EditorBase.update(modals_by_name, function()
        if overlay_widget then
            overlay_widget:_update()
        else
            for w in all(all_widgets) do
                w:_update()
            end
        end
    end)

    EditorBase.update_save_reminder(save_button_ref, save_state)
end

function _draw()
    EditorBase.draw(function()
        for w in all(all_widgets) do
            if w ~= overlay_widget then
                w:_draw()
            end
        end
        if overlay_widget then
            overlay_widget:_draw()
        end
    end, modals_by_name)
end
