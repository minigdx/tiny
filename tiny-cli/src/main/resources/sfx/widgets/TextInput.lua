local on_update = function(self, listener)
    table.insert(self.listeners, listener)
end

local fire_on_update = function(self, value)
    for l in all(self.listeners) do
        l(self, value)
    end
end

local function inside_widget(w, x, y, offset)
    local off = 0
    if (offset) then
        off = offset
    end

    return w.x - off <= x and
            x <= w.x + w.width + off and
            w.y - off <= y and
            y <= w.y + w.height + off
end

local CHAR_WIDTH = 4
local PADDING = 2

local TextInput = {
    x = 0,
    y = 0,
    width = 32,
    height = 8,
    value = "",
    focused = false,
    cursor = 0,
    enabled = true,
    listeners = {},
    on_update = on_update,
    fire_on_update = fire_on_update,
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
    -- Draw background rect
    local bg_color = 0
    if self.focused then
        bg_color = 1
    end
    shape.rectf(self.x, self.y, self.width, self.height, bg_color)

    -- Draw border
    local border_color = 6
    if self.focused then
        border_color = 7
    end
    shape.rect(self.x, self.y, self.width, self.height, border_color)

    -- Draw text
    local text_x = self.x + PADDING
    local text_y = self.y + PADDING
    print(self.value, text_x, text_y)

    -- Draw cursor (blinking line)
    if self.focused and self.cursor_blink < 40 then
        local cursor_x = text_x + self.cursor * CHAR_WIDTH
        shape.line(cursor_x, self.y + 1, cursor_x, self.y + self.height - 2, 7)
    end
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
