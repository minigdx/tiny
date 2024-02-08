package com.github.minigdx.tiny.lua

//language=Lua
const val SFX_WAVE_EXAMPLE = """
local waves = {
    { name = "sine", fun = sfx.sine, note = notes.C6 },
    { name = "pulse", fun = sfx.pulse, note = notes.C6 },
    { name = "triangle", fun = sfx.triangle, note = notes.C6 },
    { name = "noise", fun = sfx.noise, note = notes.C8 },
    { name = "sawtooth", fun = sfx.sawtooth, note = notes.C6 },
    { name = "square", fun = sfx.square, note = notes.C6 },
}

local index = 1

function new_index(index, tableSize)
     return ((index - 1) % tableSize) + 1
end

function _update()
    if ctrl.pressed(keys.left) then
        index =new_index(index - 1, #waves)
    elseif ctrl.pressed(keys.right) then
        index = new_index(index + 1, #waves)
    end

    if ctrl.pressed(keys.space) then
        waves[index].fun( waves[index].note)
    end
end

function _draw()
    gfx.cls()
    local str = "<<-- "..waves[index].name .. " -->>"
    print(str, 128 - math.floor(#str * 4 * 0.5), 128 )


    local space = "(hit space to play a note)"
    print(space, 128 - math.floor(#space * 4 * 0.5), 142)
end
"""
