package com.github.minigdx.tiny.lua

//language=Lua
const val STD_PRINT_EXAMPLE = """
function _draw()
    gfx.cls()
    -- every character is a sprite 4x4 pixels.
    print("hello")
    print("world", 10, 10)
    print("how", 10, 20, 4)
    print("are", 26, 20, 5)
    print("you", 42, 20, 6)
    print("...", 58, 20, math.rnd(10))
end
"""

//language=Lua
const val STD_RPAIRS_EXAMPLE = """
function _draw()
    gfx.cls()
    local data = {
        { name = "riri" },
        { name = "fifi" },
        { name = "loulou" }
    }

    local y = 0
    for index, key in rpairs(data) do
        print(index .. " - " .. key.name, 10, y)
        y = y + 10
    end
end
"""

//language=Lua
const val STD_NEW_EXAMPLE = """
local Player = {
    x = 128,
    y = 128,
    color = 8
}

function Player:draw()
    shape.rectf(self.x, self.y, 10, 10, self.color)
end

function _init()
    -- create a new player
    player = new(Player)
    -- create a new player with default vaules
    player2 = new(Player, {x = 200, y = 200, color = 9})
end

function _draw()
    gfx.cls()
    player:draw() -- call the draw method on the player instance.
    player2:draw() -- call the draw method on the player2 instance.
end
"""

//language=Lua
const val STD_MERGE_EXAMPLE = """
function _draw()
    gfx.cls()
    local src = {x = 1, y = 2, z = 3}
    local dst = {a = 4, b = 5}
    local result = merge(src, dst)
    debug.table(result)
end
"""

//language=Lua
const val STD_APPEND_EXAMPLE = """
function _draw()
    gfx.cls()
    local src = {1, 2, 3}
    local dst = {4, 5}
    local result = append(src, dst)
    debug.table(result)
end
"""
