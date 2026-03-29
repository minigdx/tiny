local utils = require("widgets.utils")
local inside_widget = utils.inside_widget

local Button = {
    x = 0,
    y = 0,
    width = 13,
    height = 13,
    enabled = true,
    grouped = true,
    status = 0, -- 0 : idle ; 1 : over ; 2 : active
    overlay = {x = 0, y = 0}, -- sprite index,
    on_active_button = function(current, prec)
    end,
    listeners = {},
    on_update = utils.on_update,
    fire_on_update = utils.fire_on_update,
}

Button._update = function(self)
    if self.status == 2 then
        return
    end

    local pos = ctrl.touch()

    if inside_widget(self, pos.x, pos.y) then
        self.status = 1
        local touched = ctrl.touched(0)
        if touched then
            if (self.on_change) then
                self:on_change()
            end
        end
    else
        self.status = 0
    end

end

Button._draw = function(self)
    local prev = spr.sheet(2)

    local sy = 56
    if self.status > 0 then
        sy = 72
    end

    spr.sdraw(self.x, self.y, 0, sy, self.width, self.height)

    if self.overlay ~= nil then
        spr.sdraw(self.x, self.y, self.overlay.x, self.overlay.y, self.width, self.height)
    end

    spr.sheet(prev)
end

return Button