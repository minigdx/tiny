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
    variant = 0,
    status = 0, -- 0 : idle ; 1 : over ; 2 : active
    listeners = {},
    on_update = utils.on_update,
    fire_on_update = utils.fire_on_update,
    shake_timer = 0,
    shake_offset = 0,
}

TextButton.shake = function(self)
    self.shake_timer = 0.4
end

TextButton._update = function(self)
    if self.shake_timer > 0 then
        self.shake_timer = self.shake_timer - tiny.dt
        self.shake_offset = math.sin(self.shake_timer * 30) * 2
        if self.shake_timer <= 0 then
            self.shake_timer = 0
            self.shake_offset = 0
        end
    end

    if self.status == 2 or self.is_active then
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
    local real_x = self.x
    self.x = self.x + (self.shake_offset or 0)

    local prev = spr.sheet(2)

    local draw_variant
    if self.is_active or self.status > 0 then
        draw_variant = self.variant
    else
        draw_variant = 3  -- HardBlue as idle color
    end

    local sx = draw_variant * 24
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

    self.x = real_x
end

return TextButton
