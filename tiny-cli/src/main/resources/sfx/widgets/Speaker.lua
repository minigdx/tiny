local Speaker = { x = 0, y = 0, width = 98, height = 96 }

local NOTE_SPR_X = 56
local NOTE_SPR_Y = 40
local NOTE_SIZE = 16
local NOTE_LIFETIME = 1.0
local NOTE_SPAWN_INTERVAL = 0.12

Speaker._update = function(self)
    self.particles = self.particles or {}
    self.spawn_timer = self.spawn_timer or 0

    if self.playing then
        self.spawn_timer = self.spawn_timer - tiny.dt
        if self.spawn_timer <= 0 then
            self.spawn_timer = NOTE_SPAWN_INTERVAL

            -- spawn from near the center of the speaker
            local side = math.random() > 0.5 and 1 or -1
            local cx = self.x + self.width / 2
            local cy = self.y + self.height / 2

            table.insert(self.particles, {
                x = cx + side * (4 + math.random() * 6),
                y = cy + (math.random() - 0.5) * 8,
                vx = side * (40 + math.random() * 30),
                vy = -(30 + math.random() * 35),
                life = NOTE_LIFETIME,
            })
        end
    end

    -- update particles
    local alive = {}
    for _, p in ipairs(self.particles) do
        p.life = p.life - tiny.dt
        if p.life > 0 then
            p.vy = p.vy + 80 * tiny.dt
            p.x = p.x + p.vx * tiny.dt
            p.y = p.y + p.vy * tiny.dt
            table.insert(alive, p)
        end
    end
    self.particles = alive
end

Speaker._draw = function(self)
    local prev = spr.sheet(2)

    -- draw speaker
    spr.sdraw(self.x, self.y, 208, 0, 48, 96)
    spr.sdraw(self.x + 48, self.y, 208, 0, 48, 96, true, false)

    -- draw note particles
    if self.particles then
        for _, p in ipairs(self.particles) do
            local alpha = p.life / NOTE_LIFETIME
            -- fade out: skip drawing if too faint
            if alpha > 0.1 then
                spr.sdraw(p.x - NOTE_SIZE / 2, p.y - NOTE_SIZE / 2, NOTE_SPR_X, NOTE_SPR_Y, NOTE_SIZE, NOTE_SIZE)
            end
        end
    end

    spr.sheet(prev)
end

return Speaker
