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

local BarEditor = {

}

local test = {}

BarEditor._update = function(self)
    local p = ctrl.touch()
    p = {x = p.x, y = p.y + 8}
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
        local local_y = p.y - self.y

        local value = {
            beat = roundToHalf((local_x) / 16.0),
            note = math.floor(local_y / 4),
            duration = 0.5
        }

        if (self.current_edit.beat ~= value.beat) then
            table.insert(test, value)

            state.current_bar.set_note(value)

            self.current_edit = {
                beat = roundToHalf((local_x) / 16.0),
                note = math.floor(local_y / 4),
                duration = 0.5
            }
        end
    elseif inside_widget(self, p.x, p.y) and ctrl.touched(0) ~= nil and self.current_edit == nil then
        -- create the edit state
        local local_x = p.x - self.x
        local local_y = p.y - self.y

        self.current_edit = {
            beat = roundToHalf((local_x) / 16.0),
            note = math.floor(local_y / 4),
            duration = 0.5
        }
    elseif ctrl.touched(0) == nil and self.current_edit ~= nil then
        -- commit the edit state
        local value = {
            duration = self.current_edit.duration,
            beat = self.current_edit.beat,
            note = self.current_edit.note
        }
        table.insert(test, value)

        state.current_bar.set_note(value)

        self.current_edit = nil
    elseif inside_widget(self, p.x, p.y) and ctrl.touching(1) ~= nil then
        local local_x = p.x - self.x
        local local_y = p.y - self.y

        local value = {
            beat = roundToHalf((local_x) / 16.0),
            note = math.floor(local_y / 4),
        }

        for index, t in rpairs(test) do
            if t.beat <= value.beat and value.beat <= t.beat + t.duration then
                if t.note == value.note then
                    table.remove(test, index)
                end
            end
        end
        state.current_bar.remove_note(value)
    end
end

local sharp_notes = { [1] = true, [3] = true, [6] = true, [8] = true, [10] = true }

BarEditor._draw = function(self)

    -- line notes
    for octave = 0, 3 do
        for note = 0, 11 do
            local x = self.x
            local y = (self.y + self.height) - (octave * 4 * 12) - note * 4 - 1 - 4
            if sharp_notes[note] then
                gfx.dither(0xA5A5)
                shape.rectf(x + 1, y, self.width - 3, 4, 3)
                gfx.dither()
            elseif note == 4 or note == 11 then
                gfx.dither(0x3333)
                shape.line(x + 1, y, x + self.width - 3, y, 3)
                gfx.dither()
            end
        end
    end
    -- line beats
    for x = self.x, self.x + self.width, 16 do
        shape.line(x, self.y, x, self.y + self.height, 3)
    end

    for t in all(test) do
        shape.rectf(
                self.x + t.beat * 16, self.y + t.note * 4,
                t.duration * 16, 4,
                9
        )
    end

    if self.current_edit then
        local t = self.current_edit
        shape.rect(
                self.x + t.beat * 16, self.y + t.note * 4,
                t.duration * 16, 4,
                8
        )
        --
    end

    -- border
    shape.rect(self.x, self.y, self.width, self.height, 4)
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
        table.insert(w, editor)
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
        if(knob.fields.Label == "BPM") then
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