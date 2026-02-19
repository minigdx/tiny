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
    local index = 1
    for k,v in pairs(result) do
        print(k..":"..v , index * 4 * 8, 8)
        index = index + 1
    end
end
"""

//language=Lua
const val STD_APPEND_EXAMPLE = """
function _draw()
    gfx.cls()
    local src = {1, 2, 3}
    local dst = {4, 5}
    local result = append(src, dst)
    for k,v in ipairs(result) do
        print(v, (k + 1) * 8, 8)
    end
end
"""

//language=Lua
const val STD_TEXTW_EXAMPLE = """
function _draw()
    gfx.cls()
    local text = "hello world"
    local w = textw(text)
    print(text, 10, 10, 4)
    -- draw a line under the text showing its width
    shape.line(10, 17, 10 + w, 17, 7)
    print("width=" .. w, 10, 20)
end
"""

//language=Lua
const val STD_TEXTH_EXAMPLE = """
function _draw()
    gfx.cls()
    local text = "hello world this is a long text"
    -- measure height with word wrapping at 60px
    local h = texth(text, 60)
    print("height=" .. h, 0, 0)
    -- draw the wrapped text and a bounding box
    printf(text, 10, 10, 4, 60)
    shape.rect(10, 10, 60, h, 7)
end
"""

//language=Lua
const val STD_PRINTF_EXAMPLE = """
function _draw()
    gfx.cls()
    local text = "the quick brown fox jumps over the lazy dog"
    -- left aligned (default)
    printf(text, 10, 10, 4, 80, 0)
    -- centered
    printf(text, 10, 50, 5, 80, 1)
    -- right aligned
    printf(text, 10, 90, 6, 80, 2)
    -- draw bounding boxes
    shape.rect(10, 10, 80, 24, 7)
    shape.rect(10, 50, 80, 24, 7)
    shape.rect(10, 90, 80, 24, 7)
end
"""
