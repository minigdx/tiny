local widgets = require("widgets")
local wire = require("wire")
local EditorBase = require("editor-base")
local icons = require("widgets.icons")
local Tracker = require("widgets.Tracker")

local all_widgets = {}
local modals_by_name = {}
local speaker_widgets = {}
local save_state = nil
local save_button_ref = nil
local tracker_widget = nil
local arrangement_widget = nil
local pattern_counter = nil
local duration_buttons = {}

-- Cycle of per-track durations exposed by the duration buttons.
local DURATION_CYCLE = { 32, 24, 12, 8, 4 }

local state = {
    seq = nil,
    seq_index = 0,
    pattern_index = 0,
}

-- Arrangement slot whose pattern is currently mirrored in the tracker grid.
-- Only set during playback, when the tracker follows the sequence through the arrangement.
local displayed_slot = nil
-- Pattern_index the user had selected before playback started, restored on stop.
local saved_pattern_index = nil

local function next_duration(current)
    for i, d in ipairs(DURATION_CYCLE) do
        if d == current then
            return DURATION_CYCLE[(i % #DURATION_CYCLE) + 1]
        end
    end
    return DURATION_CYCLE[1]
end

local function refresh_duration_buttons()
    if not state.seq then return end
    for col = 1, 4 do
        local btn = duration_buttons[col]
        if btn then
            local track = state.seq.track(col - 1, state.pattern_index)
            if track then
                btn.label = tostring(track.duration)
            end
        end
    end
end

local playing = false
local play_handler = nil

-- Track which note is currently sounding for the preview (per column instrument)
local preview_note = nil

-- Load the current pattern into the tracker grid
local function load_current_pattern()
    if not tracker_widget or not state.seq then return end
    -- Ensure the pattern exists
    state.seq.ensure_pattern(state.pattern_index)
    -- Load from the selected pattern
    tracker_widget:load_from_sequence(state.seq, state.pattern_index)
end

-- Sync the tracker grid back to the current pattern
local function sync_current_pattern()
    if not tracker_widget or not state.seq then return end
    tracker_widget:sync_to_sequence(state.seq, state.pattern_index)
end

local function arrangement_filled_count()
    if not arrangement_widget then return 0 end
    local count = 0
    for i = 1, 8 do
        if arrangement_widget.slots[i] ~= nil then
            count = count + 1
        else
            break
        end
    end
    return count
end

local function stop_playback()
    if play_handler then
        play_handler.stop()
    end
    playing = false
    play_handler = nil
    if tracker_widget then
        tracker_widget.play_beat = nil
    end
    if arrangement_widget then
        arrangement_widget.playing = false
        arrangement_widget.play_slot = nil
    end
    for s in all(speaker_widgets) do
        s.playing = false
    end
    -- Restore the pattern the user was editing before playback started.
    if saved_pattern_index ~= nil and state.seq then
        state.pattern_index = saved_pattern_index
        if pattern_counter then
            pattern_counter.value = saved_pattern_index
        end
        if tracker_widget then
            tracker_widget:load_from_sequence(state.seq, state.pattern_index)
        end
        refresh_duration_buttons()
    end
    saved_pattern_index = nil
    displayed_slot = nil
end

local function start_playback()
    if not state.seq then return end
    -- Sync tracker data to sequence before playing
    sync_current_pattern()
    -- Sync arrangement data
    if arrangement_widget then
        arrangement_widget:sync_to_sequence(state.seq)
    end
    play_handler = state.seq.loop()
    playing = true
    saved_pattern_index = state.pattern_index
    displayed_slot = nil
    if arrangement_widget then
        arrangement_widget.playing = true
        local fc = arrangement_filled_count()
        arrangement_widget.play_slot = fc > 0 and 1 or nil
    end
    for s in all(speaker_widgets) do
        s.playing = true
    end
end

-- Play a note preview using the instrument assigned to the current column
local function preview_note_on(cell, col)
    local name = Tracker.build_note_name(cell)
    if not name then return end
    -- Release previous preview note
    if preview_note then
        local inst = sfx.instrument(preview_note.inst_index)
        if inst then inst.note_off(preview_note.name) end
    end
    local track = state.seq and state.seq.track(col - 1)
    local inst_index = track and track.instrument or 0
    local inst = sfx.instrument(inst_index)
    if inst then
        inst.note_on(name)
        preview_note = { name = name, inst_index = inst_index }
    end
end

local function preview_note_off()
    if preview_note then
        local inst = sfx.instrument(preview_note.inst_index)
        if inst then inst.note_off(preview_note.name) end
        preview_note = nil
    end
end

function _init()
    all_widgets = {}
    modals_by_name = {}
    speaker_widgets = {}
    save_state = nil
    save_button_ref = nil
    tracker_widget = nil
    arrangement_widget = nil
    pattern_counter = nil
    duration_buttons = {}
    playing = false
    play_handler = nil
    preview_note = nil

    map.level("TrackerEditor")

    state.seq_index = _G._tiny_music_seq_index or 0
    state.seq = sfx.sequence(state.seq_index)

    -- Ensure instruments are linked on tracks
    for i = 0, 3 do
        local track = state.seq.track(i)
        if track then
            track.instrument = track.instrument
        end
    end

    -- Panels first (drawn behind everything)
    local panel_entities = map.entities("Panels")
    EditorBase.init_panels(panel_entities, all_widgets)

    -- Interactive widgets
    local widget_entities = map.entities("Widgets")

    local buttons_by_action = EditorBase.init_text_buttons(widget_entities, all_widgets)
    save_button_ref = buttons_by_action["Save"]

    EditorBase.init_speakers(widget_entities, all_widgets, speaker_widgets)

    -- Create counters (including the Pattern counter)
    for c in all(widget_entities["Counter"]) do
        local counter = widgets:create_counter(c)
        table.insert(all_widgets, counter)
    end

    -- Create arrangement widgets
    for a in all(widget_entities["Arrangement"]) do
        arrangement_widget = widgets:create_arrangement(a)
        table.insert(all_widgets, arrangement_widget)
    end

    -- Create tracker widget from entity
    for t in all(widget_entities["Tracker"]) do
        tracker_widget = widgets:create_tracker(t)
        table.insert(all_widgets, tracker_widget)

        -- Resolve Pattern counter reference from the Tracker entity
        if t.fields and t.fields.Pattern then
            pattern_counter = wire.find_widget(all_widgets, t.fields.Pattern)
        end

        -- Resolve Arrangement reference from the Tracker entity
        if t.fields and t.fields.Arragment then
            local arr_ref = wire.find_widget(all_widgets, t.fields.Arragment)
            if arr_ref then
                arrangement_widget = arr_ref
            end
        end

        -- Resolve per-track duration button refs from the Tracker entity
        local duration_fields = { "MelodyDuration", "RythmDuration", "BassDuration", "DrumDuration" }
        for col, field_name in ipairs(duration_fields) do
            if t.fields and t.fields[field_name] then
                duration_buttons[col] = wire.find_widget(all_widgets, t.fields[field_name])
            end
        end
    end

    -- Configure Pattern counter
    if pattern_counter then
        pattern_counter.min = 0
        pattern_counter.max = 7
        pattern_counter.value = state.pattern_index
        pattern_counter.on_change = function(self)
            -- Sync current pattern before switching
            sync_current_pattern()
            state.pattern_index = self.value
            -- Ensure the new pattern exists and load it
            load_current_pattern()
            refresh_duration_buttons()
        end
    end

    -- Configure per-track duration buttons (cycle 32 -> 24 -> 12 -> 8 -> 4)
    for col = 1, 4 do
        local btn = duration_buttons[col]
        if btn then
            btn.on_change = function(self)
                if not state.seq then return end
                local track = state.seq.track(col - 1, state.pattern_index)
                if not track then return end
                track.duration = next_duration(track.duration)
                self.label = tostring(track.duration)
                if tracker_widget then
                    tracker_widget:refresh_total_lines(state.seq, state.pattern_index)
                end
                -- Re-sync so the phrase's off-notes and cached buffer pick up
                -- the new duration.
                sync_current_pattern()
            end
        end
    end

    -- Load arrangement and sequence data
    if arrangement_widget and state.seq then
        arrangement_widget:load_from_sequence(state.seq)
        arrangement_widget.on_change = function(self)
            self:sync_to_sequence(state.seq)
            -- Update max_pattern from sequence
            self.max_pattern = state.seq.pattern_count - 1
        end
        arrangement_widget.on_play_toggle = function()
            if playing then
                stop_playback()
            else
                start_playback()
            end
        end
    end

    -- Load sequence data into tracker
    if tracker_widget and state.seq then
        load_current_pattern()
        tracker_widget.on_change = function(self)
            sync_current_pattern()
        end
    end

    refresh_duration_buttons()

    -- Switch toggles back to the music editor (same behaviour as TAB).
    for s in all(widget_entities["Switch"]) do
        local switch = widgets:create_switch(s)
        switch.on_change = function()
            stop_playback()
            preview_note_off()
            sync_current_pattern()
            if arrangement_widget then
                arrangement_widget:sync_to_sequence(state.seq)
            end
            _G._tiny_music_seq_index = state.seq_index
            tiny.exit("tiny-music-editor.lua")
        end
        table.insert(all_widgets, switch)
    end

    modals_by_name = EditorBase.init_buttons(widget_entities, all_widgets, {
        on_open = function()
            return state.seq.name or ""
        end,
        on_name_validate = function(value)
            if value and state.seq then
                state.seq.name = value
            end
        end,
    })

    save_state = EditorBase.init_save_reminder(all_widgets, save_button_ref, modals_by_name)
end

function _update()
    -- TAB switches back to music editor
    if ctrl.pressed(keys.tab) then
        stop_playback()
        preview_note_off()
        sync_current_pattern()
        if arrangement_widget then
            arrangement_widget:sync_to_sequence(state.seq)
        end
        _G._tiny_music_seq_index = state.seq_index
        tiny.exit("tiny-music-editor.lua")
        return
    end

    -- Space toggles play/stop
    if ctrl.pressed(keys.space) then
        if playing then
            stop_playback()
        else
            start_playback()
        end
    end

    -- Update play position from handler
    if playing and play_handler then
        if not play_handler.playing then
            stop_playback()
        else
            local beat = play_handler.beat
            if beat and arrangement_widget then
                local fc = arrangement_filled_count()
                if fc > 0 then
                    local slot = (math.floor(beat / 32) % fc) + 1
                    arrangement_widget.play_slot = slot

                    -- Follow playback: swap the tracker grid to the pattern now playing.
                    if slot ~= displayed_slot then
                        displayed_slot = slot
                        local pattern_idx = arrangement_widget.slots[slot]
                        if pattern_idx ~= nil and state.seq then
                            state.pattern_index = pattern_idx
                            if pattern_counter then
                                pattern_counter.value = pattern_idx
                            end
                            tracker_widget:load_from_sequence(state.seq, pattern_idx)
                            refresh_duration_buttons()
                        end
                    end
                else
                    arrangement_widget.play_slot = nil
                end
            end
            if beat and tracker_widget then
                tracker_widget.play_beat = beat % 32
                tracker_widget:_update_play_position()
            end
        end
    end

    -- Release preview note after a short duration (~1 beat at 120bpm = 0.5s)
    if preview_note then
        -- We'll release on the next frame after note_on (brief preview)
        -- Actually, release after a few frames for audibility
        if not preview_note.frames then
            preview_note.frames = 0
        end
        preview_note.frames = preview_note.frames + 1
        if preview_note.frames > 15 then
            preview_note_off()
        end
    end

    EditorBase.update(modals_by_name, function()
        -- Intercept piano key presses to trigger note preview
        if tracker_widget and not playing then
            local piano_map = {
                { key = "a", note = 3, accident = 1 },
                { key = "s", note = 4, accident = 1 },
                { key = "d", note = 5, accident = 1 },
                { key = "f", note = 6, accident = 1 },
                { key = "g", note = 7, accident = 1 },
                { key = "h", note = 1, accident = 1 },
                { key = "j", note = 2, accident = 1 },
                { key = "w", note = 3, accident = 2 },
                { key = "e", note = 4, accident = 2 },
                { key = "t", note = 6, accident = 2 },
                { key = "y", note = 7, accident = 2 },
                { key = "u", note = 1, accident = 2 },
            }
            for _, mapping in ipairs(piano_map) do
                if ctrl.pressed(keys[mapping.key]) then
                    -- The tracker widget will handle the data update.
                    -- We preview the sound with the note that was just entered.
                    local cell = {
                        note = mapping.note,
                        accident = mapping.accident,
                        octave = tracker_widget.last_octaves[tracker_widget.cursor_col],
                    }
                    preview_note_on(cell, tracker_widget.cursor_col)
                    break
                end
            end
        end

        for w in all(all_widgets) do
            w:_update()
        end
    end)

    EditorBase.update_save_reminder(save_button_ref, save_state)
end

function _draw()
    EditorBase.draw(function()
        for w in all(all_widgets) do
            w:_draw()
        end
    end, modals_by_name)
end
