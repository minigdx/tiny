local widgets = require("widgets")
local EditorBase = require("editor-base")
local icons = require("widgets.icons")
local Tracker = require("widgets.Tracker")

local all_widgets = {}
local modals_by_name = {}
local speaker_widgets = {}
local save_state = nil
local save_button_ref = nil
local tracker_widget = nil

local state = {
    seq = nil,
    seq_index = 0,
}

local playing = false
local play_handler = nil

-- Track which note is currently sounding for the preview (per column instrument)
local preview_note = nil

local function stop_playback()
    if play_handler then
        play_handler.stop()
    end
    playing = false
    play_handler = nil
    if tracker_widget then
        tracker_widget.play_beat = nil
    end
    for s in all(speaker_widgets) do
        s.playing = false
    end
end

local function start_playback()
    if not state.seq then return end
    -- Sync tracker data to sequence before playing
    if tracker_widget then
        tracker_widget:sync_to_sequence(state.seq)
    end
    play_handler = state.seq.play()
    playing = true
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
    playing = false
    play_handler = nil
    preview_note = nil

    map.level("TrackerEditor")

    state.seq_index = 0
    state.seq = sfx.sequence(0)

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

    -- Create tracker widget from entity
    for t in all(widget_entities["Tracker"]) do
        tracker_widget = widgets:create_tracker(t)
        table.insert(all_widgets, tracker_widget)
    end

    -- Load sequence data into tracker
    if tracker_widget and state.seq then
        tracker_widget:load_from_sequence(state.seq)
        tracker_widget.on_change = function(self)
            self:sync_to_sequence(state.seq)
        end
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
                        octave = tracker_widget.last_octave,
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
