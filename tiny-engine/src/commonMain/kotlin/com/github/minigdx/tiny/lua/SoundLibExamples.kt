package com.github.minigdx.tiny.lua

//language=Lua
const val NOTE_EXAMPLE = """
function _init()
    keys = {
        {note="E4", x=57, y=100, w=16, h=50},
        {note="Fs4", x=75, y=100, w=16, h=50},
        {note="Gs4", x=93, y=100, w=16, h=50},
        {note="A4", x=111, y=100, w=16, h=50},
        {note="B4", x=129, y=100, w=16, h=50},
        {note="Cs5", x=147, y=100, w=16, h=50},
        {note="Ds5", x=165, y=100, w=16, h=50},
        {note="E5", x=183, y=100, w=16, h=50}
    }
    active_notes = {}
end

function _update()
    local touch = ctrl.touching(0)

    for i, key in ipairs(keys) do
        if in_bounds(touch, key) then
            active_notes[i] = active_notes[i] or sound.note(key.note, 1)
        else
            active_notes[i] = active_notes[i] and active_notes[i].stop()
        end
    end
end

function _draw()
    gfx.cls()
    print("E MAJOR SCALE", 57, 84, 15)
    print("Click keys to play", 57, 92, 14)

    for i, key in ipairs(keys) do
        if active_notes[i] then
            shape.rectf(key.x, key.y, key.w, key.h, 15)
        else
            shape.rect(key.x, key.y, key.w, key.h, 14)
        end
    end
    local pos = ctrl.touch()
    shape.circlef(pos.x, pos.y, 2, 2)
end

function in_bounds(touch, key)
    return touch and touch.x >= key.x and touch.x < key.x + key.w and
           touch.y >= key.y and touch.y < key.y + key.h
end
"""
