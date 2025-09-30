local widgets = require("widgets")
local mouse = require("mouse")
local wire = require("wire")
local MatrixSelector = require("widgets/MatrixSelector")
local ModeSwitch = require("widgets/ModeSwitch")

local m = {
    widgets = {}
}

local green = 13
local red = 5
local white = 18
local shadow = 2

function roundToHalf(num)
    local rounded_step = math.floor(num * 2)
    local final_rounded = rounded_step / 2
    return final_rounded
end

local State = {
    -- is the user can edit the current bar?
    edit = false,
    -- the current bar the user is editing
    current_bar = nil,

    on_change = function()
    end
}

local state = new(State)

function inside_widget(w, x, y, offset)
    local off = 0
    if (offset) then
        off = offset
    end

    return w.x - off <= x and
            x <= w.x + w.width + off and
            w.y - off <= y and
            y <= w.y + w.height + off
end

function filter(widgetType)
    for w in all(m.widgets) do
        local mt = getmetatable(w)
        if mt and mt._update == widgetType._update then
            return w
        end
    end
    return nil
end

local VelocityEditor = {
    values = {},
    current_beat = nil, -- current played beat
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

    -- values -> beat / volume / duration
    for note in all(self.values) do
        local volume = note.volume * (self.height - 8)
        local y = low - volume
        local startVelocity = self.x + note.beat * 16
        local endVelocity = startVelocity + note.duration * 16
        local x = startVelocity + (endVelocity - startVelocity) * 0.5

        if previous_x then
            shape.line(x, y, previous_x, previous_y, red)
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

        if(is_active) then
            shape.circlef(x, y, 2, green)
        else
            shape.circle(x, y, 2, white)
        end
    end
end

local Player = {
    beat = 0,
    step_x = 16, -- adjust regarding the size of half of a bit on screen,
    time = 0,
    play = false,
}

Player._update = function(self)
    if (ctrl.pressed(keys.left)) then
        self.beat = self.beat - 0.5
    elseif (ctrl.pressed(keys.right)) then
        self.beat = self.beat + 0.5
    end

    if ctrl.pressed(keys.space) then
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

    if self.play then
        self.time = self.time + tiny.dt
        self.beat = self.time * (state.sfx.bpm / 60) * 2

        if (self.beat >= 32) then
            self.play = false
            self.beat = 0
        end
    end

    self.beat = math.clamp(0, self.beat, 32)

    self:set_value(self.beat)
end

Player._draw = function(self)
    local x = self.editor.x + self.beat * self.step_x
    local y = self.editor.y - 4
    -- right
    spr.sdraw(x, y, 0, 48, 8, 8)
    -- left
    -- spr.sdraw(x - 4, y, 248, 44, 4, 4, true)
end

Player.set_value = function(self, value)
    if self.on_change then
        self:on_change(value)
    end
end

local SfxEditor = {
    -- position of the keys (y only)
    keys_y = {
        { y = 0, h = 8 },
        { y = 9, h = 8 },
        { y = 17, h = 8 },
        { y = 25, h = 8 },
        { y = 33, h = 8 },
        { y = 41, h = 8 },
        { y = 49, h = 8 },
        { y = 57, h = 8 },
        { y = 65, h = 8 },
        { y = 73, h = 8 },
        { y = 81, h = 8 },
        { y = 89, h = 8 },
        { y = 97, h = 8 },
        { y = 105, h = 8 },
        { y = 113, h = 8 },
        { y = 121, h = 8 },
        { y = 129, h = 8 },
        { y = 137, h = 8 },
        { y = 145, h = 8 },
        { y = 153, h = 8 },
        { y = 161, h = 8 },
        { y = 169, h = 8 },
        { y = 177, h = 8 },
        { y = 185, h = 8 },
    },

    octave = 0,
    note = "C0",
    current_beat = nil, -- current played beat
    values = {}
}

SfxEditor._init = function(self)

end

SfxEditor._update = function(self)
    local p = ctrl.touch()

    if inside_widget(self, p.x, p.y) then
        state.edit = true
    else
        state.edit = false
    end

    if ctrl.touching(0) ~= nil and self.current_edit ~= nil and ctrl.pressing(keys.shift) then
        -- update the edit state
        local local_x = p.x - self.x
        local added_duration = math.max(0, (roundToHalf((local_x + 4) / 16) - self.current_edit.beat))
        local duration = math.max(0.5, added_duration)
        self.current_edit.duration = duration
    elseif inside_widget(self, p.x, p.y) and ctrl.touching(0) ~= nil and self.current_edit ~= nil then
        -- commit and create a new edit state
        local local_x = p.x - self.x

        local value = {
            beat = self.current_edit.beat,
            note = self.note,
            duration = 0.5,
            unique = true
        }

        local current_beat = roundToHalf((local_x - 0.5) / 16.0)

        if (self.current_edit.beat ~= current_beat) then

            self:set_value(value)

            self.current_edit = {
                beat = roundToHalf((local_x) / 16.0),
                note = self.note,
                duration = 0.5
            }
        end
    elseif inside_widget(self, p.x, p.y) and ctrl.touched(0) ~= nil and self.current_edit == nil then
        -- create the edit state
        local local_x = p.x - self.x

        self.current_edit = {
            beat = roundToHalf((local_x) / 16.0),
            note = self.note,
            duration = 0.5
        }
    elseif ctrl.touched(0) == nil and self.current_edit ~= nil then
        -- commit the edit state
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

        self:set_value(value)
    end

    -- get the current note regarding the y position.
    -- the note is computed fro the color of the color virtual keyboard
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
        self.note = note .. octave
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

        local y = self.y + self.height - (note.notei) * 8 + 4
        local end_x =  self.x + note.beat * 16 + (note.duration) * 16

        local center = end_x - 4

        local y_next = self.y + self.height - (next_note.notei) * 8 + 4
        local start_x_next = self.x + next_note.beat * 16

        local center_next = start_x_next + 4

        shape.line(center, y, center_next, y_next, green)
    end

    for note in all(self.values) do
        local y = self.y + self.height - (note.notei + 2) * 8
        local start_x = self.x + note.beat * 16
        local end_x =  self.x + note.beat * 16 + (note.duration) * 16 - 3

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
    for mode in all(entities["ModeButton"]) do
        local button = new(ModeSwitch, mode)
        table.insert(m.widgets, button)
    end
end

function _init_knob(entities)
    for k in all(entities["Knob"]) do
        local knob = widgets:create_knob(k)
        table.insert(m.widgets, knob)
    end
end

function _init_matrix_selector(entities)
    for matrix in all(entities["MatrixSelector"]) do
        local widget = new(MatrixSelector, matrix)
        widget:_init()

        if widget.fields.Label == "Instruments" then
            -- display the current instrument
            wire.sync(state, "sfx.instrument", widget, "value")
            -- set the instrument to the bar
            wire.listen(widget, "value", function(source, value)
                state.sfx.set_instrument(value)
            end)
            -- set the list of instruments
            wire.sync(state, "sfx.instrument", widget, "active_indices", function(_, _, id) return sfx.instrument(id).all end)
        end

        table.insert(m.widgets, widget)
    end
end

function _init_velocity_editor(entities)
    for volume in all(entities["VelocityEditor"]) do
        local widget = new(VelocityEditor, volume)
        wire.sync(state, "sfx.notes", widget, "values")
        widget.on_change = function(self, value)
            state.sfx.set_volume(value)
        end
        table.insert(m.widgets, widget)
    end
end

function _init_sfx_editor(entities)
    for volume in all(entities["SfxEditor"]) do
        local widget = new(SfxEditor, volume)
        wire.sync(state, "sfx.notes", widget, "values")
        widget.on_change = function(self, value)
            state.sfx.set_note(value)
        end

        table.insert(m.widgets, widget)
    end
end

function _init_player(entities)
    local player = new(Player)
    local sfxEditor = filter(SfxEditor)
    player.editor = sfxEditor

    local velocityEditor = filter(VelocityEditor)

    wire.sync(player, "beat", sfxEditor, "current_beat")
    wire.sync(player, "beat", velocityEditor, "current_beat")
    table.insert(m.widgets, player)
end

function _init()
    m.widgets = {}

    state = new(State)

    state.sfx = sfx.bar(0)

    map.level("SfxEditor")

    local entities = map.entities()

    _init_matrix_selector(entities)
    _init_mode_switch(entities)
    _init_knob(entities)
    _init_velocity_editor(entities)
    _init_sfx_editor(entities)
    _init_player(entities)

    -- force setting correct values
    if (state.on_change) then
        state:on_change()
    end
end

function _update()
    mouse._update(function()
    end, function()
    end, function()
    end)

    for w in all(m.widgets) do
        w:_update()
    end

end

function _draw()
    map.draw()

    for widget in all(m.widgets) do
        widget:_draw()
    end

    if (state.edit) then
        mouse._draw(64)
    else
        mouse._draw()
    end

end