local widgets = require("widgets")
local mouse = require("mouse")
local wire = require("wire")
local ModeSwitch = require("widgets/ModeSwitch")
local LayerManager = require("layers")

local all_widgets = {}
local modals_by_name = {}
local dropdown_widget = nil
local layer_manager = nil

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
    local low = self.y + self.height - 8

    local previous_x = nil
    local previous_y = nil

    for note in all(self.values) do
        local volume = note.volume * (self.height - 8)
        local y = low - volume
        local startVelocity = self.x + note.beat * 16
        local endVelocity = startVelocity + note.duration * 16
        local x = startVelocity + (endVelocity - startVelocity) * 0.5

        if previous_x then
            shape.line(x, y, previous_x, previous_y, 5)
        end

        previous_x = x
        previous_y = y
    end

    for note in all(self.values) do
        local volume = note.volume * (self.height - 8)
        local y = low - volume
        local startVelocity = self.x + note.beat * 16
        local endVelocity = startVelocity + note.duration * 16
        local x = startVelocity + (endVelocity - startVelocity) * 0.5

        local is_active = self.current_beat and note.beat <= self.current_beat and self.current_beat < note.beat + note.duration

        if is_active then
            shape.circlef(x, y, 2, 13)
        else
            shape.circle(x, y, 2, 18)
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
    if ctrl.pressed(keys.left) then
        self.beat = self.beat - 0.5
    elseif ctrl.pressed(keys.right) then
        self.beat = self.beat + 0.5
    end

    if ctrl.pressed(keys.space) then
        self:playSfx()
    end

    if self.play then
        self.time = self.time + tiny.dt
        self.beat = self.time * (state.sfx.bpm / 60)

        if self.beat >= 32 then
            self.play = false
            self.beat = 0
        end
    end

    self.beat = math.clamp(0, self.beat, 15.5)

    self:set_value(self.beat)
end

Player._draw = function(self)
    local x = self.editor.x + self.beat * self.step_x
    local y = self.editor.y - 4
    spr.sdraw(x, y, 0, 48, 8, 8)
end

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
    values = {}
}

SfxEditor._init = function(self)
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
    local color = spr.pget(164, 64 + local_y)

    local color_to_note = {
        [10] = "C",
        [7] = "Cs",
        [17] = "D",
        [11] = "Ds",
        [15] = "E",
        [12] = "F",
        [13] = "Fs",
        [8] = "G",
        [5] = "Gs",
        [4] = "A",
        [3] = "As",
        [1] = "B"
    }

    local note = color_to_note[color]
    local octave = (local_y <= 95 and 1) or 0
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
    for index = 1, #self.values - 1 do
        local note = self.values[index]
        local next_note = self.values[index + 1]

        local y = self.y + self.y + self.height - (note.notei + 2 - self.octave * 12) * 8 + 4
        local end_x = self.x + note.beat * 16 + (note.duration) * 16

        local center = end_x - 4

        local y_next = self.y + self.y + self.height - (next_note.notei + 2 - self.octave * 12) * 8 + 4
        local start_x_next = self.x + next_note.beat * 16

        local center_next = start_x_next + 4

        shape.line(center, y, center_next, y_next, 13)
    end

    for note in all(self.values) do
        local y = self.y + self.height - (note.notei + 2 - self.octave * 12) * 8
        local start_x = self.x + note.beat * 16
        local end_x = self.x + note.beat * 16 + (note.duration) * 16 - 3

        local is_active = 0
        if self.current_beat and note.beat <= self.current_beat and self.current_beat < note.beat + note.duration then
            is_active = 8
        end
        -- head
        spr.sdraw(
                start_x, self.y + y,
                16, 112 + is_active, 3, 8)

        -- body
        for xx = start_x + 3, end_x do
            spr.sdraw(
                    xx, self.y + y,
                    22, 112 + is_active, 1, 8)
        end
        -- tail
        spr.sdraw(
                end_x, self.y + y,
                24, 112 + is_active, 4, 8)
    end
end

function _init_mode_switch(entities)
    for mode in all(entities["ModeSwitch"]) do
        local button = new(ModeSwitch, mode)
        table.insert(all_widgets, button)
    end
end

function _init_dropdowns(entities)
    for d in all(entities["Dropdown"]) do
        local dropdown = widgets:create_dropdown(d)

        -- The wide dropdown (240px) is the SFX selector; narrower ones are instrument selectors
        if dropdown_widget == nil and dropdown.width >= 200 then
            if #dropdown.options == 0 then
                for i = 0, 31 do
                    local s = sfx.sfx(i)
                    local name = s.name or ("SFX " .. i)
                    table.insert(dropdown.options, "[" .. i .. "] " .. name)
                end
                dropdown:_init()
            end

            dropdown.on_change = function(self)
                state.sfx = sfx.sfx(self.selected - 1)
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
        end

        table.insert(all_widgets, dropdown)
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
                    target:open(state.sfx.name)
                end
            end
        end

        table.insert(all_widgets, button)
    end

    local name_modal = modals_by_name["NameModal"]
    if name_modal then
        name_modal.on_validate = function(self, value)
            if value and state.sfx then
                state.sfx.name = value
                if dropdown_widget then
                    local idx = dropdown_widget.selected
                    dropdown_widget.options[idx] = "[" .. (idx - 1) .. "] " .. value
                    dropdown_widget:_init()
                end
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
            for i = 0, 15 do
                local inst = sfx.instrument(i)
                local name = (inst and inst.name) or ("Instrument " .. i)
                table.insert(instrument_dropdown.options, "[" .. i .. "] " .. name)
            end
            instrument_dropdown:_init()
        end

        wire.sync(state, "sfx.instrument", instrument_dropdown, "selected", function(_, _, value)
            return value + 1
        end)
        instrument_dropdown.on_change = function(self)
            state.sfx.set_instrument(self.selected - 1)
        end

        -- Overlay handling for instrument dropdown
        local original_update = instrument_dropdown._update
        instrument_dropdown._update = function(self)
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

        table.insert(all_widgets, widget)
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
        wire.sync(widget, "bpm", bpm, "value")

        local playButton = wire.find_widget(all_widgets, widget.fields.PlayButton)
        playButton.on_change = function() widget:playSfx() end

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
    layer_manager = nil

    map.level("SfxEditor")
    local entities = map.entities()

    state.sfx = sfx.sfx(0)

    -- Init order matters for EntityRef resolution
    _init_mode_switch(entities)
    _init_dropdowns(entities)
    _init_buttons(entities)
    _init_knob(entities)
    _init_velocity_editor(entities)
    _init_sfx_editor(entities)    -- refs Knob + Instrument Dropdown
    _init_player(entities)        -- refs SfxEditor + VelocityEditor + Knob + Buttons

    layer_manager = LayerManager.create()
    layer_manager:register("Widgets", { tiles = "WidgetsTiles", widgets = all_widgets, always = true })
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
end

function _draw()
    layer_manager:draw_base()
    layer_manager:draw_active()
    for _, modal in pairs(modals_by_name) do
        modal:_draw()
    end
    mouse._draw()
end
