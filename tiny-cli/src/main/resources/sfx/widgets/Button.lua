local utils = require("widgets.utils")
local inside_widget = utils.inside_widget

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