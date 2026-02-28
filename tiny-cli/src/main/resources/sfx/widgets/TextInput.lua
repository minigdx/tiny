local utils = require("widgets.utils")
local inside_widget = utils.inside_widget

local PADDING = 5
local CURSOR_SX = 72
local CURSOR_SY = 40
local CURSOR_W = 4
local CURSOR_H = 12

local TextInput = {
    x = 0,
    y = 0,
    width = 32,
    height = 32,
    value = "",
    focused = false,
    cursor = 0,
    enabled = true,
    listeners = {},
    on_update = utils.on_update,
    fire_on_update = utils.fire_on_update,
    cursor_blink = 0,
}

TextInput._init = function(self)
    self.cursor = #self.value
end

TextInput._update = function(self)
    -- Handle focus on click
    local touched = ctrl.touched(0)
    if touched then
        if inside_widget(self, touched.x, touched.y) then
            self.focused = true
        else
            self.focused = false
        end
    end

    if not self.focused then
        return
    end

    -- Blink cursor
    self.cursor_blink = self.cursor_blink + 1
    if self.cursor_blink > 60 then
        self.cursor_blink = 0
    end

    local changed = false

    local pressed_keys = ctrl.pressed()
    if pressed_keys then
        for k in all(pressed_keys) do
            if k >= keys.a and k <= keys.z then
                -- Letter key: convert key ordinal to character
                local ch = string.char(string.byte("a") + (k - keys.a))
                local before = string.sub(self.value, 1, self.cursor)
                local after = string.sub(self.value, self.cursor + 1)
                self.value = before .. ch .. after
                self.cursor = self.cursor + 1
                changed = true
            elseif k >= keys["0"] and k <= keys["9"] then
                -- Number key: convert key ordinal to digit character
                local ch = string.char(string.byte("0") + (k - keys["0"]))
                local before = string.sub(self.value, 1, self.cursor)
                local after = string.sub(self.value, self.cursor + 1)
                self.value = before .. ch .. after
                self.cursor = self.cursor + 1
                changed = true
            elseif k == keys.space then
                local before = string.sub(self.value, 1, self.cursor)
                local after = string.sub(self.value, self.cursor + 1)
                self.value = before .. " " .. after
                self.cursor = self.cursor + 1
                changed = true
            elseif k == keys.delete then
                if self.cursor > 0 then
                    local before = string.sub(self.value, 1, self.cursor - 1)
                    local after = string.sub(self.value, self.cursor + 1)
                    self.value = before .. after
                    self.cursor = self.cursor - 1
                    changed = true
                end
            elseif k == keys.left then
                self.cursor = math.max(0, self.cursor - 1)
            elseif k == keys.right then
                self.cursor = math.min(#self.value, self.cursor + 1)
            end
        end
    end

    if changed then
        if self.on_change then
            self:on_change()
        end
        self:fire_on_update(self.value)
    end
end

TextInput._draw = function(self)
    local prev = spr.sheet(2)

    -- 9-patch background (White variant, flipped on both axes)
    local sx = 24  -- White variant = 1 * 24
    local sy = 0
    local corner = 5
    local edge = 14

    local inner_w = self.width - corner * 2
    local inner_h = self.height - corner * 2

    -- Corners (flipped: source positions swap)
    spr.sdraw(self.x, self.y, sx + 19, sy + 19, corner, corner, true, true)
    spr.sdraw(self.x + self.width - corner, self.y, sx, sy + 19, corner, corner, true, true)
    spr.sdraw(self.x, self.y + self.height - corner, sx + 19, sy, corner, corner, true, true)
    spr.sdraw(self.x + self.width - corner, self.y + self.height - corner, sx, sy, corner, corner, true, true)

    -- Top edge (from bottom edge source, flipped)
    local cx = 0
    while cx < inner_w do
        local tw = math.min(edge, inner_w - cx)
        spr.sdraw(self.x + corner + cx, self.y, sx + 5, sy + 19, tw, corner, true, true)
        cx = cx + tw
    end

    -- Bottom edge (from top edge source, flipped)
    cx = 0
    while cx < inner_w do
        local tw = math.min(edge, inner_w - cx)
        spr.sdraw(self.x + corner + cx, self.y + self.height - corner, sx + 5, sy, tw, corner, true, true)
        cx = cx + tw
    end

    -- Left edge (from right edge source, flipped)
    local cy = 0
    while cy < inner_h do
        local th = math.min(edge, inner_h - cy)
        spr.sdraw(self.x, self.y + corner + cy, sx + 19, sy + 5, corner, th, true, true)
        cy = cy + th
    end

    -- Right edge (from left edge source, flipped)
    cy = 0
    while cy < inner_h do
        local th = math.min(edge, inner_h - cy)
        spr.sdraw(self.x + self.width - corner, self.y + corner + cy, sx, sy + 5, corner, th, true, true)
        cy = cy + th
    end

    -- Center fill (flipped)
    cy = 0
    while cy < inner_h do
        local th = math.min(edge, inner_h - cy)
        cx = 0
        while cx < inner_w do
            local tw = math.min(edge, inner_w - cx)
            spr.sdraw(self.x + corner + cx, self.y + corner + cy, sx + 5, sy + 5, tw, th, true, true)
            cx = cx + tw
        end
        cy = cy + th
    end

    -- Draw text with monogram font
    local text_x = self.x + PADDING
    local text_y = self.y + PADDING
    text.font("monogram")
    text.print(self.value, text_x, text_y, 1)

    -- Draw sprite cursor (blinking)
    if self.focused and self.cursor_blink < 40 then
        local before_cursor = string.sub(self.value, 1, self.cursor)
        local cursor_x = text_x + text.width(before_cursor)
        spr.sdraw(cursor_x - 2, self.y + PADDING, CURSOR_SX, CURSOR_SY, CURSOR_W, CURSOR_H)
    end

    text.font()
    spr.sheet(prev)
end

TextInput.set_value = function(self, value)
    if value == self.value then
        return
    end
    self.value = value
    self.cursor = #value
    if self.on_change then
        self:on_change()
    end
    self:fire_on_update(value)
end

return TextInput
