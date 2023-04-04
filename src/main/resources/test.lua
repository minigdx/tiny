function _init()
    player = {
        x = 0,
        y = 0,
        sprite = 241,
        idle = 0,
        idleAnim = {
            [20] = 260,
            [38] = 265,
            [56] = 260,
            [126] = -1
        },
        jump = 0,
        jumping = false,
        hitLeft = function()
            return {x = self.x + 6, y = self.y + 16 + 1}
        end,
        hitRight = function()
            return {x = self.x + 10, y = self.y + 16 + 1}
        end
    }

    track = {
        {x = -10, y = -10, c = 9, f = 0.1},
        {x = -10, y = -10, c = 10, f = 0.15},
        {x = -10, y = -10, c = 11, f = 0.2}
    }

    index = 0
    dt = 0
    boom = {}

    badGuys = {}
end

function _update()
    p1 = {x = player.x + 6, y = player.y + 16 + 1}
    p2 = {x = player.x + 10, y = player.y + 16 + 1}

    sonde1 = map.flag(p1.x / 16, p1.y / 16)
    sonde2 = map.flag(p2.x / 16, p2.y / 16)

    -- jump
    if (ctrl.key(1)) then
        player.jump = -20
    else
        player.jump = max(0, player.jump - 1)
    end

    if (sonde1 == 0 and sonde2 == 0) then
        speed = -3 -- vector
    else
        speed = 0
    end

    player.y = player.y - speed + player.jump

    if ready then
        for k, v in pairs(map.entity.Player) do
            player.x = v.x
            player.y = v.y
        end
        for k, v in pairs(map.entity.BadGuys) do
            badGuys[k] = {x = v.x, y = v.y}
        end
        ready = false
    end

    player.idle = player.idle + 1
    if player.idleAnim[player.idle] == -1 then
        player.idle = 0
        player.sprite = 241
    elseif player.idleAnim[player.idle] == nil then
        -- noop
    else
        player.sprite = player.idleAnim[player.idle]
    end

    if (ctrl.down(0)) then
        player.x = player.x - 1
    elseif (ctrl.down(2)) then
        player.x = player.x + 1
    end

    if (ctrl.down(1)) then
        player.y = player.y - 1
    elseif (ctrl.down(3)) then
        player.y = player.y + 1
    end

    for k, v in pairs(track) do
        v.x = lerp(v.x, player.x, v.f)
        v.y = lerp(v.y, player.y, v.f)
    end
end

function lerp(start, finish, t)
    return (1 - t) * start + t * finish
end

function _draw()
    dt = dt + 1 / 60
    cls(2)

    map.draw()
    -- toto.draw()
    -- debug.traceback(cls(1))
    --    gfx.pal(8, 9)
    gfx.dither(1)
    for k, v in pairs(track) do
        gfx.pal(8, v.c)
        spr(player.sprite, v.x, v.y)
    end
    gfx.pal()
    
    
    gfx.dither(0)
    
    spr(player.sprite, player.x, player.y)

    for k, v in pairs(badGuys) do
        spr(340, v.x, v.y)
        -- badGuys[k] = { x = v.x, y = v.y }
    end

    pset(p1.x, p1.y, 9)
    pset(p2.x, p2.y, 10)
end
