local utils = require("widgets.utils")
local inside_widget = utils.inside_widget

local Fader = {
    x = 0,
    y = 0,
    width = 11,
    height = 80,
    enabled = true,
    min_value = 0,
    max_value = 10,
    value = 0,
    tip_color = 9,
    disabled_color = 7,
    label = "",
    type = "fader",
    data = nil,
    index = 0,
    on_value_update = function(fader, value)
    end,
    listeners = {},
    on_update = utils.on_update,
    fire_on_update = utils.fire_on_update,
    set_value = utils.set_value,
}

Fader._update = function(self)
    local pos = ctrl.touch()
    if inside_widget(self.hitbox, pos.x, pos.y) then
        if self.on_hover ~= nil then
            self:on_hover()
        end

        if ctrl.touching(0) then
            local percent = math.max(0.0, 1.0 - ((pos.y - self.y) / self.height))
            self.value = percent

            -- todo: to be removed as fire_on_update should be used instead
            if self.on_value_update then
                self:on_value_update(self.value)
            end

            self:fire_on_update(self.value)
        end
    end
end

Fader._draw = function(self)
    -- Vertical line (track)
    local line_x = self.x + math.floor(self.width / 2)
    shape.line(line_x, self.y, line_x, self.y + self.height, self.disabled_color)

    -- Sliding handle rectangle
    local handle_h = 4
    local y = self.height - self.value * self.height
    local tipy = self.y + y - math.floor(handle_h / 2)
    shape.rectf(self.x, tipy, self.width, handle_h, self.tip_color)
end

return Fader