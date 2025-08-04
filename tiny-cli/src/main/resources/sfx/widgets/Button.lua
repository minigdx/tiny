local on_update = function(self, listener)
    table.insert(self.listeners, listener)
end

local fire_on_update = function(self, value)
    for l in all(self.listeners) do
        l(self, value)
    end
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

local Button = {
    x = 0,
    y = 0,
    width = 16,
    height = 16,
    enabled = true,
    grouped = true,
    status = 0, -- 0 : idle ; 1 : over ; 2 : active
    overlay = {x = 0, y = 0}, -- sprite index,
    on_active_button = function(current, prec)
    end,
    listeners = {},
    on_update = on_update,
    fire_on_update = fire_on_update,
}

Button._update = function(self)
    if self.status == 2 then
        return
    end

    local pos = ctrl.touch()

    if inside_widget(self, pos.x, pos.y) then
        self.status = 1
        if self.on_hover ~= nil then
            self:on_hover()
        end
        local touched = ctrl.touched(0)
        if touched then
            self:fire_on_update(self.status)
            if (self.on_change) then
                self:on_change()
            end
        end
    else
        self.status = 0
    end

end

Button._draw = function(self)
    local background = 0
    if self.status > 0 then
        background = 16
    end

    spr.sdraw(self.x, self.y, 80 + background, 0, self.width, self.height)

    if self.overlay ~= nil then
        spr.sdraw(self.x, self.y, self.overlay.x, self.overlay.y, self.width, self.height)
    end
end

return Button