local utils = require("widgets.utils")

local ModeSwitch = {
    modes = { "Instrument", "Sfx", "Music", "MusicFactory" },
    scripts = {
        Instrument = "tiny-instrument-editor.lua",
        Sfx = "tiny-sfx-editor.lua",
        Music = nil,
        MusicFactory = nil,
    },
    hover_index = nil,
}

ModeSwitch._update = function(self)
    local pos = ctrl.touch()
    self.hover_index = nil

    if utils.inside_widget(self, pos.x, pos.y) then
        local rel = pos.x - self.x
        local idx = math.floor(rel / 8) + 1
        if idx >= 1 and idx <= 4 then
            self.hover_index = idx
        end

        if ctrl.touched(0) and self.hover_index then
            local mode = self.modes[self.hover_index]
            if mode ~= self.fields.Active and self.scripts[mode] then
                tiny.exit(self.scripts[mode])
            end
        end
    end
end

ModeSwitch._draw = function(self)
    local active = self.fields.Active
    for i = 1, 4 do
        local src_x = 80 + (i - 1) * 8
        local src_y = 48
        if self.modes[i] == active or i == self.hover_index then
            src_y = 56
        end
        spr.sdraw(self.x + (i - 1) * 8, self.y, src_x, src_y, 8, 8)
    end
end

return ModeSwitch
