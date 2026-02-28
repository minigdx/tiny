local utils = require("widgets.utils")
local inside_widget = utils.inside_widget

local TextButton = {
    x = 0,
    y = 0,
    width = 48,
    height = 24,
    label = "",
    is_active = false,
    enabled = true,
    status = 0, -- 0 : idle ; 1 : over ; 2 : active
    listeners = {},
    on_update = utils.on_update,
    fire_on_update = utils.fire_on_update,
}

TextButton._update = function(self)
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

TextButton._draw = function(self)
    local prev = spr.sheet(2)

    -- Use variant 1 (sx=24) for idle, variant 0 (sx=0) for hover/active/is_active
    local variant = 1
    if self.status > 0 or self.is_active then
        variant = 0
    end

    local sx = variant * 24
    local sy = 0
    local corner = 5
    local edge = 14

    local inner_w = self.width - corner * 2
    local inner_h = self.height - corner * 2

    -- Corners
    spr.sdraw(self.x, self.y, sx, sy, corner, corner)
    spr.sdraw(self.x + self.width - corner, self.y, sx + 19, sy, corner, corner)
    spr.sdraw(self.x, self.y + self.height - corner, sx, sy + 19, corner, corner)
    spr.sdraw(self.x + self.width - corner, self.y + self.height - corner, sx + 19, sy + 19, corner, corner)

    -- Top edge
    local cx = 0
    while cx < inner_w do
        local tw = math.min(edge, inner_w - cx)
        spr.sdraw(self.x + corner + cx, self.y, sx + 5, sy, tw, corner)
        cx = cx + tw
    end

    -- Bottom edge
    cx = 0
    while cx < inner_w do
        local tw = math.min(edge, inner_w - cx)
        spr.sdraw(self.x + corner + cx, self.y + self.height - corner, sx + 5, sy + 19, tw, corner)
        cx = cx + tw
    end

    -- Left edge
    local cy = 0
    while cy < inner_h do
        local th = math.min(edge, inner_h - cy)
        spr.sdraw(self.x, self.y + corner + cy, sx, sy + 5, corner, th)
        cy = cy + th
    end

    -- Right edge
    cy = 0
    while cy < inner_h do
        local th = math.min(edge, inner_h - cy)
        spr.sdraw(self.x + self.width - corner, self.y + corner + cy, sx + 19, sy + 5, corner, th)
        cy = cy + th
    end

    -- Center fill
    cy = 0
    while cy < inner_h do
        local th = math.min(edge, inner_h - cy)
        cx = 0
        while cx < inner_w do
            local tw = math.min(edge, inner_w - cx)
            spr.sdraw(self.x + corner + cx, self.y + corner + cy, sx + 5, sy + 5, tw, th)
            cx = cx + tw
        end
        cy = cy + th
    end

    spr.sheet(prev)

    -- Render label centered
    if self.label and #self.label > 0 then
        text.font("monogram")
        local tx = self.x + 4
        local ty = self.y + 2
        text.print(self.label, tx, ty, 1)
        text.font()
    end
end

return TextButton
