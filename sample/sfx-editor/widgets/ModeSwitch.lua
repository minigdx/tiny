local ModeSwitch = {
    hover_index = nil,
    selected_index = 0,
    button_width = 16,
    button_height = 16,
    button_margin_right = 8,
    buttons = {
        Instrument = {
            overlay = { x = 16, y = 80 },
            on_change = function()
                tiny.exit("instrument-editor.lua")
            end,
            help = "Instrument Editor"
        },
        Sfx = {
            overlay = { x = 32, y = 80 },
            on_change = function()
                tiny.exit("sfx-editor.lua")
            end,
            help = "SFX Editor"
        },
        Music = {
            overlay = { x = 32, y = 9 * 16 },
            on_change = function()
                tiny.exit("music-editor.lua")
            end,
            help = "Music Editor"
        },
    },
    background_unselected = { x = 112, y = 0 },
    background_hover = { x = 120, y = 0 },
    background_selected = { x = 112, y = 8 }
}

ModeSwitch._update = function(self)
    local pos = ctrl.touch()

    if inside_widget(self, pos.x, pos.y) then
        self.hover = true
        if ctrl.touched(0) then
            local button = self.buttons[self.fields.ModeType]
            button.on_change()
        end
    else
        self.hover = false
    end

end

ModeSwitch._draw = function(self)

    local button = self.buttons[self.fields.ModeType]

    if self.fields.IsSelected then
        for i = 0, 8, 8 do
            for j = 0, 8, 8 do
                spr.sdraw(self.x + i, self.y + j, self.background_selected.x, self.background_selected.y, 8, 8)
            end
        end
    elseif self.hover then
        for i = 0, 8, 8 do
            for j = 0, 8, 8 do
                spr.sdraw(self.x + i, self.y + j, self.background_hover.x, self.background_hover.y, 8, 8)
            end
        end
    else
        for i = 0, 8, 8 do
            for j = 0, 8, 8 do
                spr.sdraw(self.x + i, self.y + j, self.background_unselected.x, self.background_unselected.y, 8, 8)
            end
        end
    end

    spr.sdraw(self.x, self.y, button.overlay.x, button.overlay.y, self.width, self.height)

end

return ModeSwitch