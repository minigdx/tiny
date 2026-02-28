local utils = require("widgets.utils")
local inside_widget = utils.inside_widget

local Keyboard = {
    value = nil,
}

Keyboard._update = function(self)

    local spr_x = 0
    local spr_y = 224

    local color_to_note = {
        [0] = "C4",
        [1] = "Cs4",
        [4] = "D4",
        [9] = "Ds4",
        [11] = "E4",
        [3] = "F4",
        [5] = "Fs4",
        [8] = "G4",
        [2] = "Gs4",
        [6] = "A4",
        [7] = "As4",
        -- B4: sprite uses wrong color (110,184,135), maps to palette 1 (same as Cs4)
        -- Fix sprite-sheet.png to use palette 10 (#FFF1E8) for B4, then add: [10] = "B4"
    }

    local key_to_note = {
        a = "C4",
        w = "Cs4",
        s = "D4",
        e = "Ds4",
        d = "E4",
        f = "F4",
        t = "Fs4",
        g = "G4",
        y = "Gs4",
        h = "A4",
        u = "As4",
        j = "B4"
    }

    local pos = ctrl.touch()

    local value
    if (ctrl.touching(0) and inside_widget(self, pos.x, pos.y)) then
        local relative_x = pos.x - self.x
        local relative_y = pos.y - self.y

        local prev = spr.sheet(2)
        local color = spr.pget(relative_x + spr_x, relative_y + spr_y)
        spr.sheet(prev)
        value = color_to_note[color]
    else
        -- No mouse/touch input: check physical keyboard
        for k, note in pairs(key_to_note) do
            if ctrl.pressing(keys[k]) then
                value = note
                self._held_key = k
                break
            end
        end
        -- Detect key release
        if value == nil and self._held_key then
            if not ctrl.pressing(keys[self._held_key]) then
                self._held_key = nil
            end
        end
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
    local prev = spr.sheet(2)
    spr.sdraw(self.x, self.y, 0, 192, self.width, self.height)
    spr.sheet(prev)
end

return Keyboard