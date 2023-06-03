--[[
Particles library

This library will help you to manage simple particles drawn as circle.
The position, speed or color of particles can be set on the generator.

Example:

function _init()
    generator = new(Particles, {
        x = { 126, 127, 128, 129, 130, 131, 132 },
        y = { 126, 127, 128, 129, 130, 131, 132 },
        sx = { -0.5, 0, 0.5 },
        sy = { -0.5, 0, 0.5 },
        colors = { 7, 8, 9, 2, 3, 4, 3, 6 },
        ttl = { 4, 5, 6 },
        radius = { 4, 6, 7 },
    })
end

function _update()
    if ctrl.pressed(keys.space) then
        generator:emit(5)
    end
    generator:_update()
end

function _draw()
    gfx.cls()
    generator:_draw()
end

]] --
local Particles = {
    x = {0}, -- x position of the particle
    y = {0}, -- y position of the particle
    radius = {4}, -- start radius of the particle
    colors = {1}, -- color used to draw the particle
    sx = 0, -- x speed of the particle
    sy = 1, -- y speed of the particle
    sr = 0.1, -- radius speed of the particle
    ttl = 1, -- ttl, time to live in seconds. The particle is detroy when reach 0
    particles = {} -- list of the particles managed by this particle generator
}

-- Update all particles of the generator
-- This methods should be called by the user in the _update function.
function Particles:_update()
    local colors = self.__asTable(self.colors)

    for k, v in rpairs(self.particles) do
        v.x = v.x + v.sx
        v.y = v.y + v.sy
        v.r = v.r - v.sr
        v.ttl = v.ttl - tiny.dt
        v.frame = v.frame + 1
        v.color = colors[math.min(#colors, v.frame)]
        if v.ttl < 0 then
            table.remove(self.particles, k)
        end
    end
end

-- Draw all particles of the generator
-- This methods should be called by the user in the _draw function.
function Particles:_draw()
    for p in all(self.particles) do
        self:__draw(p)
    end
end

-- PRIVATE --

local __Particle = {
    x = 0,
    y = 0,
    r = 0,
    sx = 0,
    sy = 0,
    sr = 0,
    ttl = 0,
    frame = 0,
    color = 0
}

-- return the value or the random value of a table
function Particles:__get(value)
    if (type(value) == "table") then
        return math.rnd(value)
    else
        return value
    end
end

function Particles:__asTable(value)
    if (type(value) == "table") then
        return value
    else
        return {value}
    end
end

function Particles:emit(number)
    for i = 0, number do
        table.insert(
                self.particles,
                new(
                        __Particle,
                        {
                            x = self:__get(self.x),
                            y = self:__get(self.y),
                            r = self:__get(self.radius),
                            sx = self:__get(self.sx),
                            sy = self:__get(self.sy),
                            sr = self:__get(self.sr),
                            ttl = self:__get(self.ttl)
                        }
                )
        )
    end
end

function Particles:__draw(particle)
    shape.circlef(particle.x, particle.y, particle.r, particle.color)
end

return function(options)
    return new(Particles, options)
end
