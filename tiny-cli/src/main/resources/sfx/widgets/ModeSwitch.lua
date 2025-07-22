local ModeSwitch = {
    hover_index = nil,
    selected_index = 0,
    button_width = 16,
    button_height = 16,
    button_margin_right = 8,
    buttons = {
        {
            overlay = { x = 0, y = 9*16 },
            on_change = function()
                tiny.exit("instrument-editor.lua")
            end,
            help = "Instrument Editor"
        },
        {
            overlay = { x = 16, y = 9*16 },
            on_change = function()
                tiny.exit("sfx-editor.lua")
            end,
            help = "SFX Editor"
        },
        {
            overlay = { x = 32, y = 9*16 },
            on_change = function()
                tiny.exit("music-editor.lua")
            end,
            help = "Music Editor"
        }
    }
}

ModeSwitch._update = function(self)
    local pos = ctrl.touch()
    
    if inside_widget(self, pos.x, pos.y) then
        local relative_x = pos.x - self.x
        local button_index = math.floor(relative_x / (self.button_width + self.button_margin_right))
        local is_hover_button = (relative_x - button_index * (self.button_width + self.button_margin_right)) <= self.button_width

        if is_hover_button and button_index >= 0 and button_index < #self.buttons then
            self.hover_index = button_index
            
            if self.on_hover then
                self:on_hover()
            end
            
            if ctrl.touched(0) then
                self.selected_index = button_index
                local button = self.buttons[button_index + 1]
                if button.on_change then
                    button.on_change()
                end
                if self.on_change then
                    self:on_change()
                end
            end
        else
            self.hover_index = nil
        end
    else
        self.hover_index = nil
    end
end

ModeSwitch._draw = function(self)
    for i = 0, #self.buttons - 1 do
        local button = self.buttons[i + 1]
        local x = self.x + i * (self.button_width + self.button_margin_right)
        local y = self.y
        
        local background = 0
        if self.hover_index == i or self.selected_index == i then
            background = 16
        end
        
        spr.sdraw(x, y, 0 + background, 0, self.button_width, self.button_height)
        
        if button.overlay then
            spr.sdraw(x, y, button.overlay.x, button.overlay.y, self.button_width, self.button_height)
        end
    end
end

return ModeSwitch