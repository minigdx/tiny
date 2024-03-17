local widgets = require("widgets")
local mouse = require("mouse")

local menu = {}
local help = nil

-- name to level index
local mode = {
    score = {
        id = 1,
        widgets = {}
    },
    fx = {
        id = 2,
        widgets = {}
    },
    music = {
        id = 3,
        widgets = {}
    }
}


local button_type = {
    Sine = {
        spr = 60,
        color = 9
    },
    Noise = {
        spr = 61,
        color = 4
    },
    Triangle = {
        spr = 63,
        color = 13
    },
    Pulse = {
        spr = 62,
        color = 10
    },
    Square = {
        spr = 62,
        color = 11
    },
    Silence = {
        spr = 62,
        color = 2
    },
    Play = {
        spr = 31
    },
    Prev = {
        spr = 32 * 5 + 28
    },
    Next = {
        spr = 32 * 5 + 29
    }
}


mode.score.configure = function(self, content)
    local content = self.file_selector:current()
    --
    -- TODO: allow to select another pattern
    for index, note in ipairs(content.tracks[1].patterns[1]) do
        self.sound.notes[index].value = note.note / 108
        self.sound.notes[index].tip_color = button_type[note.type].color 
        self.sound.volumes[index].value = note.volume / 255
    end

    self.sound.bpm.value = content.bpm / 255
    self.sound.volume.value = content.volume / 255
end

local current_mode = mode.score

function switch_to(new_mode)
    current_mode = new_mode
    if current_mode.configure ~= nil then
        current_mode:configure()
    end
end

local find_widget = function(widgets, ref)
    for w in all(widgets) do
        if w.iid == ref.entityIid then
            return w
        end
    end
end

function _init()
    widgets:_init()
    help = nil
    menu = {}

    local on_click = {
        Wave = function(self)
            switch_to(mode.score)
        end,
        Fx = function(self)
            switch_to(mode.fx)
        end,
        Music = function(self)
            switch_to(mode.music)
        end
    }

    for i in all(map.entities["MenuItem"]) do
        if i.customFields.Item == "Help" then
            local w = widgets:create_help(i)
            table.insert(menu, w)
            help = w
        end
    end

    local on_menu_item_hover = function(self)
        help.label = self.help
    end

    for i in all(map.entities["MenuItem"]) do
        local w = widgets:create_menu_item(i)
        table.insert(menu, w)
        w.on_hover = on_menu_item_hover
        w.on_click = on_click[i.customFields.Item]
        if i.customFields.Item == "Wave" then
            w.active = 1
        end
    end

    local FileSelector = {
        current_file = 1,
        files = {}, -- item -> {file, content}
        screen = nil,
        current = function(self)
            return self.files[self.current_file].content
        end,
        currentName = function(self)
            return self.files[self.current_file].file
        end
    }
    local file_selector = nil

    for i in all(map.entities["FilesSelector"]) do
        file_selector = new(FileSelector, i)
        local files = ws.list()
        if #files == 0 then
            local new_file = ws.create("sfx", "sfx")
            table.insert(file_selector.files, {
                file = new_file,
                content = sfx.to_table(sfx.empty_score())
            })
        else
            for f in all(files) do
                table.insert(file_selector.files, {
                    file = f,
                    content = sfx.to_table(ws.load(f))
                })
            end
        end
        file_selector.next = find_widget(menu, file_selector.customFields.Next)
        file_selector.previous = find_widget(menu, file_selector.customFields.Previous)
        file_selector.screen = find_widget(menu, file_selector.customFields.Screen)
        file_selector.save = find_widget(menu, file_selector.customFields.Save)

        file_selector.next.on_click = function(self)
            file_selector.current_file = math.min(#file_selector.files, file_selector.current_file + 1)
            file_selector.screen.label = file_selector.files[file_selector.current_file].file

        end

        file_selector.previous.on_click = function(self)
            file_selector.current_file = math.max(1, file_selector.current_file - 1)
            file_selector.screen.label = file_selector.files[file_selector.current_file].file
        end

        file_selector.save.on_click = function(self)
            debug.console("saving file...")
            local score = sfx.to_score(file_selector:current())
            ws.save(file_selector:currentName(), score)
            debug.console("savedfile!") --
        end
        file_selector.previous:on_click()
    end

    -- preload mode
    for name, m in pairs(mode) do
        m.file_selector = file_selector

        debug.console("preload screen " .. name)
        map.level(m.id)
        for k in all(map.entities["Knob"]) do
            local knob = widgets:create_knob(k)
            knob.on_hover = on_menu_item_hover
            table.insert(m.widgets, knob)
        end

        for k in all(map.entities["Button"]) do
            local knob = widgets:create_button(k)
            knob.on_hover = on_menu_item_hover
            knob.overlay = button_type[k.customFields.Type].spr
            knob.type = k.customFields.Type

            table.insert(m.widgets, knob)
        end

        for k in all(map.entities["Fader"]) do
            local knob = widgets:create_fader(k)
            knob.on_hover = on_menu_item_hover
            knob.id = k.customFields.Id
            knob.type = k.customFields.Type
            -- knob.on_value_update
            table.insert(m.widgets, knob)
        end

        for k in all(map.entities["Checkbox"]) do
            local knob = widgets:create_checkbox(k)
            knob.on_hover = on_menu_item_hover
            table.insert(m.widgets, knob)
        end

        for k in all(map.entities["Envelop"]) do
            local knob = widgets:create_envelop(k)
            knob.on_hover = on_menu_item_hover
            local f = find_widget(m.widgets, knob.customFields.Attack)
            knob.attack_fader = f
            f.on_value_update = function(self, value)
                knob.attack = value
            end

            f = find_widget(m.widgets, knob.customFields.Decay)
            knob.decay_fader = f
            f.on_value_update = function(self, value)
                knob.decay = value
            end

            f = find_widget(m.widgets, knob.customFields.Sustain)
            knob.sustain_fader = f
            f.on_value_update = function(self, value)
                knob.sustain = value
            end

            f = find_widget(m.widgets, knob.customFields.Release)
            knob.release_fader = f
            f.on_value_update = function(self, value)
                knob.release = value
            end

            table.insert(m.widgets, knob)
        end

        for k in all(map.entities["Vibrato"]) do
            local Vibrato = {
                enabled = false,
                vibrato = 0,
                depth = 0,
                _update = function(self)
                end,
                _draw = function(self)
                end
            }
            local knob = new(Vibrato, k)
            local e = find_widget(m.widgets, knob.customFields.Enabled)
            e.on_changed = function(self, value)
                knob.enabled = value
            end

            local v = find_widget(m.widgets, knob.customFields.Vibrato)
            v.on_update = function(self, value)
                knob.vibrato = value
            end
            local d = find_widget(m.widgets, knob.customFields.Depth)
            d.on_update = function(self, value)
                knob.depth = value
            end

            table.insert(m.widgets, knob)
        end

        for k in all(map.entities["Sweep"]) do
            local Sweep = {
                enabled = false,
                sweep = 0,
                acceleration = 0,
                _update = function(self)
                end,
                _draw = function(self)
                end
            }
            local knob = new(Sweep, k)
            local e = find_widget(m.widgets, knob.customFields.Enabled)
            e.on_changed = function(self, value)
                knob.enabled = value
            end

            local v = find_widget(m.widgets, knob.customFields.Sweep)
            v.on_update = function(self, value)
                knob.sweep = value
            end
            local d = find_widget(m.widgets, knob.customFields.Acceleration)
            d.on_update = function(self, value)
                knob.acceleration = value
            end

            table.insert(m.widgets, knob)
        end

        for k in all(map.entities["WaveSelector"]) do
            local WaveSelector = {
                selected = "Sine",
                selector = {},
                _update = function(self)
                end,
                _draw = function(self)
                end
            }
            local knob = new(WaveSelector, k)
            local on_changed = function(self)
                knob.selected = self.type
                debug.console("selected?")
                debug.console(knob.selected)
                for b in all(knob.selector) do
                    b.status = 0
                end
                self.status = 2
            end

            local e = find_widget(m.widgets, knob.customFields.Sine)
            table.insert(knob.selector, e)
            e.on_changed = on_changed
            e:on_changed() -- default selection

            e = find_widget(m.widgets, knob.customFields.Triangle)
            table.insert(knob.selector, e)
            e.on_changed = on_changed

            e = find_widget(m.widgets, knob.customFields.Noise)
            table.insert(knob.selector, e)
            e.on_changed = on_changed

            e = find_widget(m.widgets, knob.customFields.Pulse)
            table.insert(knob.selector, e)
            e.on_changed = on_changed

            table.insert(m.widgets, knob)
        end

        for k in all(map.entities["Sound"]) do
            local Sound = {
                volumes = {},
                notes = {},
                _draw = function(self)
                end,
                _update = function(self)
                end
            }
            local s = new(Sound, k)
            local selector = find_widget(m.widgets, k.customFields.WaveSelector)
            for key, v in ipairs(k.customFields.Volumes) do
                local f = find_widget(m.widgets, v)
                s.volumes[key] = f
                f.on_value_update = function(self, value)
                    local content = file_selector:current()
                    content.tracks[1].patterns[1][key].volume = value * 255
                end
            end
            
            for key, v in ipairs(k.customFields.Notes) do
                local f = find_widget(m.widgets, v)
                s.notes[key] = f
                f.on_value_update = function(self, value)
                    self.tip_color = button_type[selector.selected].color
                    local content = file_selector:current()
                    content.tracks[1].patterns[1][key].type = selector.selected
                    content.tracks[1].patterns[1][key].index = selector.selectedIndex
                    content.tracks[1].patterns[1][key].note = value * 108 -- 108 = number of total notes
                end
            end
            s.bpm = find_widget(m.widgets, k.customFields.BPM)
            s.bpm.on_update = function(self)
                local content = file_selector:current()
                -- TODO: update here
                content.bpm = self.value * 255
            end
            s.volume = find_widget(m.widgets, k.customFields.Volume)
            s.volume.on_update = function(self)
                local content = file_selector:current()
                content.volume = self.value * 255
            end
            m.sound = s
        end
    end

    switch_to(mode.score)
end

function _update()
    mouse._update(function()
    end, function()
    end, function()
    end)
    widgets:_update()

    for w in all(menu) do
        w:_update()
    end

    help:_update()

    for w in all(current_mode.widgets) do
        w:_update()
    end
end

function _draw()
    gfx.cls()

    map.level(0)
    map.draw()
    map.level(current_mode.id)
    map.layer(1)
    map.draw()

    for w in all(menu) do
        w:_draw()
    end
    help:_draw()

    widgets:_draw()

    for w in all(current_mode.widgets) do
        w:_draw()
    end
    mouse._draw(2)
end
