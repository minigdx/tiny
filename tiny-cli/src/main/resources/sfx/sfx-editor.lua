local widgets = require("widgets")
local mouse = require("mouse")
local wire = require("wire")

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

    -- the instrument of the current_bar
    current_instrument = nil,
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

local SfxMatrix = {
    hover_index = nil,
    value = nil,
    size = 16
}

SfxMatrix._update = function(self)
    local p = ctrl.touch()

    local cols = math.ceil(math.sqrt(self.size))
    local rows = math.ceil(self.size / cols)
    local cell_width = self.width / cols
    local cell_height = self.height / rows

    if inside_widget(self, p.x, p.y) then
        local x = p.x - self.x
        local y = p.y - self.y

        local col = math.floor(x / cell_width)
        local row = math.floor(y / cell_height)
        local index = col + row * cols

        if index < self.size then
            self.hover_index = index
        else
            self.hover_index = nil
        end
    else
        self.hover_index = nil
    end

    if (self.hover_index and ctrl.touched(0)) then
        self.value = self.hover_index
        if (self.on_change) then
            self:on_change()
        end
    end
end

SfxMatrix._draw = function(self)
    local cols = math.ceil(math.sqrt(self.size))
    local rows = math.ceil(self.size / cols)
    local cell_width = self.width / cols
    local cell_height = self.height / rows
    
    local index = 0

    for row = 0, rows - 1 do
        for col = 0, cols - 1 do
            if index < self.size then
                local x = self.x + col * cell_width
                local y = self.y + row * cell_height

                if (self.value == index) then
                    shape.rectf(x, y, cell_width, cell_height, 3)
                elseif (self.hover_index == index) then
                    shape.rect(x, y, cell_width, cell_height, 3)
                else
                    shape.rect(x, y, cell_width, cell_height, 4)
                end
                print(index, x + 2, y + 2)
            end
            index = index + 1
        end
    end
end

local InstrumentName = {
    index = 0
}

InstrumentName._update = function(self)

end

InstrumentName._draw = function(self)
    local x, y = 0, 160
    local ox = (self.index % 4) * 16
    local oy = math.floor(self.index / 4) * 16
    spr.sdraw(self.x, self.y, x + ox, y + oy, 16, 16)
end

local VolumeEditor = {

}

VolumeEditor._update = function(self)
    local p = ctrl.touch()
    if ctrl.touching(0) and inside_widget(self, p.x, p.y) then
        local local_x = p.x - self.x
        local local_y = p.y - self.y
        local beat = roundToHalf((local_x) / 16.0)

        if local_y > self.height - 8 then
            state.current_bar.set_volume(beat, 0)
        else
            local volume = math.clamp(0, 1.0 - (local_y / (self.height - 8)), 1.0)
            state.current_bar.set_volume(beat, volume)
        end

    end
end

VolumeEditor._draw = function(self)
    local low = self.y + self.height - 8

    for note in all(state.current_bar.notes()) do
        local volume = note.volume * (self.height - 8)
        for nx = self.x + note.beat * 16, self.x + (note.duration + note.beat) * 16, 8 do
            shape.rectf(
                    nx + 1, low - volume,
                    8 - 2, volume,
                    9
            )
        end

    end

    shape.line(self.x, low, self.x + self.width - 1, low, 9)
    shape.rect(self.x, self.y, self.width, self.height, 9)
end

local CursorEditor = {
    editor = nil,
    beat = 0,
    note = 0,
    step_x = 8, -- adjust regarding the size of half of a bit on screen,
    time = 0,
    play = false,
}

CursorEditor._update = function(self)
    if (ctrl.pressed(keys.left)) then
        self.beat = self.beat - 1
    elseif (ctrl.pressed(keys.right)) then
        self.beat = self.beat + 1
    end

    if ctrl.pressed(keys.space) then
        self.beat = 0
        self.play = not self.play
        self.time = 0

        if self.play then
            state.current_bar.play()
        end
    end

    if self.play then
        self.time = self.time + tiny.dt
        self.beat = self.time * (state.current_bar.bpm / 60) * 2

        if (self.beat >= 32) then
            self.play = false
            self.beat = 0
        end
    end

    self.beat = math.clamp(0, self.beat, 32)
end

CursorEditor._draw = function(self)
    local x = self.editor.x + self.beat * self.step_x
    local y = self.editor.y - 4
    -- right
    spr.sdraw(x, y, 248, 44, 4, 4)
    -- left
    spr.sdraw(x - 4, y, 248, 44, 4, 4, true)
end

local BarEditor = {
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

    octave = 4,
    note = 0,
    hitbox_octaves = {
        { x = -16, y = -8, width = 8, height = 8 },
        { x = -16, y = 0, width = 8, height = 8 },
    }
}

BarEditor._init = function(self)

end

BarEditor._update = function(self)
    local p = ctrl.touch()

    -- octave management
    -- (as the octave buttons is using the default mouse, the mouse position needs to be checked without the offset)
    local up = self.hitbox_octaves[1]
    local down = self.hitbox_octaves[2]
    if ctrl.touched(0) then
        if inside_widget({ x = self.x + up.x, y = self.y + up.y, width = up.width, height = up.height }, p.x, p.y) then
            self.octave = self.octave + 1
        elseif inside_widget({ x = self.x + down.x, y = self.y + self.height + down.y, width = down.width, height = down.height }, p.x, p.y) then
            self.octave = self.octave - 1
        end

        self.octave = math.clamp(0, self.octave, 8)
    end

    p = { x = p.x, y = p.y + 8 }
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

        local current_beat = roundToHalf((local_x) / 16.0)
        debug.console("beat -> ", value.beat, " current beat -> ", current_beat)
        if (self.current_edit.beat ~= current_beat) then

            debug.console("set_note", value)
            state.current_bar.set_note(value)

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

        debug.console("set_note", value)

        state.current_bar.set_note(value)

        self.current_edit = nil
    elseif inside_widget(self, p.x, p.y) and ctrl.touching(1) ~= nil then
        local local_x = p.x - self.x

        local value = {
            beat = roundToHalf((local_x) / 16.0),
            note = self.note,
        }

        state.current_bar.remove_note(value)
    end

    -- get the current note regarding the y position.
    -- the note is computed fro the color of the color virtual keyboard
    local y = math.clamp(0, p.y - self.y, 192)
    local color = spr.pget(164, 64 + y)

    local color_to_note = {
        [16] = "C",
        [15] = "Cs",
        [14] = "D",
        [13] = "Ds",
        [12] = "E",
        [11] = "F",
        [10] = "Fs",
        [9] = "G",
        [7] = "Gs",
        [6] = "A",
        [5] = "As",
        [4] = "B"
    }

    local octave = (y <= 95 and self.octave + 1) or self.octave
    local note = color_to_note[color]
    if note then
        self.note = note .. octave
    end
end

BarEditor._draw = function(self)
    -- line beats
    for x = self.x, self.x + self.width, 16 do
        shape.line(x, self.y, x, self.y + self.height, 3)
    end

    for note in all(state.current_bar.notes()) do

        local i = note.notei - self.octave * 12

        if self.octave <= note.octave and note.octave < self.octave + 2 then
            local keys = self.keys_y[1 + #self.keys_y - i]
            local y = keys.y
            local h = keys.h

            shape.rectf(
                    self.x + note.beat * 16, self.y + y,
                    note.duration * 16, h,
                    9
            )
        end
    end

    if self.current_edit then
        local t = self.current_edit

        local note = state.current_bar.note_data(t.note)
        local i = note.notei - self.octave * 12
        local keys = self.keys_y[1 + #self.keys_y - i]

        local y = keys.y
        local h = keys.h

        shape.rect(
                self.x + t.beat * 16, self.y + y,
                t.duration * 16, h,
                8
        )
    end

    -- border
    shape.rect(self.x, self.y, self.width, self.height + 1, 4)

    -- keyboard
    spr.sdraw(self.x - 3 * 8, self.y, 136, 64, 3 * 8, self.height)
    -- octave up
    local up = self.hitbox_octaves[1]
    spr.sdraw(self.x + up.x, self.y + up.y, 240, 40, up.width, up.height)
    -- octave down
    local down = self.hitbox_octaves[2]
    spr.sdraw(self.x + down.x, self.y + self.height + down.y, 240, 40, down.width, down.height, false, true)

    local p = ctrl.touch()
    local x = math.clamp(self.x, p.x, self.x + self.width)

    print(self.note, x, self.y - 8)
end

local w = {}

function _init()
    w = {}
    test = {}
    state = new(State)

    state.current_bar = sfx.bar(0)
    state.current_instrument = sfx.instrument(state.current_bar.instrument())

    map.level("BarEditor")
    local entities = map.entities()

    for b in all(entities["BarEditor"]) do
        local editor = new(BarEditor, b)
        local cursor = new(CursorEditor)
        cursor.editor = editor

        editor:_init()
        table.insert(w, editor)
        table.insert(w, cursor)
    end

    local to_bpm = function(source, target, value)
        return 80 + 300 * value

    end

    local from_bpm = function(source, target, value)
        return (value - 80) / 300
    end

    for k in all(entities["Knob"]) do
        local knob = widgets:create_knob(k)
        table.insert(w, knob)
        if (knob.fields.Label == "BPM") then
            wire.bind(knob, "value", state, "current_bar.bpm", to_bpm)

        end
    end

    for b in all(entities["MenuItem"]) do
        local button = widgets:create_menu_item(b)
        table.insert(w, button)
    end

    for instrument_name in all(entities["InstrumentName"]) do
        local label = new(InstrumentName, instrument_name)
        wire.sync(state, "current_instrument.index", label, "index", nil, "update")
        table.insert(w, label)

        local prev = wire.find_widget(w, label.fields.Prev)
        wire.listen(prev, "status", function(source, value)
            state.current_bar.instrument(state.current_bar.instrument() - 1)
            state.current_instrument = sfx.instrument(state.current_bar.instrument())
            if (state.on_change) then
                state:on_change()
            end
        end)
        local next = wire.find_widget(w, label.fields.Next)
        wire.listen(next, "status", function(source, value)
            state.current_bar.instrument(state.current_bar.instrument() + 1)
            state.current_instrument = sfx.instrument(state.current_bar.instrument())
            if (state.on_change) then
                state:on_change()
            end
        end)
    end

    for mode in all(entities["EditorMode"]) do
        local button = widgets:create_mode_switch(mode)
        table.insert(w, button)
    end

    for mode in all(entities["VolumeEditor"]) do
        local button = new(VolumeEditor, mode)
        table.insert(w, button)
    end

    for b in all(entities["Button"]) do
        if (b.fields.Type == "SAVE") then
            local button = widgets:create_button(b)
            button.on_change = function(self)
                sfx.save("test.sfx")
            end
            table.insert(w, button)
        end
    end

    for b in all(entities["SfxMatrix"]) do
        local button = new(SfxMatrix, b)

        wire.sync(state, "current_bar.index", button, "value", nil, "update")
        wire.listen(button, "value", function(source, value)
            state.current_bar = sfx.bar(value)
            if (state.on_change) then
                state:on_change()
            end
        end)
        table.insert(w, button)
    end

    state:on_change()
end

function _update()
    mouse._update(function()
    end, function()
    end, function()
    end)

    for widget in all(w) do
        widget:_update()
    end
end

function _draw()
    map.draw()

    for widget in all(w) do
        widget:_draw()
    end

    if (state.edit) then
        mouse._draw(25 + 32)
    else
        mouse._draw()
    end

end