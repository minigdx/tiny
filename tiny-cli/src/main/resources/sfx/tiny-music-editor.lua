local widgets = require("widgets")
local wire = require("wire")
local EditorBase = require("editor-base")
local music_templates = require("music-templates")
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
local config_dirty = true

local config = {
    root = "C",
    scale_name = "Major",
    progression_name = "Classic",
    lead_style = "Stepwise",
    drum_pattern = "Rock",
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

local themes = {
    { name = "Adventurous", scale_name = "Major", progression_name = "Classic", lead_style = "Stepwise", drum_pattern = "Rock", bpm = 120 },
    { name = "Jumper", scale_name = "Penta Maj", progression_name = "Upbeat", lead_style = "Bouncy", drum_pattern = "Dance", bpm = 140 },
    { name = "Mystery", scale_name = "Dorian", progression_name = "Tense", lead_style = "Sparse", drum_pattern = "Sparse", bpm = 90 },
    { name = "Sadness", scale_name = "Minor", progression_name = "Melancholy", lead_style = "Stepwise", drum_pattern = "Halftime", bpm = 100 },
    { name = "Industrial", scale_name = "Mixolydian", progression_name = "Tense", lead_style = "Arpeggiated", drum_pattern = "Funky", bpm = 130 },
    { name = "Dreamy", scale_name = "Penta Min", progression_name = "Dreamy", lead_style = "Arpeggiated", drum_pattern = "Halftime", bpm = 80 },
    { name = "March", scale_name = "Major", progression_name = "Upbeat", lead_style = "Stepwise", drum_pattern = "March", bpm = 110 },
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

local function find_index(list, value)
    for i, v in ipairs(list) do
        if v == value then return i end
    end
    return 1
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

local function mark_config_dirty()
    config_dirty = true
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

    selector_dd_ref = selector_dd
    play_button_ref = play_button

    -- Find unreferenced dropdown for Lead Style by position (21, 72)
    local lead_style_dd = nil
    local referenced_iids = {}
    for _, field_name in ipairs({
        "DrumPattern", "DrumVolume", "MusicTheme", "MusicScale",
        "LeadInstrument", "LeadVolume", "BassInstrument", "BassVolume",
        "RythmVolume", "RythmChordProgression", "RythmInstrument",
        "Play", "Volume", "Selector", "Export",
    }) do
        local ref = gen.fields[field_name]
        if ref then
            referenced_iids[ref.entityIid] = true
        end
    end
    for w in all(all_widgets) do
        if w.options and not referenced_iids[w.iid]
            and w.x >= 18 and w.x <= 24
            and w.y >= 69 and w.y <= 75 then
            lead_style_dd = w
            break
        end
    end

    -- Build option lists
    local theme_names = {}
    for _, t in ipairs(themes) do
        table.insert(theme_names, t.name)
    end

    local inst_options = build_instrument_options()

    local seq_options = {}
    for i = 0, 7 do
        table.insert(seq_options, build_seq_label(i))
    end

    -- Populate dropdowns with options
    populate_dropdown(theme_dd, theme_names, 1)
    populate_dropdown(scale_dd, music_templates.scale_names, find_index(music_templates.scale_names, config.scale_name))
    populate_dropdown(progression_dd, music_templates.progression_names, find_index(music_templates.progression_names, config.progression_name))
    populate_dropdown(drum_pattern_dd, music_templates.drum_pattern_names, find_index(music_templates.drum_pattern_names, config.drum_pattern))
    populate_dropdown(lead_style_dd, music_templates.lead_styles, find_index(music_templates.lead_styles, config.lead_style))
    populate_dropdown(chord_inst_dd, inst_options, config.chord_instrument + 1)
    populate_dropdown(bass_inst_dd, inst_options, config.bass_instrument + 1)
    populate_dropdown(lead_inst_dd, inst_options, config.lead_instrument + 1)
    populate_dropdown(selector_dd, seq_options, state.seq_index + 1)

    -- Set initial fader values
    if chord_volume_fader then chord_volume_fader.value = config.chord_volume end
    if bass_volume_fader then bass_volume_fader.value = config.bass_volume end
    if lead_volume_fader then lead_volume_fader.value = config.lead_volume end
    if drum_volume_fader then drum_volume_fader.value = config.drum_volume end
    if master_volume_fader then master_volume_fader.value = 1.0 end

    -- Dropdown callbacks: update config on change
    if scale_dd then
        scale_dd.on_change = function(self)
            config.scale_name = music_templates.scale_names[self.selected]
            mark_config_dirty()
        end
    end

    if progression_dd then
        progression_dd.on_change = function(self)
            config.progression_name = music_templates.progression_names[self.selected]
            mark_config_dirty()
        end
    end

    if drum_pattern_dd then
        drum_pattern_dd.on_change = function(self)
            config.drum_pattern = music_templates.drum_pattern_names[self.selected]
            mark_config_dirty()
        end
    end

    if lead_style_dd then
        lead_style_dd.on_change = function(self)
            config.lead_style = music_templates.lead_styles[self.selected]
            mark_config_dirty()
        end
    end

    if chord_inst_dd then
        chord_inst_dd.on_change = function(self)
            config.chord_instrument = self.selected - 1
            mark_config_dirty()
        end
    end

    if bass_inst_dd then
        bass_inst_dd.on_change = function(self)
            config.bass_instrument = self.selected - 1
            mark_config_dirty()
        end
    end

    if lead_inst_dd then
        lead_inst_dd.on_change = function(self)
            config.lead_instrument = self.selected - 1
            mark_config_dirty()
        end
    end

    -- Fader callbacks: update config on change
    if chord_volume_fader then
        chord_volume_fader.on_change = function(self)
            config.chord_volume = self.value
            mark_config_dirty()
        end
    end

    if bass_volume_fader then
        bass_volume_fader.on_change = function(self)
            config.bass_volume = self.value
            mark_config_dirty()
        end
    end

    if lead_volume_fader then
        lead_volume_fader.on_change = function(self)
            config.lead_volume = self.value
            mark_config_dirty()
        end
    end

    if drum_volume_fader then
        drum_volume_fader.on_change = function(self)
            config.drum_volume = self.value
            mark_config_dirty()
        end
    end

    -- Master volume: apply multiplier to all track volumes
    if master_volume_fader then
        master_volume_fader.on_change = function(self)
            local vol = self.value
            local base_volumes = { config.chord_volume, config.bass_volume, config.lead_volume, config.drum_volume }
            for i = 0, 3 do
                local track = state.seq.track(i)
                if track then
                    track.volume = base_volumes[i + 1] * vol
                end
            end
            -- Master volume changes need audio re-render but not note regeneration
            state.seq.invalidate()
        end
    end

    -- Theme dropdown: updates ALL config fields + syncs all other dropdowns
    if theme_dd then
        theme_dd.on_change = function(self)
            local theme = themes[self.selected]
            if not theme then return end

            config.scale_name = theme.scale_name
            config.progression_name = theme.progression_name
            config.lead_style = theme.lead_style
            config.drum_pattern = theme.drum_pattern
            config.bpm = theme.bpm

            if scale_dd then
                scale_dd:set_selected(find_index(music_templates.scale_names, theme.scale_name))
            end
            if progression_dd then
                progression_dd:set_selected(find_index(music_templates.progression_names, theme.progression_name))
            end
            if drum_pattern_dd then
                drum_pattern_dd:set_selected(find_index(music_templates.drum_pattern_names, theme.drum_pattern))
            end
            if lead_style_dd then
                lead_style_dd:set_selected(find_index(music_templates.lead_styles, theme.lead_style))
            end

            mark_config_dirty()
        end
    end

    -- Play button: toggle generate + play / stop
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
                if config_dirty then
                    music_templates.generate(state.seq, config)
                    state.seq.invalidate()
                    config_dirty = false
                end
                play_handler = state.seq.play()
                playing = true
                play_button.overlay = icons.Stop
                for s in all(speaker_widgets) do
                    s.playing = true
                end
            end
        end
    end

    -- Export button: export current sequence as wav
    if export_button then
        export_button.on_change = function()
            if config_dirty then
                music_templates.generate(state.seq, config)
                state.seq.invalidate()
                config_dirty = false
            end
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
            config_dirty = true
        end
    end
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
    config_dirty = true

    map.level("MusicEditor")

    state.seq_index = 0
    state.seq = sfx.sequence(0)

    -- Panels first (drawn behind everything)
    local panel_entities = map.entities("Panels")
    EditorBase.init_panels(panel_entities, all_widgets)

    -- Then all interactive widgets
    local widget_entities = map.entities("Widgets")

    local buttons_by_action = EditorBase.init_text_buttons(widget_entities, all_widgets)
    save_button_ref = buttons_by_action["Save"]

    EditorBase.init_speakers(widget_entities, all_widgets, speaker_widgets)
    EditorBase.init_mode_switch(widget_entities, all_widgets)

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

    -- Wire music generator widgets BEFORE save reminder
    _init_music_generator(widget_entities)

    save_state = EditorBase.init_save_reminder(all_widgets, save_button_ref, modals_by_name)
end

function _update()
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
