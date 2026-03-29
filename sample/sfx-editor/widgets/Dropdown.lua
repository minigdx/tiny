local utils = require("widgets.utils")
local inside_widget = utils.inside_widget
local inside_rect = utils.inside_rect

local draw_9patch = function(x, y, width, height, sx, sy)
    local corner = 5
    local edge = 14
    local inner_w = width - corner * 2
    local inner_h = height - corner * 2

    -- Corners
    spr.sdraw(x, y, sx, sy, corner, corner)
    spr.sdraw(x + width - corner, y, sx + 19, sy, corner, corner)
    spr.sdraw(x, y + height - corner, sx, sy + 19, corner, corner)
    spr.sdraw(x + width - corner, y + height - corner, sx + 19, sy + 19, corner, corner)

    -- Top edge
    local cx = 0
    while cx < inner_w do
        local tw = math.min(edge, inner_w - cx)
        spr.sdraw(x + corner + cx, y, sx + 5, sy, tw, corner)
        cx = cx + tw
    end

    -- Bottom edge
    cx = 0
    while cx < inner_w do
        local tw = math.min(edge, inner_w - cx)
        spr.sdraw(x + corner + cx, y + height - corner, sx + 5, sy + 19, tw, corner)
        cx = cx + tw
    end

    -- Left edge
    local cy = 0
    while cy < inner_h do
        local th = math.min(edge, inner_h - cy)
        spr.sdraw(x, y + corner + cy, sx, sy + 5, corner, th)
        cy = cy + th
    end

    -- Right edge
    cy = 0
    while cy < inner_h do
        local th = math.min(edge, inner_h - cy)
        spr.sdraw(x + width - corner, y + corner + cy, sx + 19, sy + 5, corner, th)
        cy = cy + th
    end

    -- Center fill
    cy = 0
    while cy < inner_h do
        local th = math.min(edge, inner_h - cy)
        cx = 0
        while cx < inner_w do
            local tw = math.min(edge, inner_w - cx)
            spr.sdraw(x + corner + cx, y + corner + cy, sx + 5, sy + 5, tw, th)
            cx = cx + tw
        end
        cy = cy + th
    end
end

local draw_headless_9patch = function(x, y, width, height, sx, sy)
    local corner = 5
    local edge = 14
    local inner_w = width - corner * 2
    local inner_h = height - corner

    -- Bottom corners
    spr.sdraw(x, y + height - corner, sx, sy + 19, corner, corner)
    spr.sdraw(x + width - corner, y + height - corner, sx + 19, sy + 19, corner, corner)

    -- Bottom edge
    local cx = 0
    while cx < inner_w do
        local tw = math.min(edge, inner_w - cx)
        spr.sdraw(x + corner + cx, y + height - corner, sx + 5, sy + 19, tw, corner)
        cx = cx + tw
    end

    -- Left edge
    local cy = 0
    while cy < inner_h do
        local th = math.min(edge, inner_h - cy)
        spr.sdraw(x, y + cy, sx, sy + 5, corner, th)
        cy = cy + th
    end

    -- Right edge
    cy = 0
    while cy < inner_h do
        local th = math.min(edge, inner_h - cy)
        spr.sdraw(x + width - corner, y + cy, sx + 19, sy + 5, corner, th)
        cy = cy + th
    end

    -- Center fill
    cy = 0
    while cy < inner_h do
        local th = math.min(edge, inner_h - cy)
        cx = 0
        while cx < inner_w do
            local tw = math.min(edge, inner_w - cx)
            spr.sdraw(x + corner + cx, y + cy, sx + 5, sy + 5, tw, th)
            cx = cx + tw
        end
        cy = cy + th
    end
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
    height = 16,
    options = {},
    selected = 1,
    value = "",
    open = false,
    hovered = nil,
    item_height = 16,
    listeners = {},
    on_update = utils.on_update,
    fire_on_update = utils.fire_on_update,
    set_value = set_value,
    set_selected = set_selected,
}

Dropdown._init = function(self)
    self.height = math.max(self.height, 16)
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
    local prev = spr.sheet(2)

    -- Closed box: White variant
    local sx = 1 * 24
    local sy = 0
    draw_9patch(self.x, self.y, self.width, self.height, sx, sy)

    -- Dropdown icon
    local icon_x = self.x + self.width - 8 - 4
    local icon_y = self.y + math.floor((self.height - 8) / 2)
    spr.sdraw(icon_x, icon_y, 8, 32, 8, 8)

    spr.sheet(prev)

    -- Selected value with monogram font
    text.font("monogram")
    local ty = self.y + 2
    text.print(self.value, self.x + 4, ty, 1)
    text.font()

    -- Open state: draw options list below
    if self.open then
        local mouse = ctrl.touch()
        self.hovered = nil

        prev = spr.sheet(2)

        for i = 1, #self.options do
            local oy = self.y + self.height + (i - 1) * self.item_height

            if inside_rect(mouse.x, mouse.y, self.x, oy, self.width, self.item_height) then
                self.hovered = i
                draw_headless_9patch(self.x, oy, self.width, self.item_height, 2 * 24, sy)
            else
                draw_headless_9patch(self.x, oy, self.width, self.item_height, 1 * 24, sy)
            end
        end

        spr.sheet(prev)

        text.font("monogram")
        for i = 1, #self.options do
            local oy = self.y + self.height + (i - 1) * self.item_height
            text.print(self.options[i], self.x + 4, oy + 2, 1)
        end
        text.font()
    end
end

return Dropdown
