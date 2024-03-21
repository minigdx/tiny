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
    },
}

-- from a content, set the correct values in the score pannel
mode.score.configure = function(self, content)
    local content = self.file_selector:current()
    
    for index, note in ipairs(content.tracks[1].patterns[1]) do
        self.sound.selector.selected = note.type
        self.sound.notes[index].tip_color = button_type[note.type].color 
        self.sound.notes[index]:set_value(note.note / 107)
        self.sound.volumes[index]:set_value(note.volume / 255)
    end
    
    self.sound.bpm.value = content.bpm / 255
    self.sound.volume.value = content.volume / 255
end

-- from a content, set the correct values in the fx pannel
mode.fx.configure = function(self, content)
    local content = self.file_selector:current()

    local mod = content.tracks[1].mod
    if mod.type == 1 then
        self.fx.sweep.checkbox.value = true
        self.fx.sweep.enabled = true
        self.fx.sweep.sweep = mod.a / 107
        self.fx.sweep.acceleration = mod.b / 255
        self.fx.sweep.knob_sweep.value = self.fx.sweep.sweep
        self.fx.sweep.knob_acceleration.value = self.fx.sweep.acceleration
    elseif mod.type == 2 then
        self.fx.vibrato.checkbox.value = true
        self.fx.vibrato.enabled = true
        self.fx.vibrato.vibrato = mod.a / 107
        self.fx.vibrato.depth = mod.b / 255
        self.fx.vibrato.knob_vibrato.value = self.fx.vibrato.vibrato
        self.fx.vibrato.knob_depth.value = self.fx.vibrato.depth
    end

    local env = content.tracks[1].env
    if env ~= nil then
        self.fx.envelope.attack_fader:set_value(env.attack / 255)
        self.fx.envelope.decay_fader:set_value(env.decay / 255)
        self.fx.envelope.sustain_fader:set_value(env.sustain / 255)
        self.fx.envelope.release_fader:set_value(env.release / 255)
    end
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
        w:on_update(on_click[i.customFields.Item])
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
        local files = ws.list("sfx")
        table.sort(files)
        if #files == 0 then
            local new_file = ws.create("sfx", "sfx")
            table.insert(file_selector.files, {
                file = new_file,
                content = sfx.to_table(sfx.empty_score())
            })
        else
            local i = 1
            for f in all(files) do
                table.insert(file_selector.files, {
                    file = f,
                    content = sfx.to_table(ws.load(f))
                })
                i = i + 1
            end
        end

        file_selector.next = find_widget(menu, file_selector.customFields.Next)
        file_selector.previous = find_widget(menu, file_selector.customFields.Previous)
        file_selector.screen = find_widget(menu, file_selector.customFields.Screen)
        file_selector.save = find_widget(menu, file_selector.customFields.Save)
        file_selector.new_file = find_widget(menu, file_selector.customFields.NewFile)
        file_selector.screen.label = true

        file_selector.next:on_update(function(self)
            file_selector.current_file = math.min(#file_selector.files, file_selector.current_file + 1)
            file_selector.screen:set_value(file_selector.files[file_selector.current_file].file)
            switch_to(current_mode)
        end)
        
        file_selector.previous:on_update(function(self)
            file_selector.current_file = math.max(1, file_selector.current_file - 1)
            file_selector.screen:set_value(file_selector.files[file_selector.current_file].file)
            switch_to(current_mode)
        end)

        file_selector.new_file:on_update(function(self)
           debug.console("creating file") 
           local new_file = ws.create("sfx", "sfx")
            table.insert(file_selector.files, {
                file = new_file,
                content = sfx.to_table(sfx.empty_score())
            })
            file_selector.current_file = #file_selector.files
            file_selector.next:set_value()
        end)

        file_selector.save:on_update(function(self)
            debug.console("saving file...")
            local score = sfx.to_score(file_selector:current())
            debug.console(file_selector:current())
            debug.console(score)
            ws.save(file_selector:currentName(), score)
            debug.console("file saved!") --
        end)
        file_selector.screen:set_value(file_selector.files[file_selector.current_file].file)
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
            local on_value_update = function(self, value)
                knob.attack = value
                local content = file_selector:current()
                content.tracks[1].env.attack = value * 255
            end
            f:on_update(on_value_update)

            f = find_widget(m.widgets, knob.customFields.Decay)
            knob.decay_fader = f
            local on_value_update = function(self, value)
                knob.decay = value
                local content = file_selector:current()
                content.tracks[1].env.decay = value * 255
            end
            f:on_update(on_value_update)


            f = find_widget(m.widgets, knob.customFields.Sustain)
            knob.sustain_fader = f
            local on_value_update = function(self, value)
                knob.sustain = value
                local content = file_selector:current()
                content.tracks[1].env.sustain = value * 255
            end
            f:on_update(on_value_update)

            f = find_widget(m.widgets, knob.customFields.Release)
            knob.release_fader = f
            local on_value_update = function(self, value)
                knob.release = value
                local content = file_selector:current()
                content.tracks[1].env.release = value * 255
            end
            f:on_update(on_value_update)

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
                end,
                switch = function(self, active)
                    self.enabled = active
                    self.checkbox.value = active
                    if active then
                        local content = file_selector:current()
                        content.tracks[1].mod.type = 2
                        content.tracks[1].mod.a = self.vibrato * 107
                        content.tracks[1].mod.b = self.depth * 255
                    end
                end
            }
            local knob = new(Vibrato, k)
            local e = find_widget(m.widgets, knob.customFields.Enabled)
            e.on_changed = function(self, value)
                knob.enabled = value
            end
            knob.checkbox = e

            local v = find_widget(m.widgets, knob.customFields.Vibrato)
            knob.knob_vibrato = v
            v.on_update = function(self, value)
                knob.vibrato = value
                local content = file_selector:current()
                content.tracks[1].mod.a = knob.vibrato * 107
            end
            local d = find_widget(m.widgets, knob.customFields.Depth)
            knob.knob_depth = d
            d.on_update = function(self, value)
                knob.depth = value
                local content = file_selector:current()
                content.tracks[1].mod.b = knob.depth * 255
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
                end,

                switch = function(self, active)
                    self.enabled = active
                    self.checkbox.value = active
                    if active then
                        local content = file_selector:current()
                        content.tracks[1].mod.type = 1
                        content.tracks[1].mod.a = self.sweep * 107
                        content.tracks[1].mod.b = self.acceleration * 255
                    end
                end
            }
            local knob = new(Sweep, k)
            local e = find_widget(m.widgets, knob.customFields.Enabled)
            knob.checkbox = e
            e.on_changed = function(self, value)
                knob.enabled = value
            end
            
            local v = find_widget(m.widgets, knob.customFields.Sweep)
            knob.knob_sweep = v
            v.on_update = function(self, value)
                knob.sweep = value
                local content = file_selector:current()
                content.tracks[1].mod.a = knob.sweep * 107
            end
            local d = find_widget(m.widgets, knob.customFields.Acceleration)
            knob.knob_acceleration = d
            d.on_update = function(self, value)
                knob.acceleration = value
                local content = file_selector:current()
                content.tracks[1].mod.b = knob.acceleration * 255
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
            local on_update = function(self)
                knob.selected = self.type
                for b in all(knob.selector) do
                    b.status = 0
                end
                self.status = 2
            end

            local e = find_widget(m.widgets, knob.customFields.Sine)
            table.insert(knob.selector, e)
            e:on_update(on_update)
            on_update(e) -- default selection

            e = find_widget(m.widgets, knob.customFields.Triangle)
            table.insert(knob.selector, e)
            e:on_update(on_update)
            
            e = find_widget(m.widgets, knob.customFields.Noise)
            table.insert(knob.selector, e)
            e:on_update(on_update)
            
            e = find_widget(m.widgets, knob.customFields.Pulse)
            table.insert(knob.selector, e)
            e:on_update(on_update)
            
            table.insert(m.widgets, knob)
        end

        local play = function(self)
            local content = file_selector:current()
            local score = sfx.to_score(content)
            sfx.sfx(score)
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
                local on_update = function(self, value)
                    local content = file_selector:current()
                    content.tracks[1].patterns[1][key].volume = value * 255
                end
                f:on_update(on_update)
            end
            
            for key, v in ipairs(k.customFields.Notes) do
                local f = find_widget(m.widgets, v)
                s.notes[key] = f
                local on_update = function(self, value)
                    self.tip_color = button_type[selector.selected].color
                    local content = file_selector:current()
                    content.tracks[1].patterns[1][key].type = selector.selected
                    content.tracks[1].patterns[1][key].note = value * 107 -- 107 = number of total notes

                    if content.tracks[1].patterns[1][key].volume <= 0 then
                        s.volumes[key]:set_value(1)
                    end
                end

                f:on_update(on_update)
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

            s.play = find_widget(m.widgets, k.customFields.Play)
            s.play:on_update(play)
            s.selector = selector
            m.sound = s
        end

        for k in all(map.entities["Fx"]) do
            local Fx = {
                envelope = nil,
                sweep = nil,
                vibrato = nil,
                tied_notes = nil
            }

            local fx = new(Fx, k)
            fx.envelope = find_widget(m.widgets, k.customFields.Envelope)
            fx.sweep = find_widget(m.widgets, k.customFields.Sweep)
            fx.vibrato = find_widget(m.widgets, k.customFields.Vibrato)

            fx.sweep.checkbox.on_changed = function(self)
                fx.sweep:switch(self.value)
                fx.vibrato:switch(not self.value)
            end
            fx.vibrato.checkbox.on_changed = function(self)
                fx.sweep:switch(not self.value)
                fx.vibrato:switch(self.value)
            end
            fx.tied_notes = find_widget(m.widgets, k.customFields.Envelope)
            fx.play = find_widget(m.widgets, k.customFields.Play)
            fx.play:on_update(play)
            m.fx = fx
        end
    end

    switch_to(mode.score)
end

function _update()
    mouse._update(function()
    end, function()
    end, function()
    end)
    
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

    for w in all(current_mode.widgets) do
        w:_draw()
    end
    mouse._draw(2)
end
