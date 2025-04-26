local widgets = require("widgets")
local mouse = require("mouse")

function roundToHalf(num)
    local rounded_step = math.floor(num * 2)
    local final_rounded = rounded_step / 2
    return final_rounded
end

local State = {
    edit = false,
    current_bar = nil
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
    if inside_widget(self, p.x, p.y) then
        state.edit = true
    else
        state.edit = false
    end

    if ctrl.touching(0) ~= nil and self.current_edit ~= nil then
        -- update the edit state
        local local_x = p.x - self.x
        local added_duration = math.max(0, (roundToHalf((local_x + 4) / 16) - self.current_edit.beat))
        local duration = math.max(0.5, added_duration)
        self.current_edit.duration = duration
    elseif inside_widget(self, p.x, p.y) and ctrl.touched(0) ~= nil and self.current_edit == nil then
        -- create the edit state
        local local_x = p.x - self.x
        local local_y = p.y - self.y + 8

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
    elseif inside_widget(self, p.x, p.y) and ctrl.touched(1) ~= nil then
        local local_x = p.x - self.x
        local local_y = p.y - self.y + 8


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

BarEditor._draw = function(self)
    shape.rect(self.x, self.y, self.width, self.height, 4)
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
    end
end

local w = {}

function _init()
    w = {}
    test = {}
    state = new(State)
    state.current_bar = sfx.bar(0)
    map.level("BarEditor")
    local entities = map.entities()

    for b in all(entities["BarEditor"]) do
        local editor = new(BarEditor, b)
        table.insert(w, editor)
    end
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