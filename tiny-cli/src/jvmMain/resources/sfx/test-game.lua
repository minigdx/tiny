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
    }
}

local button_type = {
    Sine = {
        spr = 60,
        color = 9
    },
    Noise = {
        spr = 61,
        color = 4,
    },
    Triangle = {
        spr = 63,
        color = 13
    },
    Pulse = {
        spr = 62,
        color = 10
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
local current_mode = mode.score

function switch_to(new_mode)
    current_mode = new_mode
    -- configure every buttons?
end

function _init()
    widgets:_init()
    help = nil
    menu = {}

    local on_click = {
        Prev = function(self)
            debug.console("previous")
        end,
        Next = function(self)
            debug.console("next")
        end,
        Wave = function(self)
            switch_to(mode.score)
        end,
        Fx = function(self)
            switch_to(mode.fx)
            debug.console("FX")
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
    end

    -- preload mode
    for name, m in pairs(mode) do
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

        local find_widget = function(widgets, ref)
            for w in all(widgets) do
                if w.iid == ref.entityIid then
                    return w
                end
            end
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
                    debug.log("v " .. self.vibrato)
                    debug.log("d " .. self.depth)
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
                    debug.log("s " .. self.sweep)
                    debug.log("a " .. self.acceleration)
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
            }
            local s = new(Sound, k)
            local selector = find_widget(m.widgets, k.customFields.WaveSelector)
            for key,v in ipairs(k.customFields.Volumes) do
                local f = find_widget(m.widgets, v)
                f.on_value_update = function(self, value)
                    s.volumes[key] = value
                end
            end

            for key,v in ipairs(k.customFields.Notes) do
                local f = find_widget(m.widgets, v)
                f.on_value_update = function(self, value)
                    s.notes[key] = value
                    self.tip_color = button_type[selector.selected].color
                    debug.console(self.tip_color)
                end
            end
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
