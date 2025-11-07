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

local MenuItem = {
    _type = "MenuItem",
    spr = nil,
    hold = false,
    status = 0,
    active = 0,
    help = "",
    on_click = function()
    end,
    on_hover = function()
    end,
    listeners = {},
    on_update = on_update,
    fire_on_update = fire_on_update,
    set_value = set_value,
}

local menuItems = {}

MenuItem._update = function(self)
    local pos = ctrl.touch()
    if not self.hold then
        self.active = 0
    end

    if inside_widget(self, pos.x, pos.y) then
        if self.active == 0 then
            self.status = 1
        end
        if ctrl.touched(0) then
            self:fire_on_update(self.status)
            if(self.on_change) then
                self:on_change()
            end
            if self.hold then
                for i in all(menuItems) do
                    i.active = 0
                end
            end
            self.active = 1
            self.status = 0
        end
        self:on_hover()
    else
        self.status = 0
    end

end

MenuItem._draw = function(self)
    if self.spr ~= nil then
        spr.draw(self.spr + self.status * 128 + self.active * (128 + 32), self.x, self.y)
    end

    if self.label ~= nil then
        print(self.value, self.x + 5, self.y + 2)
    end
end

return { MenuItem = MenuItem, menuItems = menuItems }