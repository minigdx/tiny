local Panel = {
    x = 0,
    y = 0,
    width = 48,
    height = 48,
    variant = 0,
}

Panel._update = function(self)
    -- purely visual, no interaction
end

Panel._draw = function(self)
    local prev = spr.sheet(2)

    local sx = self.variant * 24
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
end

return Panel
