local utils = require("widgets.utils")
local inside_widget = utils.inside_widget
local inside_rect = utils.inside_rect

local MAX_SLOTS = 8

-- Layout offsets (relative to widget.x/.y)
local POPUP_DY = 0
local MAIN_DY = 14
local STR_DY = 28
local PLAY_DX = 8
local FIRST_SLOT_DX = 21
local SLOT_STRIDE = 13

-- Pattern sprite footprint
local PATTERN_W = 11
local PATTERN_H = 13

-- Sprite coordinates on sheet 2
local EMPTY_AVAIL_SX, EMPTY_AVAIL_SY = 0, 136
local EMPTY_NOTAVAIL_SX, EMPTY_NOTAVAIL_SY = 16, 136
local HOVER_BORDER_SX, HOVER_BORDER_SY = 0, 152
local HOVER_BORDER_W = 12
local HOVER_BORDER_H = 14
local PLAYING_BORDER_SX, PLAYING_BORDER_SY = 0, 168

-- Play button sprites
local PLAY_BG_SX, PLAY_BG_SY = 16, 136
local PLAY_ICON_SX, PLAY_ICON_SY = 40, 88
local PAUSE_ICON_SX, PAUSE_ICON_SY = 56, 88
local PLAY_ICON_W = 8
local PLAY_ICON_H = 10

-- Color sprites cycled per pattern index (A..H)
local COLOR_SPRITES = {
    { 16, 152 }, -- yellow
    { 32, 152 }, -- pink
    { 48, 152 }, -- light white
    { 64, 152 }, -- green
}

-- Arrow sprites (popup selector)
local ARROW_SIZE = 8
local ARROW_RIGHT_SX, ARROW_RIGHT_SY = 32, 136
local ARROW_LEFT_SX, ARROW_LEFT_SY = 32, 144
local ARROW_RIGHT_HOVER_SX, ARROW_RIGHT_HOVER_SY = 40, 136
local ARROW_LEFT_HOVER_SX, ARROW_LEFT_HOVER_SY = 40, 144

-- Palette indices
local COLOR_TEXT_DARK = 1
local COLOR_STR_BG = 7      -- orange
local COLOR_STR_PREVIEW = 2 -- muted blue-grey
local COLOR_WHITE = 11

-- STR label dimensions
local STR_W = 13
local STR_H = 6
local STR_BORDER_W = 14
local STR_BORDER_H = 8

local function index_to_letter(i)
    return string.char(string.byte("A") + i)
end

local function filled_count(self)
    local count = 0
    for i = 1, MAX_SLOTS do
        if self.slots[i] ~= nil then
            count = count + 1
        else
            break
        end
    end
    return count
end

local function pattern_sprite_for(idx)
    if idx == nil then
        return EMPTY_AVAIL_SX, EMPTY_AVAIL_SY
    end
    local c = COLOR_SPRITES[(idx % 4) + 1]
    return c[1], c[2]
end

local function slot_main_pos(self, i)
    return self.x + FIRST_SLOT_DX + (i - 1) * SLOT_STRIDE, self.y + MAIN_DY
end

local function slot_popup_pos(self, i)
    return self.x + FIRST_SLOT_DX + (i - 1) * SLOT_STRIDE, self.y + POPUP_DY
end

local function slot_str_pos(self, i)
    return self.x + FIRST_SLOT_DX + (i - 1) * SLOT_STRIDE, self.y + STR_DY
end


local function draw_str_label(x, y, bg, fg)
    shape.rectf(x, y, STR_W, STR_H, bg)
    local tx = x + 1
    local ty = y + 1
    print("str", tx, ty, fg)
end

local function draw_str_border(x, y)
    shape.rect(x - 1, y - 1, STR_BORDER_W, STR_BORDER_H, COLOR_WHITE)
end

local function cycle_value(self, i, dir)
    local current = self.slots[i]
    local fc = filled_count(self)
    local can_unset = (current ~= nil) and (i == fc) and (i > 1)

    if dir > 0 then
        if current == nil then
            self.slots[i] = 0
        elseif current >= self.max_pattern then
            if can_unset then
                self.slots[i] = nil
            else
                self.slots[i] = 0
            end
        else
            self.slots[i] = current + 1
        end
    else
        if current == nil then
            self.slots[i] = self.max_pattern
        elseif current <= 0 then
            if can_unset then
                self.slots[i] = nil
            else
                self.slots[i] = self.max_pattern
            end
        else
            self.slots[i] = current - 1
        end
    end
end

local function clamp_loop(self)
    local fc = filled_count(self)
    if self.loop_index >= fc then
        self.loop_index = -1
    end
end

local Arrangement = {
    x = 0,
    y = 0,
    width = 264,
    height = 32,
    slots = {},
    loop_index = 0,
    max_pattern = 0,
    selected_slot = nil,
    playing = false,
    play_slot = nil,
    on_change = function(self) end,
    on_play_toggle = function(self) end,
    listeners = {},
    on_update = utils.on_update,
    fire_on_update = utils.fire_on_update,
}

local function play_button_pos(self)
    return self.x + PLAY_DX, self.y + MAIN_DY
end

Arrangement._init = function(self)
    self.slots = {}
    for i = 1, MAX_SLOTS do
        self.slots[i] = nil
    end
    self.selected_slot = nil
end

Arrangement.load_from_sequence = function(self, seq)
    local arr = seq.arrangement
    for i = 1, MAX_SLOTS do
        self.slots[i] = nil
    end
    if arr then
        for i = 1, math.min(#arr, MAX_SLOTS) do
            self.slots[i] = arr[i]
        end
    end
    self.loop_index = seq.loopFrom or 0
    self.max_pattern = seq.pattern_count - 1
    self.selected_slot = nil
end

Arrangement.sync_to_sequence = function(self, seq)
    local arr = {}
    for i = 1, MAX_SLOTS do
        if self.slots[i] ~= nil then
            table.insert(arr, self.slots[i])
        else
            break
        end
    end
    if #arr == 0 then
        arr = { 0 }
    end
    seq.arrangement = arr
    seq.loopFrom = self.loop_index
end

Arrangement._update = function(self)
    local touched = ctrl.touched(0)
    if not touched then
        return
    end

    -- Play button
    local pbx, pby = play_button_pos(self)
    if inside_rect(touched.x, touched.y, pbx, pby, PATTERN_W, PATTERN_H) then
        self:on_play_toggle()
        return
    end

    -- Popup arrows take priority
    if self.selected_slot then
        local px, py = slot_popup_pos(self, self.selected_slot)
        local arrow_y = py + math.floor((PATTERN_H - ARROW_SIZE) / 2)
        local left_ax = px - ARROW_SIZE
        local right_ax = px + PATTERN_W

        if inside_rect(touched.x, touched.y, left_ax, arrow_y, ARROW_SIZE, ARROW_SIZE) then
            cycle_value(self, self.selected_slot, -1)
            if self.slots[self.selected_slot] == nil then
                self.selected_slot = nil
            end
            clamp_loop(self)
            self:on_change()
            return
        end

        if inside_rect(touched.x, touched.y, right_ax, arrow_y, ARROW_SIZE, ARROW_SIZE) then
            cycle_value(self, self.selected_slot, 1)
            if self.slots[self.selected_slot] == nil then
                self.selected_slot = nil
            end
            clamp_loop(self)
            self:on_change()
            return
        end

        if inside_rect(touched.x, touched.y, px, py, PATTERN_W, PATTERN_H) then
            return
        end
    end

    -- STR label: toggle loop start on the clicked slot
    for i = 1, MAX_SLOTS do
        if self.slots[i] ~= nil then
            local sx, sy = slot_str_pos(self, i)
            if inside_rect(touched.x, touched.y, sx, sy, STR_W, STR_H) then
                if self.loop_index == i - 1 then
                    self.loop_index = -1
                else
                    self.loop_index = i - 1
                end
                self:on_change()
                return
            end
        end
    end

    -- Main row: fill an empty-available slot or toggle popup on a filled slot
    local fc = filled_count(self)
    for i = 1, MAX_SLOTS do
        local sx, sy = slot_main_pos(self, i)
        if inside_rect(touched.x, touched.y, sx, sy, PATTERN_W, PATTERN_H) then
            local is_filled = self.slots[i] ~= nil
            local is_available = (not is_filled) and i == fc + 1

            if is_filled then
                if self.selected_slot == i then
                    self.selected_slot = nil
                else
                    self.selected_slot = i
                end
            elseif is_available then
                self.slots[i] = 0
                self.selected_slot = i
                self:on_change()
            end
            return
        end
    end

    if self.selected_slot then
        self.selected_slot = nil
    end
end

Arrangement._draw = function(self)
    local pos = ctrl.touch()
    local prev = spr.sheet(2)
    local fc = filled_count(self)

    text.font("monogram")

    -- Play button
    local pbx, pby = play_button_pos(self)
    spr.sdraw(pbx, pby, PLAY_BG_SX, PLAY_BG_SY, PATTERN_W, PATTERN_H)
    local icon_sx = self.playing and PAUSE_ICON_SX or PLAY_ICON_SX
    local icon_sy = self.playing and PAUSE_ICON_SY or PLAY_ICON_SY
    local icon_x = pbx + math.floor((PATTERN_W - PLAY_ICON_W) / 2)
    local icon_y = pby + math.floor((PATTERN_H - PLAY_ICON_H) / 2)
    spr.sdraw(icon_x, icon_y, icon_sx, icon_sy, PLAY_ICON_W, PLAY_ICON_H)
    if inside_rect(pos.x, pos.y, pbx, pby, PATTERN_W, PATTERN_H) then
        spr.sdraw(pbx, pby, HOVER_BORDER_SX, HOVER_BORDER_SY, HOVER_BORDER_W, HOVER_BORDER_H)
    end

    -- Main row of pattern squares
    for i = 1, MAX_SLOTS do
        local sx, sy = slot_main_pos(self, i)
        local is_filled = self.slots[i] ~= nil
        local is_available = (not is_filled) and i == fc + 1

        local sp_x, sp_y
        if is_filled then
            sp_x, sp_y = pattern_sprite_for(self.slots[i])
        elseif is_available then
            sp_x, sp_y = EMPTY_AVAIL_SX, EMPTY_AVAIL_SY
        else
            sp_x, sp_y = EMPTY_NOTAVAIL_SX, EMPTY_NOTAVAIL_SY
        end

        spr.sdraw(sx, sy, sp_x, sp_y, PATTERN_W, PATTERN_H)

        if is_filled then
            local letter = index_to_letter(self.slots[i])
            text.print(letter, sx + 2, sy + 1, COLOR_TEXT_DARK)
        end

        if self.playing and self.play_slot == i and is_filled then
            spr.sdraw(sx, sy, PLAYING_BORDER_SX, PLAYING_BORDER_SY, HOVER_BORDER_W, HOVER_BORDER_H)
        end

        if inside_rect(pos.x, pos.y, sx, sy, PATTERN_W, PATTERN_H) then
            spr.sdraw(sx, sy, HOVER_BORDER_SX, HOVER_BORDER_SY, HOVER_BORDER_W, HOVER_BORDER_H)
        end
    end

    -- Popup above the selected slot
    if self.selected_slot then
        local i = self.selected_slot
        local px, py = slot_popup_pos(self, i)
        local arrow_y = py + math.floor((PATTERN_H - ARROW_SIZE) / 2)
        local left_ax = px - ARROW_SIZE
        local right_ax = px + PATTERN_W + 3

        local idx = self.slots[i]
        local sp_x, sp_y = pattern_sprite_for(idx)
        spr.sdraw(px, py, sp_x, sp_y, PATTERN_W, PATTERN_H)

        if idx ~= nil then
            local letter = index_to_letter(idx)
            text.print(letter, px + 2, py + 1, COLOR_TEXT_DARK)
        end

        local left_hover = inside_rect(pos.x, pos.y, left_ax, arrow_y, ARROW_SIZE, ARROW_SIZE)
        local right_hover = inside_rect(pos.x, pos.y, right_ax, arrow_y, ARROW_SIZE, ARROW_SIZE)

        local lsx, lsy = ARROW_LEFT_SX, ARROW_LEFT_SY
        if left_hover then
            lsx, lsy = ARROW_LEFT_HOVER_SX, ARROW_LEFT_HOVER_SY
        end
        spr.sdraw(left_ax, arrow_y, lsx, lsy, ARROW_SIZE, ARROW_SIZE)

        local rsx, rsy = ARROW_RIGHT_SX, ARROW_RIGHT_SY
        if right_hover then
            rsx, rsy = ARROW_RIGHT_HOVER_SX, ARROW_RIGHT_HOVER_SY
        end
        spr.sdraw(right_ax, arrow_y, rsx, rsy, ARROW_SIZE, ARROW_SIZE)
    end

    spr.sheet(prev)

    -- STR labels: orange on the loop start, preview on hover of other filled slots
    for i = 1, MAX_SLOTS do
        if self.slots[i] ~= nil then
            local sx, sy = slot_str_pos(self, i)
            local px, py = slot_main_pos(self, i)
            local is_loop = (self.loop_index == i - 1)
            local over_pattern = inside_rect(pos.x, pos.y, px, py, PATTERN_W, PATTERN_H)
            local over_str = inside_rect(pos.x, pos.y, sx, sy, STR_W, STR_H)

            if is_loop then
                draw_str_label(sx, sy, COLOR_STR_BG, COLOR_TEXT_DARK)
                if over_str then
                    draw_str_border(sx, sy)
                end
            elseif over_pattern or over_str then
                draw_str_label(sx, sy, COLOR_STR_PREVIEW, COLOR_TEXT_DARK)
                if over_str then
                    draw_str_border(sx, sy)
                end
            end
        end
    end

    text.font()
end

return Arrangement
