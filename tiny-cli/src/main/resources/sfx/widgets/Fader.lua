local on_update = function(self, listener)
    table.insert(self.listeners, listener)
end

local fire_on_update = function(self, value)
    for l in all(self.listeners) do
        l(self, value)
    end
end

local set_value = function(self, value)
    self.value = value
    if (self.on_change) then
        self:on_change()
    end
    self:fire_on_update(value)
end

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
    on_update = on_update,
    fire_on_update = fire_on_update,
    set_value = set_value,
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
    local color = self.disabled_color

    if self.value ~= nil and self.value > 0 then
        color = self.tip_color
    end
    local y = self.height - self.value * self.height
    local tipy = self.y + y
    shape.rectf(self.x + 1, tipy, self.width - 2, 2, self.tip_color)
end

return Fader