local Tracker = {
    x = 0,
    y = 0,
    width = 264,
    height = 144,
    data = nil,
    cursor_line = 1,
    cursor_col = 1,
    cursor_spot = 1,
    scroll_offset = 0,
    total_lines = 64,
    num_cols = 4,
    line_h = 10,
    line_gap = 1,
    gutter_w = 8,
    col_w = 64,
    col_positions = nil,
    on_change = function(self) end,
}

local note_names = { "A", "B", "C", "D", "E", "F", "G" }
local accident_names = { "-", "#", "b" }
local note_colors = { 11, 4, 6, 9, 3, 7, 8 } -- A=11, B=4, C=6, D=9, E=3, F=7, G=8

local spot_offsets = { 3, 10, 20, 32 }
local spot_widths = { 7, 7, 7, 13 }

Tracker._init = function(self)
     text.font("monogram")
     self.gutter_w = text.width(" 99")
    self.data = {}
    for i = 1, self.total_lines do
        self.data[i] = {}
        for j = 1, self.num_cols do
            self.data[i][j] = { note = nil, accident = 1, volume = 5, duration = 4 }
        end
    end
    self.col_positions = {}
    local cx = self.x + self.gutter_w
    for c = 1, self.num_cols do
        self.col_positions[c] = cx
        cx = cx + self.col_w
    end
end

Tracker._visible_lines = function(self)
    return math.floor(self.height / (self.line_h + self.line_gap))
end

Tracker._ensure_visible = function(self)
    local vis = self:_visible_lines()
    if self.cursor_line < self.scroll_offset + 1 then
        self.scroll_offset = self.cursor_line - 1
    elseif self.cursor_line > self.scroll_offset + vis then
        self.scroll_offset = self.cursor_line - vis
    end
end

Tracker._update = function(self)
    -- Navigation
    if ctrl.pressed(keys.up) then
        self.cursor_line = math.max(1, self.cursor_line - 1)
        self:_ensure_visible()
    elseif ctrl.pressed(keys.down) then
        self.cursor_line = math.min(self.total_lines, self.cursor_line + 1)
        self:_ensure_visible()
    elseif ctrl.pressed(keys.left) then
        self.cursor_spot = self.cursor_spot - 1
        if self.cursor_spot < 1 then
            self.cursor_col = self.cursor_col - 1
            if self.cursor_col < 1 then
                self.cursor_col = self.num_cols
            end
            self.cursor_spot = 4
        end
    elseif ctrl.pressed(keys.right) then
        self.cursor_spot = self.cursor_spot + 1
        if self.cursor_spot > 4 then
            self.cursor_col = self.cursor_col + 1
            if self.cursor_col > self.num_cols then
                self.cursor_col = 1
            end
            self.cursor_spot = 1
        end
    end

    -- Value modification
    local delta = 0
    if ctrl.pressed(keys.ctrl) then delta = 1 end
    if ctrl.pressed(keys.shift) then delta = -1 end

    if delta ~= 0 then
        local cell = self.data[self.cursor_line][self.cursor_col]
        if self.cursor_spot == 1 then
            if cell.note == nil then
                cell.note = 1
            else
                cell.note = cell.note + delta
                if cell.note < 1 then
                    cell.note = 7
                elseif cell.note > 7 then
                    cell.note = 1
                end
            end
        elseif self.cursor_spot == 2 and cell.note then
            cell.accident = cell.accident + delta
            if cell.accident < 1 then
                cell.accident = 3
            elseif cell.accident > 3 then
                cell.accident = 1
            end
        elseif self.cursor_spot == 3 and cell.note then
            cell.volume = cell.volume + delta
            if cell.volume < 0 then
                cell.volume = 9
            elseif cell.volume > 9 then
                cell.volume = 0
            end
        elseif self.cursor_spot == 4 and cell.note then
            cell.duration = cell.duration + delta
            if cell.duration < 1 then
                cell.duration = 64
            elseif cell.duration > 64 then
                cell.duration = 1
            end
        end
        self:on_change()
    end

    -- Delete clears current cell
    if ctrl.pressed(keys.delete) then
        local cell = self.data[self.cursor_line][self.cursor_col]
        cell.note = nil
        cell.accident = 1
        cell.volume = 5
        cell.duration = 4
        self:on_change()
    end
end

Tracker._draw = function(self)
    text.font("monogram")
    local vis = self:_visible_lines()

    for i = 0, vis - 1 do
        local line = self.scroll_offset + i + 1
        if line > self.total_lines then break end

        local ly = self.y + i * (self.line_h + self.line_gap)
        local is_current = (line == self.cursor_line)
        local bg = is_current and 1 or 2
        local gutter_fg = is_current and 2 or 1
        local dot_color = gutter_fg

        -- Gutter background and text
        shape.rectf(self.x, ly, self.gutter_w, self.line_h, bg)
        text.print(string.format("%02d", line), self.x + 1, ly - 1, gutter_fg)

        -- Column backgrounds
        for c = 1, self.num_cols do
            local cx = self.col_positions[c]
            shape.rectf(cx + 1, ly, self.col_w - 2, self.line_h, bg)
        end

        -- Selection cursor highlight
        if is_current then
            local sel_cx = self.col_positions[self.cursor_col]
            shape.rectf(sel_cx + spot_offsets[self.cursor_spot], ly, spot_widths[self.cursor_spot], self.line_h, 5)
        end

        -- Column text content
        for c = 1, self.num_cols do
            local cx = self.col_positions[c]
            local cell = self.data[line][c]

            if cell.note then
                local nc = note_colors[cell.note]
                text.print(note_names[cell.note], cx + spot_offsets[1], ly - 1, nc)
                text.print(accident_names[cell.accident], cx + spot_offsets[2], ly - 1, nc)
                text.print(tostring(cell.volume), cx + spot_offsets[3], ly - 1, 7)
                text.print(string.format("%2d", cell.duration), cx + spot_offsets[4], ly - 1, 6)
            else
                text.print(".", cx + spot_offsets[1], ly - 1, dot_color)
                text.print(".", cx + spot_offsets[2], ly - 1, dot_color)
                text.print(".", cx + spot_offsets[3], ly - 1, dot_color)
                text.print("..", cx + spot_offsets[4], ly - 1, dot_color)
            end
        end
    end

    text.font()
end

return Tracker
