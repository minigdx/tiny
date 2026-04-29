local utils = require("widgets.utils")
local inside_widget = utils.inside_widget

local Switch = {
    x = 0,
    y = 0,
    width = 32,
    height = 16,
    right_active = false,
    enabled = true,
    listeners = {},
    on_update = utils.on_update,
    fire_on_update = utils.fire_on_update,
}

Switch._update = function(self)
    local pos = ctrl.touched(0)
    if pos ~= nil and inside_widget(self, pos.x, pos.y) then
        self.right_active = not self.right_active
        if self.on_change then
            self:on_change()
        end
        self:fire_on_update(self.right_active)
    end
end

Switch._draw = function(self)
    local prev = spr.sheet(2)
    local sy = self.right_active and 120 or 104
    spr.sdraw(self.x, self.y, 80, sy, self.width, self.height)
    spr.sheet(prev)
end

return Switch
