local MatrixSelector = {
    hover_index = nil,
    value = nil,
    size = 16,
    active_indices = {}
}

MatrixSelector._update = function(self)
    local p = ctrl.touch()

    local cols = math.ceil(math.sqrt(self.size))
    local rows = math.ceil(self.size / cols)
    local cell_width = self.width / cols
    local cell_height = self.height / rows

    if inside_widget(self, p.x, p.y) then
        local x = p.x - self.x
        local y = p.y - self.y

        local col = math.floor(x / cell_width)
        local row = math.floor(y / cell_height)
        local index = col + row * cols

        if index < self.size then
            self.hover_index = index
        else
            self.hover_index = nil
        end
    else
        self.hover_index = nil
    end

    if (self.hover_index and ctrl.touched(0)) then
        self.value = self.hover_index
        if (self.on_change) then
            self:on_change()
        end
    end
end

MatrixSelector._draw = function(self)
    local cols = math.ceil(math.sqrt(self.size))
    local rows = math.ceil(self.size / cols)
    local cell_width = self.width / cols
    local cell_height = self.height / rows
    
    local index = 0

    for row = 0, rows - 1 do
        for col = 0, cols - 1 do
            if index < self.size then
                local x = self.x + col * cell_width
                local y = self.y + row * cell_height
                local is_active = self:is_active(index)

                if (self.value == index) then
                    -- Selected index: filled with color 3
                    shape.rectf(x, y, cell_width, cell_height, 3)
                elseif (self.hover_index == index) then
                    -- Hovered index: border with color 3
                    shape.rect(x, y, cell_width, cell_height, 3)
                elseif is_active then
                    -- Active index (not selected, not hovered): filled with color 8
                    shape.rectf(x, y, cell_width, cell_height, 4)
                else
                    -- Inactive index: border with color 4
                    shape.rect(x, y, cell_width, cell_height, 4)
                end
                print(index, x + 2, y + 2)
            end
            index = index + 1
        end
    end
end

-- Function to toggle the active state of an index
MatrixSelector.toggle_active = function(self, index)
    if self:is_active(index) then
        -- Remove from active list
        for i = #self.active_indices, 1, -1 do
            if self.active_indices[i] == index then
                table.remove(self.active_indices, i)
                break
            end
        end
    else
        -- Add to active list
        table.insert(self.active_indices, index)
    end
end

-- Function to check if an index is active
MatrixSelector.is_active = function(self, index)
    for i = 1, #self.active_indices do
        if self.active_indices[i] == index then
            return true
        end
    end
    return false
end

return MatrixSelector