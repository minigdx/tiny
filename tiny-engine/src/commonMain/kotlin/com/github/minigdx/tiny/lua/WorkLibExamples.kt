package com.github.minigdx.tiny.lua

//language=Lua
const val WORK_ADD_EXAMPLE = """
local x = 0
local y = 0

function _init()
    -- Add a background task that moves x every 0.5 seconds
    work.add(function()
        while true do
            x = x + 10
            work.wait(0.5) -- pause for 0.5 seconds
        end
    end)

    -- Add a task that waits for x to reach 50
    work.add(function()
        work.wait_until(function() return x >= 50 end)
        y = 100 -- triggered when x >= 50
    end)
end

function _draw()
    gfx.cls()
    shape.rectf(x, 10, 8, 8, 4)
    shape.rectf(10, y, 8, 8, 7)
    print("x=" .. x, 0, 0)
    print("tasks=" .. work.count(), 0, 8)
end
"""
