local utils = require("widgets.utils")
local inside_widget = utils.inside_widget

local Fader = {
    x = 0,
    y = 0,
    width = 8,
    height = 40,
    enabled = true,
    value = 0,
    label = "",
    type = "fader",
    data = nil,
    index = 0,
    focused = false,
    listeners = {},
    on_update = utils.on_update,
    fire_on_update = utils.fire_on_update,
    set_value = utils.set_value,
}

Fader._update = function(self)
    local touching = ctrl.touching(0)

    if touching ~= nil then
        local pos = ctrl.touch()
        if inside_widget(self, pos.x, pos.y) then
            if not self.focused then
                self.focused = true
                if self.on_press then
                    self:on_press()
                end
            end
            local track_top = self.y
            local track_bottom = self.y + self.height
            local track_height = track_bottom - track_top
            local clamped_y = math.max(track_top, math.min(track_bottom, pos.y))
            local percent = 1.0 - ((clamped_y - track_top) / track_height)
            local value = percent * percent
            self:set_value(value)
        else
            -- Mouse held but moved outside: unfocus so a new fader can take focus
            if self.focused then
                self.focused = false
                if self.on_release then
                    self:on_release()
                end
            end
        end
    else
        if self.focused then
            self.focused = false
            if self.on_release then
                self:on_release()
            end
        end
    end

    local pos = ctrl.touch()
    if inside_widget(self, pos.x, pos.y) then
        if self.on_hover ~= nil then
            self:on_hover()
        end
    end
end

Fader._draw = function(self)
    local prev = spr.sheet(2)

    -- Head sprite (top of track)
    spr.sdraw(self.x, self.y, 0, 24, 8, 8)

    -- Body sprites (fill the middle)
    local body_start = self.y + 8
    local body_end = self.y + self.height - 8
    local y = body_start
    while y < body_end do
        spr.sdraw(self.x, y, 0, 32, 8, 8)
        y = y + 8
    end

    -- Bottom sprite
    spr.sdraw(self.x, self.y + self.height - 8, 0, 40, 8, 8)

    -- Compute cursor Y from value (reverse the logarithmic mapping)
    local percent = self.value
    local track_top = self.y + 1
    local track_bottom = self.y + self.height - 3
    local track_height = track_bottom - track_top
    local cursor_y = math.ceil(track_top + (1.0 - percent) * track_height)

    -- Yellow fill (palette index 8) between cursor bottom and fader bottom
    local fill_top = cursor_y + 2
    local fill_bottom = self.y + self.height - 1
    if fill_top < fill_bottom then
        shape.rectf(self.x + 1, fill_top, 6, fill_bottom - fill_top, 9)
    end

    -- Cursor sprite (movable handle, 3px tall within 8x8 cell)
    spr.sdraw(self.x, cursor_y - 1, 8, 24, 8, 8)

    spr.sheet(prev)
end

return Fader
