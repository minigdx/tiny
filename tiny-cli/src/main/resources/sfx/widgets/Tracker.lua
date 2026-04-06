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
    total_lines = 32,
    num_cols = 4,
    line_h = 10,
    line_gap = 1,
    gutter_w = 8,
    col_w = 64,
    col_positions = nil,
    on_change = function(self) end,
    -- Playback position (nil when not playing, 0-based beat)
    play_beat = nil,
    -- Key repeat state
    hold_key = nil,
    hold_timer = 0,
    hold_initial_delay = 20,
    hold_repeat_rate = 4,
    -- Default octave for new notes
    last_octave = 4,
    -- Shake feedback
    shake_timer = 0,
    shake_line = 0,
    shake_col = 0,
    shake_spot = 0,
}

local note_names = { "A", "B", "C", "D", "E", "F", "G" }
local accident_names = { "-", "#", "b" }
local note_colors = { 11, 4, 6, 9, 3, 7, 8 } -- A=11, B=4, C=6, D=9, E=3, F=7, G=8

-- Spots: note, accidental, octave, volume, duration
local spot_offsets = { 3, 10, 16, 24, 32 }
local spot_widths = { 7, 6, 6, 7, 13 }

-- Piano keyboard mapping (QWERTY layout)
local piano_map = {
    -- White keys
    { key = "a", note = 3, accident = 1 },  -- A key -> C
    { key = "s", note = 4, accident = 1 },  -- S key -> D
    { key = "d", note = 5, accident = 1 },  -- D key -> E
    { key = "f", note = 6, accident = 1 },  -- F key -> F
    { key = "g", note = 7, accident = 1 },  -- G key -> G
    { key = "h", note = 1, accident = 1 },  -- H key -> A
    { key = "j", note = 2, accident = 1 },  -- J key -> B
    -- Black keys
    { key = "w", note = 3, accident = 2 },  -- W key -> C#
    { key = "e", note = 4, accident = 2 },  -- E key -> D#
    { key = "t", note = 6, accident = 2 },  -- T key -> F#
    { key = "y", note = 7, accident = 2 },  -- Y key -> G#
    { key = "u", note = 1, accident = 2 },  -- U key -> A#
}

-- Note name conversion helpers
local note_letter_to_index = { A = 1, B = 2, C = 3, D = 4, E = 5, F = 6, G = 7 }
local accident_to_suffix = { "", "s", "b" }

local function build_note_name(cell)
    if not cell.note then return nil end
    local letter = note_names[cell.note]
    local suffix = accident_to_suffix[cell.accident]
    local octave = cell.octave

    -- Handle enharmonic equivalents for names not in the Note enum
    if letter == "E" and suffix == "s" then return "F" .. octave end
    if letter == "B" and suffix == "s" then
        if octave < 8 then return "C" .. (octave + 1) end
        return nil
    end
    if letter == "C" and suffix == "b" then
        if octave > 0 then return "B" .. (octave - 1) end
        return nil
    end
    if letter == "F" and suffix == "b" then return "E" .. octave end

    return letter .. suffix .. octave
end

local function parse_note_name(name)
    if not name or name == "" then return nil, nil, nil end
    local octave = tonumber(name:sub(-1))
    if not octave then return nil, nil, nil end
    local base = name:sub(1, -2)
    local letter = base:sub(1, 1):upper()
    local ni = note_letter_to_index[letter]
    if not ni then return nil, nil, nil end
    local accident = 1
    if #base > 1 then
        local acc = base:sub(2, 2)
        if acc == "s" then accident = 2
        elseif acc == "b" then accident = 3
        end
    end
    return ni, accident, octave
end

local function shake_offset(self, line, c, spot)
    if self.shake_timer > 0 and line == self.shake_line and c == self.shake_col and spot == self.shake_spot then
        return (self.shake_timer % 2 == 0) and 1 or -1
    end
    return 0
end

Tracker._init = function(self)
    text.font("monogram")
    self.gutter_w = text.width(" 99")
    self.data = {}
    for i = 1, self.total_lines do
        self.data[i] = {}
        for j = 1, self.num_cols do
            self.data[i][j] = { note = nil, accident = 1, octave = 4, volume = 5, duration = 1 }
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

Tracker._max_duration = function(self, line, col)
    for l = line + 1, self.total_lines do
        if self.data[l][col].note then
            return l - line
        end
    end
    return self.total_lines - line + 1
end

Tracker._clamp_prev_duration = function(self, line, col)
    for l = line - 1, 1, -1 do
        if self.data[l][col].note then
            local max_dur = line - l
            if self.data[l][col].duration > max_dur then
                self.data[l][col].duration = max_dur
            end
            return
        end
    end
end

Tracker._shake = function(self, line, col, spot)
    self.shake_timer = 12
    self.shake_line = line
    self.shake_col = col
    self.shake_spot = spot
end

Tracker._move_up = function(self)
    self.cursor_line = math.max(1, self.cursor_line - 1)
    self:_ensure_visible()
end

Tracker._move_down = function(self)
    self.cursor_line = math.min(self.total_lines, self.cursor_line + 1)
    self:_ensure_visible()
end

Tracker._move_left = function(self)
    self.cursor_spot = self.cursor_spot - 1
    if self.cursor_spot < 1 then
        self.cursor_col = self.cursor_col - 1
        if self.cursor_col < 1 then
            self.cursor_col = self.num_cols
        end
        self.cursor_spot = 5
    end
end

Tracker._move_right = function(self)
    self.cursor_spot = self.cursor_spot + 1
    if self.cursor_spot > 5 then
        self.cursor_col = self.cursor_col + 1
        if self.cursor_col > self.num_cols then
            self.cursor_col = 1
        end
        self.cursor_spot = 1
    end
end

Tracker._handle_nav = function(self, key, move_fn)
    if ctrl.pressed(key) then
        move_fn(self)
        self.hold_key = key
        self.hold_timer = 0
        return true
    end
    return false
end

Tracker._update = function(self)
    -- Shake timer
    if self.shake_timer > 0 then
        self.shake_timer = self.shake_timer - 1
    end

    -- Navigation with key repeat (up/down/left/right)
    local moved = self:_handle_nav(keys.up, self._move_up)
        or self:_handle_nav(keys.down, self._move_down)
        or self:_handle_nav(keys.left, self._move_left)
        or self:_handle_nav(keys.right, self._move_right)

    if not moved and self.hold_key then
        if ctrl.pressing(self.hold_key) then
            self.hold_timer = self.hold_timer + 1
            if self.hold_timer > self.hold_initial_delay
               and (self.hold_timer - self.hold_initial_delay) % self.hold_repeat_rate == 0 then
                if self.hold_key == keys.up then self:_move_up()
                elseif self.hold_key == keys.down then self:_move_down()
                elseif self.hold_key == keys.left then self:_move_left()
                elseif self.hold_key == keys.right then self:_move_right()
                end
            end
        else
            self.hold_key = nil
        end
    end

    -- Piano keyboard input on current column
    for _, mapping in ipairs(piano_map) do
        if ctrl.pressed(keys[mapping.key]) then
            local cell = self.data[self.cursor_line][self.cursor_col]
            local was_empty = cell.note == nil
            cell.note = mapping.note
            cell.accident = mapping.accident
            cell.octave = self.last_octave
            cell.duration = 1
            if was_empty then
                self:_clamp_prev_duration(self.cursor_line, self.cursor_col)
            end
            self:on_change()
            self:_move_down()
            break
        end
    end

    -- Value modification with x/c
    local delta = 0
    if ctrl.pressed(keys.c) then delta = 1 end
    if ctrl.pressed(keys.x) then delta = -1 end

    if delta ~= 0 then
        local cell = self.data[self.cursor_line][self.cursor_col]
        if self.cursor_spot == 1 then
            if cell.note == nil then
                cell.note = 1
                cell.octave = self.last_octave
                cell.duration = 1
                self:_clamp_prev_duration(self.cursor_line, self.cursor_col)
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
            local new_oct = cell.octave + delta
            if new_oct < 1 or new_oct > 8 then
                self:_shake(self.cursor_line, self.cursor_col, 3)
            else
                cell.octave = new_oct
                self.last_octave = cell.octave
            end
        elseif self.cursor_spot == 4 and cell.note then
            cell.volume = cell.volume + delta
            if cell.volume < 0 then
                cell.volume = 9
            elseif cell.volume > 9 then
                cell.volume = 0
            end
        elseif self.cursor_spot == 5 and cell.note then
            local new_dur = cell.duration + delta
            local max_dur = self:_max_duration(self.cursor_line, self.cursor_col)
            if new_dur < 1 or new_dur > max_dur then
                self:_shake(self.cursor_line, self.cursor_col, 5)
            else
                cell.duration = new_dur
            end
        end
        self:on_change()
    end

    -- Delete clears current cell
    if ctrl.pressed(keys.delete) then
        local cell = self.data[self.cursor_line][self.cursor_col]
        cell.note = nil
        cell.accident = 1
        cell.octave = 4
        cell.volume = 5
        cell.duration = 1
        self:on_change()
    end
end

-- Load tracker grid from a sequence's 4 tracks
Tracker.load_from_sequence = function(self, seq)
    -- Clear all cells
    for i = 1, self.total_lines do
        for j = 1, self.num_cols do
            self.data[i][j] = { note = nil, accident = 1, octave = 4, volume = 5, duration = 1 }
        end
    end

    for col = 1, self.num_cols do
        local track = seq.track(col - 1)
        if not track then goto next_col end

        local beats = track.beats
        if not beats then goto next_col end

        -- First pass: place notes into the grid
        for _, beat in ipairs(beats) do
            local pos = math.floor(beat.beat)
            if beat.note and pos >= 0 and pos < self.total_lines then
                local ni, acc, oct = parse_note_name(beat.note)
                if ni then
                    local line = pos + 1
                    local vol = math.floor(beat.volume * 9 + 0.5)
                    if vol < 0 then vol = 0 end
                    if vol > 9 then vol = 9 end
                    self.data[line][col] = {
                        note = ni,
                        accident = acc,
                        octave = oct,
                        volume = vol,
                        duration = 1,
                    }
                end
            end
        end

        -- Second pass: compute durations (distance to next note)
        for i = self.total_lines, 1, -1 do
            if self.data[i][col].note then
                local dur = 1
                for j = i + 1, self.total_lines do
                    if self.data[j][col].note then break end
                    dur = dur + 1
                end
                self.data[i][col].duration = dur
            end
        end

        ::next_col::
    end
end

-- Sync tracker grid data back into a sequence's tracks
Tracker.sync_to_sequence = function(self, seq)
    for col = 1, self.num_cols do
        local track = seq.track(col - 1)
        if not track then goto next_col end

        track.clear()

        -- Collect notes in this column
        local notes = {}
        for line = 1, self.total_lines do
            local cell = self.data[line][col]
            if cell.note then
                table.insert(notes, { line = line, cell = cell })
            end
        end

        -- Set notes on the track
        for idx, entry in ipairs(notes) do
            local beat_pos = entry.line - 1
            local name = build_note_name(entry.cell)
            if name then
                local vol = entry.cell.volume / 9.0
                track.set_note({
                    beat = beat_pos,
                    note = name,
                    volume = vol,
                    duration = entry.cell.duration,
                })

                -- Add off-note if duration ends before next note
                local end_pos = beat_pos + entry.cell.duration
                local next_entry = notes[idx + 1]
                local next_pos = next_entry and (next_entry.line - 1) or self.total_lines
                if end_pos < next_pos and end_pos < self.total_lines then
                    track.set_note({
                        beat = end_pos,
                        note = name,
                        volume = 0,
                        duration = 1,
                        off = true,
                    })
                end
            end
        end

        ::next_col::
    end

    -- Clear config so playback uses the pre-computed path with actual track beats
    seq.config = nil
    seq.invalidate()
end

-- Move cursor_line to the current beat during playback
Tracker._update_play_position = function(self)
    if self.play_beat == nil then return end
    local play_line = math.floor(self.play_beat) + 1
    if play_line >= 1 and play_line <= self.total_lines then
        self.cursor_line = play_line
        self:_ensure_visible()
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

        -- Selection cursor highlight (only when not playing)
        if is_current and self.play_beat == nil then
            local sel_cx = self.col_positions[self.cursor_col]
            shape.rectf(sel_cx + spot_offsets[self.cursor_spot], ly, spot_widths[self.cursor_spot], self.line_h, 5)
        end

        -- Column text content
        for c = 1, self.num_cols do
            local cx = self.col_positions[c]
            local cell = self.data[line][c]

            if cell.note then
                local nc = note_colors[cell.note]
                text.print(note_names[cell.note], cx + spot_offsets[1] + shake_offset(self, line, c, 1), ly - 1, nc)
                text.print(accident_names[cell.accident], cx + spot_offsets[2] + shake_offset(self, line, c, 2), ly - 1, nc)
                text.print(tostring(cell.octave), cx + spot_offsets[3] + shake_offset(self, line, c, 3), ly - 1, 10)
                text.print(tostring(cell.volume), cx + spot_offsets[4] + shake_offset(self, line, c, 4), ly - 1, 7)
                text.print(string.format("%2d", cell.duration), cx + spot_offsets[5] + shake_offset(self, line, c, 5), ly - 1, 6)
            else
                text.print(".", cx + spot_offsets[1], ly - 1, dot_color)
                text.print(".", cx + spot_offsets[2], ly - 1, dot_color)
                text.print(".", cx + spot_offsets[3], ly - 1, dot_color)
                text.print(".", cx + spot_offsets[4], ly - 1, dot_color)
                text.print("..", cx + spot_offsets[5], ly - 1, dot_color)
            end
        end
    end

    text.font()
end

Tracker.build_note_name = build_note_name

return Tracker
