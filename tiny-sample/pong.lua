local Nope = {
    update = function()
    end,
    draw = function()
    end
}


local GameOut = {
    radius = 0,
    speed = 5,
    start = false
}

function GameOut:new()
    local n = {}
    setmetatable(n, self)
    self.__index = self
    return n
end


function Nope:new()
    local n = {}
    setmetatable(n, self)
    self.__index = self
    return n
end

local EndOut = {
    start_radius = 40,
    radius = 0,
    target_radius = 300,
    duration = 1,
    t = 0
}

function EndOut:new()
    local n = {}
    setmetatable(n, self)
    self.__index = self
    return n
end

function EndOut:update()
    self.t = self.t + 1 / 60
    self.radius = juice.powIn5(self.start_radius, self.target_radius, self.t / self.duration)

    if (self.radius >= 300) then
        transition = GameOut:new()
    end
end

function EndOut:draw()
    gfx.cls(1)
    shape.circlef(256 * 0.5, 212, self.radius, 0)
    spr.sheet(0)
    spr.sdraw(0, 100, 0, 208, 256, 3 * 16)

    gfx.to_sheet(2)
end

local EndIn = {
    start_radius = 256,
    radius = 256,
    target_radius = 40,
    duration = 1,
    t = 0
}

function EndIn:new()
    local n = {}
    setmetatable(n, self)
    self.__index = self
    return n
end

function EndIn:update()
    self.t = self.t + 1 / 60
    self.radius = juice.powOut5(self.start_radius, self.target_radius, self.t / self.duration)

    if (self.radius <= 40) then
        transition = EndOut:new()
    end
end

function EndIn:draw()
    gfx.cls(1)

    shape.circlef(256 * 0.5, 212, self.radius, 0)
    gfx.to_sheet(2)
end


function GameOut:update()
    if (self.start) then
        self.radius = self.radius + self.speed

        if (self.radius > 300) then
            transition = Nope:new()
        end
    end
end

function GameOut:draw()
    gfx.cls(0)
    spr.sheet(0)
    spr.sdraw(0, 100, 0, 208, 256, 3 * 16)
    shape.circlef(256 * 0.5, 212, self.radius, 0)
    gfx.to_sheet(2)
end

function _init()
    transition = GameOut:new()

    game = {
        radius_title = 0,
        started = false,
        lost = false,
        cooldown = 0
    }

    dt = 1 / 60
    longueurCode = 100
    longueurSegment = 10
    numSegments = longueurCode / longueurSegment
    gravite = 29.8
    rigidite = 1 -- 0.8
    amortissement = 0.9

    raquettes = {
        create_raquette(216),
        create_raquette(224),
        create_raquette(232),
        create_raquette(240),
        create_raquette(248),
        create_raquette(256),
    }

    local r = raquettes[1]

    balls = {
        {
            x = r.x + r.width * 0.5 - 7 * 0.5,
            y = r.y - 7,
            width = 7,
            height = 7,
            speed = { x = 3, y = -3 },
            glue_to = true,
            new_x = r.x + r.width * 0.5 - 7 * 0.5,
            new_y = r.y - 7,
            accept_move_x = true,
            accept_move_y = true,
        }
    }

    bricks = {}
    for y = 1, 6 do
        for x = 1, 14 do
            table.insert(bricks, {
                x = x * 16,
                yy = y * 8,
                y = y * 8,
                color = math.rnd(2),
                offset = -4,
                progress = x * -0.2 + y * -0.08,
                hit = nil,
            })
        end
    end

    particles = {}
    boobles = {}
end

function create_raquette(y)
    return {
        x = 128 - 16, -- center the raquette
        y = y,
        prevX = 128 - 16,
        prevY = y,
        width = 24,
        height = 8,
        color = 8,
        speed = 6,
        direction = 1,
    }
end

function boobles_create(x, y, radius, target_radius, ttl, color, filled)
    return {
        x = x,
        y = y,
        radius = radius,
        target_radius = target_radius,
        ttl = ttl,
        t = 0,
        color = color,
        filled = filled
    }
end

function boobles_update(booble)
    booble.t = booble.t + 1 / 60
    if booble.t >= booble.ttl then
        return true
    end

    booble.y = booble.y + 0.3
    return false
end

function update_raquette(raquette)
    if ctrl.pressing(keys.left) then
        raquette.x = math.max(0, raquette.x - raquette.speed)
        raquette.direction = 0
    elseif ctrl.pressing(keys.right) then
        raquette.x = math.min(raquette.x + raquette.speed, 256 - raquette.width)
        raquette.direction = 1
    end
    raquette.y = 216
end

function update_raquettes()
    update_raquette(raquettes[1])

    for i = 2, #raquettes do
        local segment = raquettes[i]
        local segmentPrecedent = raquettes[i - 1]

        segment.y = segment.y + gravite * dt

        -- integration de verlet
        local vx = segment.x - segment.prevX
        local vy = segment.y - segment.prevY

        segment.prevX = segment.x
        segment.prevY = segment.y

        segment.x = segment.x + vx * amortissement
        segment.y = segment.y + vy * amortissement

        -- contraintes
        local dx = segment.x - segmentPrecedent.x
        local dy = segment.y - segmentPrecedent.y

        local distance = math.sqrt(dx * dx + dy * dy)
        local difference = longueurSegment - distance
        local pourcentage = difference / distance / 2

        local decalageX = dx * pourcentage * rigidite
        local decalageY = dy * pourcentage * rigidite

        segment.x = segment.x + decalageX
        segment.y = segment.y + decalageY

        segmentPrecedent.x = segmentPrecedent.x - decalageX
        segmentPrecedent.y = segmentPrecedent.y - decalageY
    end

    for i = 2, #raquettes - 1 do
        local segment = raquettes[i]
        local segmentNext = raquettes[i + 1]

        segment.height = math.abs(segment.y - segmentNext.y)

    end
end
function check_collision(rect1, rect2)
    local rect1Right = rect1.x + rect1.width
    local rect1Bottom = rect1.y + rect1.height
    local rect2Right = rect2.x + rect2.width
    local rect2Bottom = rect2.y + rect2.height

    -- Check if rectangles overlap
    if rect1.x < rect2Right and rect1Right > rect2.x and rect1.y < rect2Bottom and rect1Bottom > rect2.y then
        -- Calculate overlap on each side
        local overlapLeft = rect2Right - rect1.x
        local overlapRight = rect1Right - rect2.x
        local overlapTop = rect2Bottom - rect1.y
        local overlapBottom = rect1Bottom - rect2.y

        -- Determine which side was hit based on the smallest overlap
        local smallestOverlap = math.min(overlapLeft, overlapRight, overlapTop, overlapBottom)

        if smallestOverlap == overlapLeft then
            return "left"
        elseif smallestOverlap == overlapRight then
            return "right"
        elseif smallestOverlap == overlapTop then
            return "top"
        elseif smallestOverlap == overlapBottom then
            return "bottom"
        end
    end

    return nil  -- No collision
end

function build_particle(x, y, tx, ty)
    return { x = x, y = y, tx = tx, ty = ty, ttl = 0.2 }
end

function _update()
    if (ctrl.pressing(keys.up) and not game.started) then
        game.started = true
        transition.start = true
    end

    transition:update()

    update_raquettes()

    for index, b in rpairs(balls) do
        b.accept_move_x = true
        b.accept_move_y = true

        if ctrl.pressing(keys.up) then
            b.glue_to = false
        end
        if b.glue_to then
            local r = raquettes[1]
            b.new_x = r.x + r.width * 0.5 - 7 * 0.5
            b.new_y = r.y - 7

            if ctrl.pressing(keys.left) then
                b.speed.x = -1
            elseif ctrl.pressing(keys.right) then
                b.speed.x = 1
            end
        else
            b.new_x = b.x + b.speed.x
            b.new_y = b.y + b.speed.y

            -- hit walls?
            if b.new_x > 256 then
                b.speed.x = -b.speed.x
                b.accept_move_x = false
            elseif b.new_x < 0 then
                b.speed.x = -b.speed.x
                b.accept_move_x = false
            end

            if b.new_y < 0 then
                b.speed.y = -b.speed.y
                b.accept_move_y = false
            end

            -- hit raquettes ?
            if b.new_y >= 220 then
                for r in all(raquettes) do
                    -- raquette collision
                    local collision = check_collision(
                            { x = r.x, y = r.y, width = 32, height = 8 },
                            { x = b.new_x, y = b.new_y, width = b.width, height = b.height }
                    )
                    if collision then
                        b.speed.y = -b.speed.y
                        b.accept_move_y = false
                    end

                end
            end
            local p = build_particle(b.x + 3, b.y + 3, b.x + 3 + b.speed.x, b.y + 3 + b.speed.y)
            table.insert(particles, p)
        end
    end

    for index, b in rpairs(bricks) do
        b.progress = b.progress + 1 / 20
        b.y = juice.pow2(b.yy - 20, b.yy, math.min(1.0, b.progress))

        if b.hit then
            b.hit = b.hit - 1
            if b.hit <= 0 then
                table.remove(bricks, index)
            end
        else
            for ball in all(balls) do
                local collisionX = check_collision(
                        { x = b.x, y = b.y, width = 16, height = 8 },
                        { x = ball.new_x, y = ball.y, width = ball.width, height = ball.height }
                )
                local collisionY = check_collision(
                        { x = b.x, y = b.y, width = 16, height = 8 },
                        { x = ball.x, y = ball.new_y, width = ball.width, height = ball.height }
                )
                if collisionX then
                    ball.accept_move_x = false
                    ball.speed.x = ball.speed.x * -1
                end

                if collisionY then
                    ball.accept_move_y = false
                    ball.speed.y = ball.speed.y * -1
                end

                if collisionX or collisionY then
                    b.hit = 6
                end
            end
        end
    end

    for index, b in rpairs(balls) do
        if b.accept_move_x then
            b.x = b.new_x
        end
        if b.accept_move_y then
            b.y = b.new_y
        end

        if not b.accept_move_x or not b.accept_move_y then
            for i = 1, 3 do
                table.insert(boobles, boobles_create(b.x + math.rnd(-2, 2), b.y + math.rnd(-2, 2), 3, 0, 1, math.rnd({ 8, 7, 14 }), true))
            end
        end
        if b.y > 256 then
            game.lost = true
            transition = EndIn:new()
            table.remove(balls, index)
        end
    end

    for index, p in rpairs(particles) do
        p.ttl = p.ttl - 1 / 60
        if p.ttl < 0 then
            table.remove(particles, index)
        end
    end

    for index, b in rpairs(boobles) do
        if boobles_update(b) then
            table.remove(boobles, index)
        end
    end

end

function _draw()
    transition:draw()

    -- game
    gfx.cls(13)
    spr.sheet()

    for b in all(bricks) do
        spr.sdraw(b.x, b.y, 16, b.color * 8, 16, 8)
        if b.hit then
            shape.rectf(b.x, b.y, 16, 8, 8 + b.hit)
        end
    end

    for p in all(particles) do
        -- gfx.pset(p.x, p.y, 8)
        shape.line(p.x, p.y, p.tx, p.ty, 8)
    end

    for b in all(boobles) do
        if b.filled then
            shape.circlef(b.x, b.y, juice.pow2(b.radius, b.target_radius, b.t / b.ttl), b.color)
        end
    end

    for b in all(balls) do
        if b.glue_to then
            local x = math.sign(b.speed.x) * 8
            local y = math.sign(b.speed.y) * 8

            local centerX = b.x + 3
            local centerY = b.y + 2
            shape.line(centerX, centerY, centerX + x, centerY + y, 11)
        end
        spr.sdraw(b.x, b.y, 0, 16, 8, 8)
    end

    for i = 1, #raquettes - 1 do
        local r = raquettes[i]
        local next_r = raquettes[i + 1]

        -- todo: add arms??
        if i == 1 then
            shape.rectf(r.x + r.width, r.y + 3, 1, r.height - 3, 2)
            spr.sdraw(r.x, r.y, 0, 32, r.width, r.height, r.direction == 1)
        else
            shape.rectf(r.x, r.y, r.width, math.ceil(r.height), 10)
        end
    end

    spr.sheet(2)
    spr.sdraw()
end
