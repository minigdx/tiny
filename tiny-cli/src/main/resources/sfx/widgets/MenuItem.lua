local utils = require("widgets.utils")
local inside_widget = utils.inside_widget

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
    on_update = utils.on_update,
    fire_on_update = utils.fire_on_update,
    set_value = utils.set_value,
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