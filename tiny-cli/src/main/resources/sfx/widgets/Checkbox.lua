local utils = require("widgets.utils")
local inside_widget = utils.inside_widget

local Checkbox = {
    label = "",
    value = false,
    x = 0,
    y = 0,
    width = 8,
    height = 8,
    enabled = true
}

Checkbox._update = function(self)
    local pos = ctrl.touched(0)
    if pos ~= nil then
        local w = {
            x = self.x,
            y = self.y,
            height = self.height,
            width = self.width + #self.label * 4
        }
        if inside_widget(w, pos.x, pos.y) then
            self.value = not self.value
            if self.on_change then
                self:on_change()
            end
        end
    end
end

Checkbox._draw = function(self)
    if self.value then
        spr.sdraw(self.x, self.y, 32, 32, 8, 8)
    else
        spr.sdraw(self.x, self.y, 16, 32, 8, 8)
    end
    print(self.label, self.x + 10, self.y + 2)
end

return Checkbox