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

local Knob = {
    label = "",
    value = 0,
    x = 0,
    y = 0,
    width = 16,
    height = 16,
    enabled = true,
    listeners = {},
    on_update = on_update,
    fire_on_update = fire_on_update,
    set_value = set_value,
}

Knob._draw = function(self)
    local angle = (1.8 * math.pi) * self.value + math.pi * 0.6

    local target_x = math.cos(angle) * 8 + self.x + 8
    local target_y = math.sin(angle) * 8 + self.y + 8

    spr.sdraw(self.x, self.y, 0, 64, 16, 16)
    shape.line(self.x + 8, self.y + 8, target_x, target_y, 9)
    print(self.label, self.x - 1, self.y + 18)

    if self.is_hover or self.active_color then
        local c = 9
        if (self.active_color) then
            c = self.active_color
        end
        shape.rect(self.x, self.y, self.width, self.height, c)
    end
end

Knob._update = function(self)

    local touching = ctrl.touching(0)

    -- the click started in the widget?
    if touching ~= nil and inside_widget(self, touching.x, touching.y) then
        if self.start_value == nil then
            self.start_value = self.value
        end
        local touch = ctrl.touch()

        self.active_color = 11

        local dst = self.y + 4 - touch.y
        local percent = math.max(math.min(1, dst / 32), -1)
        local value = math.min(math.max(0, self.start_value + percent), 1)
        self:set_value(value)

    end

    local pos = ctrl.touch()
    if inside_widget(self, pos.x, pos.y) then
        if self.on_hover ~= nil then
            self:on_hover()
        end
        self.is_hover = true
    else
        self.is_hover = false
    end

    if touching == nil then
        self.start_value = nil
        self.active_color = nil
    end
end

return Knob