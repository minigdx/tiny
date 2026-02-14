local utils = {}

utils.inside_widget = function(w, x, y, offset)
    local off = 0
    if (offset) then
        off = offset
    end

    return w.x - off <= x and
            x <= w.x + w.width + off and
            w.y - off <= y and
            y <= w.y + w.height + off
end

utils.inside_rect = function(x, y, rx, ry, rw, rh)
    return rx <= x and x <= rx + rw and
            ry <= y and y <= ry + rh
end

utils.on_update = function(self, listener)
    table.insert(self.listeners, listener)
end

utils.fire_on_update = function(self, value)
    for l in all(self.listeners) do
        l(self, value)
    end
end

utils.set_value = function(self, value)
    self.value = value
    if (self.on_change) then
        self:on_change()
    end
    self:fire_on_update(value)
end

return utils
