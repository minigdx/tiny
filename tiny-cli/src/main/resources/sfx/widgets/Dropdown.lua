local utils = require("widgets.utils")
local inside_widget = utils.inside_widget
local inside_rect = utils.inside_rect

local set_value = function(self, value)
    for i, opt in ipairs(self.options) do
        if opt == value then
            self.selected = i
            self.value = opt
            if self.on_change then
                self:on_change()
            end
            self:fire_on_update(self.value)
            return
        end
    end
end

local set_selected = function(self, index)
    if index >= 1 and index <= #self.options then
        self.selected = index
        self.value = self.options[index]
        if self.on_change then
            self:on_change()
        end
        self:fire_on_update(self.value)
    end
end

local Dropdown = {
    x = 0,
    y = 0,
    width = 64,
    height = 10,
    options = {},
    selected = 1,
    value = "",
    open = false,
    hovered = nil,
    item_height = 10,
    listeners = {},
    on_update = utils.on_update,
    fire_on_update = utils.fire_on_update,
    set_value = set_value,
    set_selected = set_selected,
}

Dropdown._init = function(self)
    self.item_height = self.height
    if #self.options > 0 then
        self.value = self.options[self.selected] or self.options[1]
    end
end

Dropdown._update = function(self)
    local pos = ctrl.touched(0)
    if pos == nil then
        return
    end

    if self.open then
        -- Check if an option row was clicked
        for i = 1, #self.options do
            local oy = self.y + self.height + (i - 1) * self.item_height
            if inside_rect(pos.x, pos.y, self.x, oy, self.width, self.item_height) then
                self.selected = i
                self.value = self.options[i]
                self.open = false
                self.hovered = nil
                if self.on_change then
                    self:on_change()
                end
                self:fire_on_update(self.value)
                return
            end
        end

        -- Check if the closed box was clicked (toggle close)
        if inside_widget(self, pos.x, pos.y) then
            self.open = false
            self.hovered = nil
            return
        end

        -- Clicked outside — close
        self.open = false
        self.hovered = nil
    else
        -- Closed: click on the widget opens it
        if inside_widget(self, pos.x, pos.y) then
            self.open = true
        end
    end
end

Dropdown._draw = function(self)
    -- Background and border for the closed box
    shape.rectf(self.x, self.y, self.width, self.height, 1)
    shape.rect(self.x, self.y, self.width, self.height)

    -- Selected option text
    print(self.value, self.x + 2, self.y + 2)

    -- Downward triangle indicator on the right side
    local tx = self.x + self.width - 7
    local ty = self.y + 3
    shape.trianglef(tx, ty, tx + 4, ty, tx + 2, ty + 3)

    -- Open state: draw options list below
    if self.open then
        local list_h = #self.options * self.item_height

        -- Options container background and border
        shape.rectf(self.x, self.y + self.height, self.width, list_h, 1)
        shape.rect(self.x, self.y + self.height, self.width, list_h)

        -- Hover detection
        local mouse = ctrl.touch()
        self.hovered = nil

        for i = 1, #self.options do
            local oy = self.y + self.height + (i - 1) * self.item_height

            -- Highlight hovered option
            if inside_rect(mouse.x, mouse.y, self.x, oy, self.width, self.item_height) then
                self.hovered = i
                shape.rectf(self.x + 1, oy, self.width - 2, self.item_height, 9)
            end

            -- Option text
            print(self.options[i], self.x + 2, oy + 2)
        end
    end
end

return Dropdown
