local mouse = require("mouse")

local triggers = {}

local Trigger = {
    tick = 0,
    note = 0,
    width = 8,
    height = 4,
    active = false,
    player = false,
}

local metronome = {
    tick = 0
}

function _init()
    for x = 1, 10 do
        for y = 1, 40 do
            table.insert(triggers, new(Trigger, {
                tick = x,
                note = y,
                x = x * 10,
                y = y * 6
            }))
        end
    end
end

function on_click(x, y)
    for i = 1, #triggers do
        local trigger = triggers[i]
        if x > trigger.x and x < trigger.x + trigger.width and y > trigger.y and y < trigger.y + trigger.height then
            trigger.active = not trigger.active
        end
    end
end

function tick_updated()
    for i = 1, #triggers do
        local trigger = triggers[i]
        if metronome.tick == trigger.tick and trigger.active then
            sfx.noise(trigger.note, 0.1666)
        end
    end
end

function _update()
    mouse._update(on_click)

    if tiny.frame % 10 == 0 then
        metronome.tick = metronome.tick + 1
        if (metronome.tick > 10) then
            metronome.tick = 1
        end
        tick_updated()
    end
end

function _draw()
    gfx.cls()

    for i = 1, #triggers do
        local trigger = triggers[i]
        local color = 7
        if trigger.tick == metronome.tick then
            color = 9
        end
        if(trigger.active) then
            -- gfx.dither(0xA5A5)
            shape.rectf(trigger.x, trigger.y, trigger.width, trigger.height, 8)
        else
            shape.rect(trigger.x, trigger.y, trigger.width, trigger.height, color)
            -- gfx.dither()
        end
    end

    mouse._draw()
end
