local function inside_widget(w, x, y, offset)
    local off = 0
    if (offset) then
        off = offset
    end

    return w.x - off <= x and
            x <= w.x + w.width + off and
            w.y - off <= y and
            y <= w.y + w.height + off
end

local Keyboard = {
    value = nil,
}

Keyboard._update = function(self)

    local spr_x = 0
    local spr_y = 224

    local color_to_note = {
        [1] = "C4",
        [3] = "Cs4",
        [4] = "D4",
        [5] = "Ds4",
        [8] = "E4",
        [13] = "F4",
        [12] = "Fs4",
        [15] = "G4",
        [11] = "Gs4",
        [17] = "A4",
        [7] = "As4",
        [10] = "B4"
    }
    local pos = ctrl.touch()

    local value
    if (ctrl.touching(0) and inside_widget(self, pos.x, pos.y)) then
        local relative_x = pos.x - self.x
        local relative_y = pos.y - self.y

        local color = spr.pget(relative_x + spr_x, relative_y + spr_y)
        value = color_to_note[color]
    else
        value = nil
    end

    -- There is a value change.
    if self.value ~= value then
        self.value = value
        if (self.on_change) then
            self:on_change()
        end
    end
end

Keyboard._draw = function(self)
    spr.sdraw(self.x, self.y, 0, 192, self.width, self.height)
end

return Keyboard