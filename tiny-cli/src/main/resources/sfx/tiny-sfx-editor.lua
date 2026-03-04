local widgets = require("widgets")
local wire = require("wire")
local sfx_templates = require("sfx-templates")
local EditorBase = require("editor-base")
local icons = require("widgets.icons")

local all_widgets = {}
local modals_by_name = {}
local dropdown_widget = nil
local speaker_widgets = {}
local sfx_editor_ref = nil
local octave_counter_ref = nil
local overlay_widget = nil

-- colors
local yellow = 9
local orange = 8
local light_green = 6
local dark_green = 13
local pink = 7
local purple = 4


local state = {
    sfx = nil,
}

function roundToHalf(num)
    local rounded_step = math.floor(num * 2)
    local final_rounded = rounded_step / 2
    return final_rounded
end

-- VelocityEditor widget: volume/velocity curve editor per beat
local VelocityEditor = {
    values = {},
    current_beat = nil,
    playing = false,
}

VelocityEditor._update = function(self)
    local p = ctrl.touch()
    if ctrl.touching(0) and inside_widget(self, p.x, p.y) then
        local local_x = p.x - self.x
        local local_y = p.y - self.y
        local beat = roundToHalf((local_x) / 16.0)

        if local_y > self.height - 8 then
            self:set_value(beat, 0)
        else
            local volume = math.clamp(0, 1.0 - (local_y / (self.height - 8)), 1.0)
            self:set_value(beat, volume)
        end
    end
end

VelocityEditor.set_value = function(self, beat, volume)
    if self.on_change then
        self:on_change({ beat = beat, volume = volume })
    end
end

VelocityEditor._draw = function(self)
    -- background
    shape.rectf(self.x, self.y, self.width, self.height, 2)
    shape.rect(self.x, self.y, self.width, self.height, 1)

    local low = self.y + self.height

    -- Pass 1: Group outlines at tip level
    for note in all(self.values) do
        if note.duration > 0.5 then
            local volume_height = note.volume * (self.height - 8)
            local tip_y = (volume_height > 0) and (low - volume_height - 2) or (low - 2)
            local bg_x = self.x + note.beat * 16
            local bg_width = note.duration * 16
            shape.rect(bg_x, tip_y, bg_width, 2, 3)
        end
    end

    -- Pass 2: Fader bars per half-beat
    for note in all(self.values) do
        local num_half_beats = math.floor(note.duration / 0.5)
        local is_active = self.playing and self.current_beat and note.beat <= self.current_beat and self.current_beat < note.beat + note.duration

        for i = 0, num_half_beats - 1 do
            local cell_x = self.x + (note.beat + i * 0.5) * 16
            local volume_height = note.volume * (self.height - 8)

            local fill_color = is_active and light_green or pink
            local tip_color = is_active and light_green or purple

            if volume_height > 0 then
                shape.rectf(cell_x + 1, low - volume_height, 6, volume_height, fill_color)
            end

            local tip_y = (volume_height > 0) and (low - volume_height - 2) or (low - 2)
            shape.rectf(cell_x + 1, tip_y, 6, 2, tip_color)
        end
    end
end

-- Player widget: playback controller (playhead, play/stop, BPM timing)
local Player = {
    beat = 0,
    step_x = 16,
    bpm = 0,
    time = 0,
    play = false,
}

Player._update = function(self)
    if ctrl.pressed(keys.space) then
        self:playSfx()
    end

    if self.play then
        self.time = self.time + tiny.dt
        self.beat = self.time * (state.sfx.bpm / 60)

        local finished = false
        if self.handler and not self.handler.playing then
            finished = true
        elseif self.beat >= 32 then
            finished = true
        end

        if finished then
            self.play = false
            self.beat = 0
            self.time = 0
            if self.playButton then
                self.playButton.overlay = icons.Play
            end
        end
    end

    self.beat = math.clamp(0, self.beat, 15.5)

    self:set_value(self.beat)
end

Player._draw = function(self) end

Player.playSfx = function(self)
    self.beat = 0
    self.play = not self.play
    self.time = 0

    if self.handler then
        self.handler.stop()
    end

    if self.play then
        self.handler = state.sfx.play()
    end
end

Player.set_value = function(self, value)
    if self.on_change then
        self:on_change(value)
    end
end

-- SfxEditor widget: main note editing grid
local SfxEditor = {
    octave = 2,
    note = "C2",
    current_beat = nil,
    playing = false,
    values = {},
    y_factor = 1,
    note_y_factor = 1,
}

SfxEditor._init = function(self)
    self.y_factor = 192 / self.height
    self.note_y_factor = self.height / 24
end

SfxEditor._update = function(self)
    local p = ctrl.touch()

    if ctrl.touching(0) ~= nil and self.current_edit ~= nil and ctrl.pressing(keys.shift) then
        local local_x = p.x - self.x
        local added_duration = math.max(0, (roundToHalf((local_x + 4) / 16) - self.current_edit.beat))
        local duration = math.max(0.5, added_duration)
        self.current_edit.duration = duration
    elseif inside_widget(self, p.x, p.y) and ctrl.touching(0) ~= nil and self.current_edit ~= nil then
        local local_x = p.x - self.x

        local value = {
            beat = self.current_edit.beat,
            note = self.note,
            duration = 0.5,
            unique = true
        }

        local current_beat = roundToHalf((local_x - 0.5) / 16.0)

        if self.current_edit.beat ~= current_beat then
            self:set_value(value)

            self.current_edit = {
                beat = roundToHalf((local_x) / 16.0),
                note = self.note,
                duration = 0.5
            }
        end
    elseif inside_widget(self, p.x, p.y) and ctrl.touched(0) ~= nil and self.current_edit == nil then
        local local_x = p.x - self.x

        self.current_edit = {
            beat = roundToHalf((local_x) / 16.0),
            note = self.note,
            duration = 0.5
        }
    elseif ctrl.touched(0) == nil and self.current_edit ~= nil then
        local value = {
            duration = self.current_edit.duration,
            beat = self.current_edit.beat,
            note = self.current_edit.note,
            unique = true
        }

        self:set_value(value)

        self.current_edit = nil
    elseif inside_widget(self, p.x, p.y) and ctrl.touching(1) ~= nil then
        local local_x = p.x - self.x

        local value = {
            beat = roundToHalf((local_x) / 16.0),
            note = self.note,
        }

        if self.remove_value then
            self:remove_value(value)
        end
    end

    local local_y = math.clamp(0, p.y - self.y, self.height)

    local prev = spr.sheet(2)
    local color = spr.pget(164, (64 + local_y * self.y_factor))
    spr.sheet(prev)
    self.yy = color

    local color_to_note = {
        [1] = "C",
        [2] = "Cs",
        [5] = "D",
        [10] = "Ds",
        [12] = "E",
        [4] = "F",
        [6] = "Fs",
        [9] = "G",
        [3] = "Gs",
        [7] = "A",
        [8] = "As",
        [13] = "B"
    }

    local note = color_to_note[color]
    local octave = (local_y <= self.height * 0.5 and 1) or 0
    if note then
        self.note = note .. (self.octave + octave)
    end
end

SfxEditor.set_value = function(self, value)
    if self.on_change then
        self:on_change(value)
    end
end

SfxEditor._draw = function(self)
    -- background
    shape.rectf(self.x, self.y, self.width, self.height, 2)
    shape.rect(self.x, self.y, self.width, self.height, 1)

    local bottom = self.y + self.height

    -- Pass 1: Group outlines at tip level
    for note in all(self.values) do
        if note.duration > 0.5 then
            local note_y = self.y + (23 - (note.notei - self.octave * 12)) * 8
            local tip_y = math.max(self.y, note_y - 2)
            local bg_x = self.x + note.beat * 16
            local bg_width = note.duration * 16
            shape.rect(bg_x, tip_y, bg_width, 2, 3)
        end
    end

    -- Pass 2: Fader bars from bottom up to pitch level
    for note in all(self.values) do
        -- octave_offset should be equal to 0 or 1
        local octave_offset = (note.octave - self.octave)
        if(octave_offset > 1) then
            console.log("octave_offset > 1 = ", octave_offset)
        end
        -- notei is the index of the note in an octave (0 < notei < 12
        local notei = note.notei - note.octave * 12
        if notei > 12 then
            console.log("notei is an invalid note index as > 12", notei)
        end

        local note_height = (notei + octave_offset * 12) * self.note_y_factor

        local note_y = self.y + self.height - note_height
        local fader_height = bottom - note_y
        local num_half_beats = math.floor(note.duration / 0.5)
        local is_active = self.playing and self.current_beat and note.beat <= self.current_beat and self.current_beat < note.beat + note.duration

        for i = 0, num_half_beats - 1 do
            local cell_x = self.x + (note.beat + i * 0.5) * 16

            local fill_color = is_active and light_green or yellow
            local tip_color = is_active and light_green or orange

            if fader_height > 0 then
                shape.rectf(cell_x + 1, note_y, 6, fader_height, fill_color)
            end

            local tip_y = math.max(self.y, note_y - 2)
            shape.rectf(cell_x + 1, tip_y, 6, 2, tip_color)
        end
    end
end


local function shift_notes(old_octave, new_octave)
    local delta = new_octave - old_octave
    local notes = state.sfx.notes
    local saved = {}
    for note in all(notes) do
        table.insert(saved, {
            beat = note.beat,
            note = note.note,
            octave = note.octave,
            duration = note.duration,
            volume = note.volume,
        })
    end
    for _, note in ipairs(saved) do
        state.sfx.remove_note({ beat = note.beat, note = note.note })
    end
    for _, note in ipairs(saved) do
        local pitch_class = string.sub(note.note, 1, #note.note - 1)
        local new_note_name = pitch_class .. (note.octave + delta)
        state.sfx.set_note({
            beat = note.beat,
            note = new_note_name,
            duration = note.duration,
        })
        state.sfx.set_volume({ beat = note.beat, volume = note.volume })
    end
end

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

function _init_knob(entities)
    for k in all(entities["Knob"]) do
        local knob = widgets:create_knob(k)
        table.insert(all_widgets, knob)
    end
end

function _init_fader(entities)
    for f in all(entities["Fader"]) do
        local fader = widgets:create_fader(f)
        table.insert(all_widgets, fader)
    end
end

function _init_counter_entities(entities)
    for c in all(entities["Counter"]) do
        local counter = widgets:create_counter(c)
        table.insert(all_widgets, counter)
    end
end

function _init_velocity_editor(entities)
    for volume in all(entities["VelocityEditor"]) do
        local widget = new(VelocityEditor, volume)
        wire.sync(state, "sfx.notes", widget, "values")
        widget.on_change = function(self, value)
            state.sfx.set_volume(value)
        end
        table.insert(all_widgets, widget)
    end
end

function _init_sfx_editor(entities)
    for editor in all(entities["SfxEditor"]) do
        local widget = new(SfxEditor, editor)
        widget:_init()
        local bpm = wire.find_widget(all_widgets, widget.fields.BPM)

        local transform = {
            to_widget = function(to, from, value)
                return (value - 60) / 520
            end,

            from_widget = function(to, from, value)
                return 60 + value * 520
            end
        }
        wire.bind(state, "sfx.bpm", bpm, "value", transform)

        local volume_knob = wire.find_widget(all_widgets, widget.fields.Volume)
        if volume_knob then
            wire.bind(state, "sfx.volume", volume_knob, "value")
        end

        local octave_counter = wire.find_widget(all_widgets, widget.fields.Octave)
        if octave_counter then
            octave_counter.min = 0
            octave_counter.max = 7
            octave_counter.value = widget.octave
            octave_counter.on_change = function(self)
                local old_octave = widget.octave
                local new_octave = self.value
                if old_octave ~= new_octave then
                    shift_notes(old_octave, new_octave)
                    widget.octave = new_octave
                end
            end
            octave_counter_ref = octave_counter
        end

        wire.sync(state, "sfx.notes", widget, "values")
        widget.on_change = function(self, value)
            state.sfx.set_note(value)
        end
        widget.remove_value = function(self, value)
            state.sfx.remove_note(value)
        end

        -- Instrument dropdown
        local instrument_dropdown = wire.find_widget(all_widgets, widget.fields.Instrument)
        if #instrument_dropdown.options == 0 then
            for i = 0, 7 do
                local inst = sfx.instrument(i)
                local name = (inst and inst.name) or ("Instrument " .. i)
                table.insert(instrument_dropdown.options, "[" .. i .. "] " .. name)
            end
            instrument_dropdown:_init()
        end

        wrap_dropdown_overlay(instrument_dropdown)

        wire.sync(state, "sfx.instrument", instrument_dropdown, "selected", function(_, _, value)
            return value + 1
        end)
        instrument_dropdown.on_change = function(self)
            state.sfx.set_instrument(self.selected - 1)
        end

        table.insert(all_widgets, widget)
        sfx_editor_ref = widget
        return widget
    end
end

function _init_player(entities)
    for p in all(entities["Player"]) do
        local widget = new(Player, p)
        local sfxEditor = wire.find_widget(all_widgets, widget.fields.SfxEditor)
        widget.editor = sfxEditor
        local velocityEditor = wire.find_widget(all_widgets, widget.fields.VelocityEditor)
        local bpm = wire.find_widget(all_widgets, widget.fields.BPM)

        wire.sync(widget, "beat", sfxEditor, "current_beat")
        wire.sync(widget, "beat", velocityEditor, "current_beat")
        wire.sync(widget, "play", sfxEditor, "playing")
        wire.sync(widget, "play", velocityEditor, "playing")
        wire.sync(widget, "bpm", bpm, "value")

        for _, s in ipairs(speaker_widgets) do
            wire.sync(widget, "play", s, "playing")
        end

        if widget.fields.SfxSelector then
            local sfxSelector = wire.find_widget(all_widgets, widget.fields.SfxSelector)
            widget.sfxSelector = sfxSelector
        end

        local playButton = wire.find_widget(all_widgets, widget.fields.PlayButton)
        widget.playButton = playButton
        playButton.on_change = function()
            widget:playSfx()
            if widget.play then
                playButton.overlay = icons.Stop
            else
                playButton.overlay = icons.Play
            end
        end

        local saveButton = wire.find_widget(all_widgets, widget.fields.SaveButton)
        saveButton.on_change = function() sfx.save() end

        local exportButton = wire.find_widget(all_widgets, widget.fields.ExportButton)
        exportButton.on_change = function() state.sfx.export() end

        table.insert(all_widgets, widget)
    end
end

function _init()
    all_widgets = {}
    modals_by_name = {}
    dropdown_widget = nil
    speaker_widgets = {}
    sfx_editor_ref = nil
    octave_counter_ref = nil
    overlay_widget = nil

    map.level("SfxEditor")

    state.sfx = sfx.sfx(0)

    -- Panels first (drawn behind everything)
    local panel_entities = map.entities("Panels")
    EditorBase.init_panels(panel_entities, all_widgets)

    -- Then all interactive widgets
    local widget_entities = map.entities("Widgets")
    EditorBase.init_text_buttons(widget_entities, all_widgets)
    EditorBase.init_speakers(widget_entities, all_widgets, speaker_widgets)
    EditorBase.init_mode_switch(widget_entities, all_widgets)

    dropdown_widget = EditorBase.init_entity_dropdown(widget_entities, all_widgets, {
        count = 32,
        fetch = function(i) return sfx.sfx(i) end,
        label = "SFX",
        min_width = 150,
        on_select = function(index) state.sfx = sfx.sfx(index) end,
    })

    if dropdown_widget then
        wrap_dropdown_overlay(dropdown_widget)
    end

    modals_by_name = EditorBase.init_buttons(widget_entities, all_widgets, {
        modal_sizes = {
            NameModal = { x = 96, y = 64, width = 192, height = 128 },
            RandomSfxModal = { x = 72, y = 68, width = 240, height = 120 },
        },
        on_open = function() return state.sfx.name end,
        on_name_validate = function(value)
            if value and state.sfx then
                state.sfx.name = value
                EditorBase.update_dropdown_name(dropdown_widget, value)
            end
        end,
    })

    -- Wire RandomSfxModal
    local random_modal = modals_by_name["RandomSfxModal"]
    if random_modal then
        if random_modal.dropdown then
            random_modal.dropdown.options = sfx_templates.list
            random_modal.dropdown:_init()
        end

        random_modal.on_validate = function(self, value, dropdown_index)
            if dropdown_index and state.sfx then
                local template_name = sfx_templates.list[dropdown_index]
                if template_name then
                    local lowest_octave = sfx_templates.generate(state.sfx, template_name)
                    if lowest_octave and sfx_editor_ref and octave_counter_ref then
                        sfx_editor_ref.octave = lowest_octave
                        octave_counter_ref.value = math.clamp(octave_counter_ref.min, lowest_octave, octave_counter_ref.max)
                    end
                end
            end
        end
    end

    _init_knob(widget_entities)
    _init_fader(widget_entities)
    _init_counter_entities(widget_entities)
    _init_velocity_editor(widget_entities)
    _init_sfx_editor(widget_entities)
    _init_player(widget_entities)
end

function _update()
    EditorBase.update(modals_by_name, function()
        for w in all(all_widgets) do
            w:_update()
        end
    end)
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
