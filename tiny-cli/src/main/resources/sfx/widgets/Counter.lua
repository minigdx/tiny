local utils = require("widgets.utils")
local inside_widget = utils.inside_widget

local Counter = {
    x = 0,
    y = 0,
    width = 40,
    height = 16,
    value = 2,
    min = 0,
    max = 7,
    label = "",
    status = 0,
    listeners = {},
    on_update = utils.on_update,
    fire_on_update = utils.fire_on_update,
}

Counter._update = function(self)
    local pos = ctrl.touched(0)
    if pos ~= nil and inside_widget(self, pos.x, pos.y) then
        local local_x = pos.x - self.x
        if local_x < 12 then
            self:set_value(self.value - 1)
        elseif local_x > self.width - 12 then
            self:set_value(self.value + 1)
        end
    end
end

Counter.set_value = function(self, value)
    local clamped = math.clamp(self.min, value, self.max)
    if clamped ~= self.value then
        self.value = clamped
        if self.on_change then
            self:on_change()
        end
        self:fire_on_update(clamped)
    end
end

Counter._draw = function(self)
    shape.rectf(self.x, self.y, self.width, self.height, 2)
    shape.rect(self.x, self.y, self.width, self.height, 1)

    text.font("monogram")

    local left_color = (self.value > self.min) and 1 or 3
    text.print("<", self.x + 3, self.y + 2, left_color)

    local right_color = (self.value < self.max) and 1 or 3
    text.print(">", self.x + self.width - 9, self.y + 2, right_color)

    local val_str = tostring(self.value)
    local val_x = self.x + math.floor(self.width / 2) - math.floor(#val_str * 3)
    text.print(val_str, val_x, self.y + 2, 1)

    text.font()
end

return Counter
