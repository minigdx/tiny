-- TRANSITIONS --
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

local EndOut = {
    start_radius = 40,
    radius = 0,
    target_radius = 300,
    duration = 1,
    t = 0
}

function EndOut:update()
    self.t = self.t + 1 / 60
    self.radius = juice.powIn5(self.start_radius, self.target_radius, self.t / self.duration)

    if (self.radius >= 300) then
        for b in all(balls) do
            b:reset()
        end
        transition = new(GameOut)
        game.started = false
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

function EndIn:update()
    self.t = self.t + 1 / 60
    self.radius = juice.powOut5(self.start_radius, self.target_radius, self.t / self.duration)

    if (self.radius <= 40) then
        transition = new(EndOut)
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
            transition = new(Nope)
        end
    end
end

function GameOut:draw()
    gfx.cls(0)
    spr.sheet(0)
    -- title
    spr.sdraw(0, 100, 0, 208, 256, 3 * 16)
    -- space
    spr.sdraw(80, 150, 0, 128, 3 * 16, 16)
    -- left and right
    spr.sdraw(88, 150 + 16, 4 * 16, 128, 16, 16)
    spr.sdraw(88 + 16, 150 + 16, 3 * 16, 128, 16, 16)
    print("launch the ball", 80 + 3 * 16, 154, 2)
    print("move the paddle", 80 + 3 * 16, 154 + 16, 2)

    shape.circlef(256 * 0.5, 212, self.radius, 0)
    gfx.to_sheet(2)
end

-- PARTICLES --
local Particle = {
    x = 0,
    y = 0,
    dir = { x = 0, y = 0, r = 0 },
    radius = 1,
    ttl = 0.5,
    color = 1
}

function Particle:update()
    self.x = self.x + self.dir.x
    self.y = self.y + self.dir.y
    self.radius = self.radius + self.dir.r
    self.ttl = self.ttl - 1 / 60
    return self.ttl < 0
end

function Particle:draw()
    shape.circlef(self.x, self.y, self.radius, self.color)
end

-- Bricks
local Brick = {
    -- position
    x = 0,
    y = 0,
    start_y = 0,
    -- size
    width = 16,
    height = 8,
    -- sprite
    color = 0,
    hit = nil,
    offset = -4,
    progress = 0,
}

function Brick:update()
    self.progress = self.progress + 1 / 20
    self.y = juice.pow2(self.start_y - 20, self.start_y, math.min(1.0, self.progress))

    if self.hit then
        self.hit = self.hit - 1
        return self.hit <= 0 -- is the brick should be destroyed?
    end
    for ball in all(balls) do
        local collisionX = check_collision(
                { x = self.x, y = self.y, width = 16, height = 8 },
                { x = ball.new_x, y = ball.y, width = ball.width, height = ball.height }
        )
        local collisionY = check_collision(
                { x = self.x, y = self.y, width = 16, height = 8 },
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
            self.hit = 6

            table.insert(particles, new(Particle,
                    {
                        x = self.x,
                        y = self.y,
                        dir = { x = 0, y = -0.2, r = 0 },
                        draw = function(self)
                            local c = 2
                            print("+1", self.x + 1, self.y, c)
                            print("+1", self.x - 1, self.y, c)
                            print("+1", self.x, self.y + 1, c)
                            print("+1", self.x, self.y - 1, c)
                            print("+1", self.x, self.y, math.rnd({ 8, 7, 14 }))
                        end
                    }
            ))
        end
    end
    return false
end

local Ball = {
    x = 0,
    y = 0,
    width = 7,
    height = 7,
    speed = { x = 3, y = -3 },
    glue_to = true,
    new_x = 0,
    new_y = 0,
    accept_move_x = true,
    accept_move_y = true,
}

function Ball:reset()
    local r = raquettes[1]

    self.x = r.x + r.width * 0.5 - 7 * 0.5
    self.y = r.y - 7
    self.speed = { x = 3, y = -3 }
    self.new_x = r.x + r.width * 0.5 - 7 * 0.5
    self.new_y = r.y - 7
    self.accept_move_x = true
    self.accept_move_y = true
    self.glue_to = true

    return self
end

function Ball:update()
    self.accept_move_x = true
    self.accept_move_y = true

    if (ctrl.touched(0) or ctrl.pressed(keys.space)) then
        -- release the ball
        self.glue_to = false
    end

    if self.glue_to then
        local r = raquettes[1]
        self.new_x = r.x + r.width * 0.5 - 7 * 0.5
        self.new_y = r.y - 7

        if ctrl.pressing(keys.left) then
            self.speed.x = -1
        elseif ctrl.pressing(keys.right) then
            self.speed.x = 1
        end

        local touch = ctrl.touching(0)
        if touch then
            if touch.x < (r.x + r.width * 0.5) then
                self.speed.x = -1
            else
                self.speed.x = 1
            end
        end
    else
        self.new_x = self.x + self.speed.x
        self.new_y = self.y + self.speed.y

        -- hit walls?
        if self.new_x > 256 then
            self.speed.x = -self.speed.x
            self.accept_move_x = false
        elseif self.new_x < 0 then
            self.speed.x = -self.speed.x
            self.accept_move_x = false
        end

        if self.new_y < 0 then
            self.speed.y = -self.speed.y
            self.accept_move_y = false
        end

        -- hit paddles ?
        if self.new_y >= raquettes[1].y then
            for r in all(raquettes) do
                -- raquette collision
                local collision = check_collision(
                        { x = r.x, y = r.y, width = 32, height = 8 },
                        { x = self.new_x, y = self.new_y, width = self.width, height = self.height }
                )
                if collision then
                    self.speed.y = -self.speed.y
                    self.accept_move_y = false
                end

            end
        end

        table.insert(particles, new(Particle, {
            x = self.x + 3 + math.rnd(-2, 2),
            y = self.y + 3 + math.rnd(-2, 2),
            ttl = 0.4,
            dir = { x = 0, y = 0, r = -0.3 },
            radius = 4,
            color = 8
        }))
    end
end

function Ball:valid_move()
    if self.accept_move_x then
        self.x = self.new_x
    end
    if self.accept_move_y then
        self.y = self.new_y
    end

    if not self.accept_move_x or not self.accept_move_y then
        for i = 1, 3 do
            table.insert(particles, new(Particle, {
                x = self.x + 3 + math.rnd(-1, 1),
                y = self.y + 3 + math.rnd(-1, 1),
                ttl = 1.5,
                dir = { x = -self.speed.x * 0.1, y = -self.speed.y * 0.1, r = -0.1 },
                radius = 5,
                color = math.rnd({ 8, 7, 14 })
            }))
        end
    end
    return self.y > 256
end

local player = nil

function _init()
    transition = new(GameOut)

    game = {
        radius_title = 0,
        started = false,
        lost = false,
        cooldown = 0
    }

    player = new(Player)
    for y=216,248,8 do
        player:createPaddle(y)
    end

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
        new(Ball):reset()
    }

    bricks = {}
    for y = 1, 6 do
        for x = 1, 14 do
            table.insert(bricks, new(Brick, {
                x = x * 16,
                y = y * 8,
                start_y = y * 8,
                color = math.rnd(2),
                progress = x * -0.2 + y * -0.08,
            }))
        end
    end

    particles = {}
end

-- FIXME: replace with class and use the term paddle
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

function update_raquette(raquette)
    local touching = ctrl.touching(0)
    local touch = ctrl.touch()
    local mleft = touching and touch.x < (raquette.x + raquette.width * 0.5)
    local mright = touching and touch.x >= (raquette.x + raquette.width * 0.5)

    if ctrl.pressing(keys.left) or mleft then
        raquette.x = math.max(0, raquette.x - raquette.speed)
        raquette.direction = 0
    elseif ctrl.pressing(keys.right) or mright then
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

function _update()
    if ((ctrl.touched(0) or ctrl.pressed(keys.space)) and not game.started) then
        game.started = true
        transition.start = true
    end

    transition:update()

    update_raquettes()

    for index, b in rpairs(balls) do
        b:update()
    end

    for index, b in rpairs(bricks) do
        if b:update() then
            table.remove(bricks, index)
        end
    end

    for index, b in rpairs(balls) do
        if b:valid_move() then
            b:reset()
            game.lost = true
            transition = new(EndIn)
        end
    end

    for index, p in rpairs(particles) do
        if p:update() then
            table.remove(particles, index)
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
        p:draw()
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
