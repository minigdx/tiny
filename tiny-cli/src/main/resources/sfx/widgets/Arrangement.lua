local utils = require("widgets.utils")
local inside_widget = utils.inside_widget
local inside_rect = utils.inside_rect

local MAX_SLOTS = 8
local ARROW_W = 10  -- width reserved for the loop arrow area on the left
local SLOT_PAD = 1  -- padding between slots

-- Sprite coordinates on sheet 2
local PATTERN_BG_SX, PATTERN_BG_SY = 48, 120
local PATTERN_BG_W, PATTERN_BG_H = 26, 12

local LEFT_ARROW_SX, LEFT_ARROW_SY = 48, 136
local LEFT_ARROW_HOVER_SX, LEFT_ARROW_HOVER_SY = 48, 144
local RIGHT_ARROW_SX, RIGHT_ARROW_SY = 56, 136
local RIGHT_ARROW_HOVER_SX, RIGHT_ARROW_HOVER_SY = 56, 144
local ARROW_SIZE = 8

local SLOT_CONTENT_W = ARROW_SIZE + PATTERN_BG_W + ARROW_SIZE

local Arrangement = {
    x = 0,
    y = 0,
    width = ARROW_W + SLOT_CONTENT_W,
    height = 104,
    -- arrangement data: array of pattern indices (1-based Lua), nil = empty
    slots = {},
    -- loop arrow position (0-based arrangement index, -1 = no loop)
    loop_index = 0,
    -- max pattern index (0-based) the user can assign
    max_pattern = 0,
    -- dragging state for the loop arrow
    dragging = false,
    drag_start_y = 0,
    -- callback
    on_change = function(self) end,
    listeners = {},
    on_update = utils.on_update,
    fire_on_update = utils.fire_on_update,
}

local function slot_height(self)
    return math.floor(self.height / MAX_SLOTS)
end

local function slot_y(self, i)
    return self.y + (i - 1) * slot_height(self)
end

local function slot_x(self)
    return self.x + ARROW_W
end

local function slot_w(self)
    return self.width - ARROW_W
end

-- How many slots are filled (non-nil)?
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

-- Arrow handle area (small rectangle next to the triangle)
local function arrow_handle_rect(self)
    local idx = self.loop_index + 1  -- 1-based
    local fc = filled_count(self)
    if fc == 0 then fc = 1 end
    -- Clamp arrow to valid range
    if idx < 1 then idx = 1 end
    if idx > fc then idx = fc end
    local sy = slot_y(self, idx)
    local sh = slot_height(self)
    local handle_y = sy + math.floor(sh / 2) - 3
    return self.x, handle_y, ARROW_W - 2, 7
end

Arrangement._init = function(self)
    self.slots = {}
    for i = 1, MAX_SLOTS do
        self.slots[i] = nil
    end
end

-- Load arrangement data from a sequence object
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
end

-- Write arrangement data back to a sequence object
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
    local pos = ctrl.touch()

    -- Handle drag of loop arrow
    if self.dragging then
        local touching = ctrl.touching(0)
        if touching then
            -- Compute which slot the mouse is closest to
            local sh = slot_height(self)
            local rel_y = pos.y - self.y
            local slot_idx = math.floor(rel_y / sh)
            local fc = filled_count(self)
            if fc == 0 then fc = 1 end
            -- Clamp: 0 to fc (fc means "after the last pattern" = no loop)
            slot_idx = math.max(0, math.min(fc, slot_idx))
            if slot_idx >= fc then
                -- Past the last filled slot = no loop
                self.loop_index = -1
            else
                self.loop_index = slot_idx
            end
        else
            self.dragging = false
            self:on_change()
        end
        return
    end

    -- Check for click/touch on the arrow handle to start drag
    local touched = ctrl.touched(0)
    if touched then
        local hx, hy, hw, hh = arrow_handle_rect(self)
        if inside_rect(touched.x, touched.y, hx, hy, hw, hh) then
            self.dragging = true
            return
        end
    end

    -- Check for click on slots
    if touched then
        local sx = slot_x(self)
        local sh = slot_height(self)

        for i = 1, MAX_SLOTS do
            local sy = slot_y(self, i)
            local pattern_x = sx + ARROW_SIZE
            if inside_rect(touched.x, touched.y, pattern_x, sy, PATTERN_BG_W, PATTERN_BG_H) then
                local fc = filled_count(self)
                local is_filled = self.slots[i] ~= nil

                -- Arrow positions (matching _draw)
                local left_x = sx + 10
                local right_x = pattern_x + PATTERN_BG_W - 8
                local arrow_vy = sy + 3
                local on_left_arrow = is_filled and inside_rect(touched.x, touched.y, left_x, arrow_vy, ARROW_SIZE, ARROW_SIZE)
                local on_right_arrow = is_filled and inside_rect(touched.x, touched.y, right_x, arrow_vy, ARROW_SIZE, ARROW_SIZE)

                if on_right_arrow then
                    -- Right arrow: decrement or clear
                    if is_filled then
                        if self.slots[i] > 0 then
                            self.slots[i] = self.slots[i] - 1
                        else
                            -- Can only clear if it's the last filled slot
                            if i == fc and i > 1 then
                                self.slots[i] = nil
                            end
                        end
                    end
                elseif on_left_arrow then
                    -- Left arrow: increment or fill next empty slot
                    if is_filled then
                        if self.slots[i] < self.max_pattern then
                            self.slots[i] = self.slots[i] + 1
                        end
                    else
                        if i == fc + 1 then
                            self.slots[i] = 0
                        end
                    end
                else
                    -- Click on pattern area (not on arrows): fill next empty slot
                    if not is_filled and i == fc + 1 then
                        self.slots[i] = 0
                    end
                end

                -- Adjust loop_index if it points past the filled area
                local new_fc = filled_count(self)
                if self.loop_index >= new_fc then
                    self.loop_index = math.max(0, new_fc - 1)
                end

                self:on_change()
                break
            end
        end
    end
end

Arrangement._draw = function(self)
    local sh = slot_height(self)
    local sx = slot_x(self)
    local fc = filled_count(self)
    local pos = ctrl.touch()

    text.font("monogram")

    -- Draw slots using sprites
    local prev = spr.sheet(2)

    for i = 1, MAX_SLOTS do
        local sy = slot_y(self, i)
        local is_filled = self.slots[i] ~= nil

        -- Layout positions
        local left_x = sx + 10
        local pattern_x = sx + ARROW_SIZE
        local right_x = pattern_x + PATTERN_BG_W - 8
        local arrow_vy = sy + 3

        -- Pattern button background
        spr.sdraw(pattern_x, sy, PATTERN_BG_SX, PATTERN_BG_SY, PATTERN_BG_W, PATTERN_BG_H)

        -- Left arrow (shown if clicking it would do something)
        local show_left = is_filled or i == fc + 1
        if show_left then
            local left_hover = inside_rect(pos.x, pos.y, left_x, arrow_vy, ARROW_SIZE, ARROW_SIZE)
            if left_hover then
                spr.sdraw(left_x, arrow_vy, LEFT_ARROW_HOVER_SX, LEFT_ARROW_HOVER_SY, ARROW_SIZE, ARROW_SIZE)
            else
                spr.sdraw(left_x, arrow_vy, LEFT_ARROW_SX, LEFT_ARROW_SY, ARROW_SIZE, ARROW_SIZE)
            end
        end

        if is_filled then
            -- Pattern ID text (centered on pattern background)
            local val_str = tostring(self.slots[i])
            local tx = pattern_x + math.floor(PATTERN_BG_W / 2) - math.floor(#val_str * 3)
            text.print(val_str, tx, sy + 1, 1)

            -- Right arrow (only for filled slots)
            local right_hover = inside_rect(pos.x, pos.y, right_x, arrow_vy, ARROW_SIZE, ARROW_SIZE)
            if right_hover then
                spr.sdraw(right_x, arrow_vy, RIGHT_ARROW_HOVER_SX, RIGHT_ARROW_HOVER_SY, ARROW_SIZE, ARROW_SIZE)
            else
                spr.sdraw(right_x, arrow_vy, RIGHT_ARROW_SX, RIGHT_ARROW_SY, ARROW_SIZE, ARROW_SIZE)
            end
        end
    end

    spr.sheet(prev)

    -- Draw loop arrow
    local arrow_slot = self.loop_index
    if arrow_slot >= 0 and fc > 0 then
        local arrow_i = arrow_slot + 1  -- 1-based
        if arrow_i > fc then arrow_i = fc end
        local ay = slot_y(self, arrow_i) + math.floor(sh / 2)

        -- Dithered vertical line from arrow to bottom of filled area
        local line_top = slot_y(self, 1) + 2
        local line_bottom = slot_y(self, fc) + sh - 3
        local line_x = self.x + 3

        gfx.dither(0x5050)
        shape.line(line_x, line_top, line_x, line_bottom, 3)
        gfx.dither()

        -- Arrow triangle (pointing right)
        local tx = self.x + 1
        local ty = ay
        shape.rectf(tx, ty - 2, 5, 5, 6)
        -- Triangle shape: draw 3 rows
        gfx.pset(tx + 5, ty - 1, 6)
        gfx.pset(tx + 5, ty, 6)
        gfx.pset(tx + 5, ty + 1, 6)
        gfx.pset(tx + 6, ty, 6)

        -- Arrow handle (small rectangle for dragging)
        shape.rectf(self.x, ay - 3, 2, 7, 6)
    elseif fc > 0 then
        -- No loop: show a small mark at the last slot
        local ay = slot_y(self, fc) + math.floor(sh / 2)
        local line_x = self.x + 3
        gfx.dither(0x5050)
        shape.line(line_x, slot_y(self, 1) + 2, line_x, ay, 3)
        gfx.dither()
        -- X mark to indicate no loop
        shape.line(self.x + 1, ay - 2, self.x + 5, ay + 2, 4)
        shape.line(self.x + 5, ay - 2, self.x + 1, ay + 2, 4)
    end

    text.font()
end

return Arrangement
