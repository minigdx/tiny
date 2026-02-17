local utils = require("widgets.utils")
local inside_widget = utils.inside_widget
local inside_rect = utils.inside_rect

local draw_option_row = function(x, y, width, sx, sy)
    spr.sdraw(x, y, sx, sy, 8, 8)                 -- left cap
    spr.sdraw(x, y + 8, sx, sy, 8, 8)                 -- left cap
    local last_x = x
    for cx = x + 8, x + width - 8 - 8, 8 do
        spr.sdraw(cx, y, sx + 8, sy, 8, 8)        -- center (repeated)
        spr.sdraw(cx, y + 8, sx + 8, sy, 8, 8)        -- center (repeated)
        last_x = cx
    end
    spr.sdraw(last_x + 8, y, sx + 8, sy, x + width - 8 - (last_x + 8), 8) -- partial center fill
    spr.sdraw(last_x + 8, y + 8, sx + 8, sy, x + width - 8 - (last_x + 8), 8) -- partial center fill

    spr.sdraw(x + width - 8, y, sx + 16, sy, 8, 8) -- right cap
    spr.sdraw(x + width - 8, y + 8, sx + 16, sy, 8, 8) -- right cap
end

local draw_last_option_row = function(x, y, width, sx, sy)
    spr.sdraw(x, y, sx, sy, 8, 16)
    local last_x = x
    for cx = x + 8, x + width - 8 - 8, 8 do
        spr.sdraw(cx, y, sx + 8, sy, 8, 16)
        last_x = cx
    end
    spr.sdraw(last_x + 8, y, sx + 8, sy, x + width - 8 - (last_x + 8), 16)
    spr.sdraw(x + width - 8, y, sx + 16, sy, 8, 16)
end

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
    spr.sdraw(self.x, self.y, 72, 32, 8, 16) -- left side
    local last_x = 0
    for x = self.x + 8, self.x + self.width - 10 - 11, 11 do
        spr.sdraw(x, self.y, 75, 32, 11, 16) -- body
        last_x = x
    end
    spr.sdraw(last_x + 11, self.y, 75, 32, self.x + self.width - 10 - (last_x + 11) ,16) -- right body
    spr.sdraw(self.x + self.width - 10, self.y, 86, 32, 10, 16) -- right side
    -- Selected option text
    print(self.value, self.x + self.width * 0.5 - #self.value * 3, self.y + 5, 3)

    -- Open state: draw options list below
    if self.open then
        -- Hover detection
        local mouse = ctrl.touch()
        self.hovered = nil

        for i = 1, #self.options do
            local oy = self.y + self.height + (i - 1) * self.item_height

            if inside_rect(mouse.x, mouse.y, self.x, oy, self.width, self.item_height) then
                self.hovered = i
                if i == #self.options then
                    draw_last_option_row(self.x, oy, self.width, 120, 32)
                else
                    draw_option_row(self.x, oy, self.width, 120, 32)
                end
            elseif i == #self.options then
                draw_last_option_row(self.x, oy, self.width, 96, 32)
            else
                draw_option_row(self.x, oy, self.width, 96, 32)
            end

            print(self.options[i], self.x + 4, oy + 2, 3)
        end
    end
end

return Dropdown
