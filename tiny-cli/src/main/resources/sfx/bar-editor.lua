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

    octave = 0,
    note = 0,
}

BarEditor._init = function(self)

end

BarEditor._update = function(self)
    local p = ctrl.touch()
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
            beat = roundToHalf((local_x) / 16.0),
            note = self.note,
            duration = 0.5
        }

        if (self.current_edit.beat ~= value.beat) then
            table.insert(test, value)

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
            note = self.current_edit.note
        }

        state.current_bar.set_note(value)

        self.current_edit = nil
    elseif inside_widget(self, p.x, p.y) and ctrl.touching(1) ~= nil then
        local local_x = p.x - self.x
        local local_y = p.y - self.y

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

    local octave = (y <= 97 and self.octave + 1) or self.octave
    local note = color_to_note[color]
    if note then
        self.note = note .. octave
    end
end

BarEditor._draw = function(self)
    -- line beats
    for x = self.x, self.x + self.width, 15 do
        shape.line(x, self.y, x, self.y + self.height, 3)
    end

    for note in all(state.current_bar.notes()) do
        local i = (note.notei) % 25

        local keys = self.keys_y[1 + #self.keys_y - i]
        local y = keys.y
        local h = keys.h

        shape.rectf(
                self.x + note.beat * 16, self.y + y,
                note.duration * 16, h,
                9
        )
    end

    if self.current_edit then
        local t = self.current_edit
        debug.console(t.note)
        local note = state.current_bar.note_data(t.note)
        local i = (note.notei) % 25
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
    shape.rect(self.x, self.y, self.width, self.height, 4)

    spr.sdraw(self.x - 3 * 8, self.y, 136, 64, 3 * 8, self.height)

    gfx.pal(2, 8)
    local p = ctrl.touch()
    local x = math.clamp(self.x, p.x, self.x + self.width)
    spr.sdraw(x - 8, self.y, 232, 64, 3 * 8, self.height)
    gfx.pal()

    print(self.note, self.x - 10, self.y - 8)
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
            wire.produce_to(knob, { "value" }, state, { "current_bar", "bpm" }, to_bpm)
            wire.produce_to(state, { "current_bar", "bpm" }, knob, { "value" }, from_bpm)

        end
    end

    for b in all(entities["MenuItem"]) do
        local button = widgets:create_menu_item(b)
        table.insert(w, button)
    end

    for instrument_name in all(entities["InstrumentName"]) do
        local inst = widgets:create_help(instrument_name)
        wire.consume_on_update(inst, { "label" }, state, { "current_instrument", "name" })
        table.insert(w, inst)

        local prev = wire.find_widget(w, inst.fields.Prev)
        wire.listen_to(prev, { "status" }, function(source, value)
            state.current_bar.instrument(state.current_bar.instrument() - 1)
            state.current_instrument = sfx.instrument(state.current_bar.instrument())
            if (state.on_change) then
                state:on_change()
            end
        end)
        local next = wire.find_widget(w, inst.fields.Next)
        wire.listen_to(next, { "status" }, function(source, value)
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

    for b in all(entities["Button"]) do
        if (b.fields.Type == "SAVE") then
            local button = widgets:create_button(b)
            button.on_change = function(self)
                sfx.save("test.sfx")
            end
            table.insert(w, button)
        end
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

    if ctrl.pressed(keys.space) then
        state.current_bar.play()
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