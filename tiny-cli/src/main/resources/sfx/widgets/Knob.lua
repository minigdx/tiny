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
    color = 0,
}

Knob._init = function(self)
    local color_mapping = {
        Blue = 0,
        Green = 1,
        Purple = 2,
        Red = 3
    }
    self.color = color_mapping[self.fields.Color] or 0
    self.label = self.fields.Label
end

Knob._draw = function(self)
    local i = math.floor(self.value * 6)

    spr.sdraw(self.x, self.y, 16 + self.color * 16, 48, 16, 16)
    spr.sdraw(self.x, self.y, 16 + i * 16, 64, 16, 16)

    print(self.label, self.x, self.y + self.height + 2)
end

Knob._update = function(self)

    local touching = ctrl.touching(0)

    -- the click started in the widget?
    if touching ~= nil and inside_widget(self, touching.x, touching.y) then
        if self.start_value == nil then
            self.start_value = self.value
            if(self.on_press) then
                self:on_press()
            end
        end
        local touch = ctrl.touch()

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
        if self.start_value and self.on_release then
            self:on_release()
        end
        self.start_value = nil
        self.active_color = nil
    end
end

Knob.set_value = function(self, value)
    -- Ignore if the value didn't change
    if value == self.value then
        return
    end
    self.value = value
    if self.on_change then
        self:on_change()
    end
end

return Knob