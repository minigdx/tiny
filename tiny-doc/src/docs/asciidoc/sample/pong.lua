player = nil
bricks = {}
balls = {}

local draw_stencil = 2
local draw_in_stencil = 3
local draw_outside_stencil = 4

-- TRANSITIONS --
local Nope = {
    update = function()
    end,
    draw_start = function()
    end,
    draw_end = function()
    end
}


-- Transition when the game start.
-- It will erase the title screen using a growing circle
local GameOut = {
    progress = 0,
    speed = 5,
    start = false,
    y = 0
}

GameOut.update = function(self)
    if (self.start) then
        self.progress = self.progress + self.speed

        if (self.progress > 300) then
            transition = new(Nope)
        end
        self.y = juice.powIn2(self.y, -200, self.progress / 500)
    end
end

GameOut.draw_start = function(self)
end

GameOut.draw_end = function(self)
    gfx.draw_mode(draw_stencil)
    shape.circlef(128, 128, self.progress, 0)
    gfx.draw_mode(draw_outside_stencil)

    local y = 0 -- self.y

    -- title
    spr.sdraw(0, 100 + y, 0, 208, 256, 3 * 16)
    -- space
    spr.sdraw(80, 150 + y, 0, 128, 3 * 16, 16)
    -- left and right
    spr.sdraw(88, 150 + 16 + y, 4 * 16, 128, 16, 16)
    spr.sdraw(88 + 16, 150 + 16 + y, 3 * 16, 128, 16, 16)
    print("launch the ball", 80 + 3 * 16, 154 + y, 2)
    print("move the paddle", 80 + 3 * 16, 154 + 16 + y, 2)

    -- go back to normal drawing mode
    gfx.draw_mode()
end

local EndOut = {
    start_radius = 40,
    radius = 0,
    target_radius = 300,
    duration = 1,
    t = 0
}

EndOut.update = function(self)
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

EndOut.draw_start = function(self)
    --  gfx.cls(1)
    --  shape.circlef(256 * 0.5, 212, self.radius, 0)
    --  spr.sheet(0)
    --  spr.sdraw(0, 100, 0, 208, 256, 3 * 16)

    --  gfx.to_sheet(2)
end

EndOut.draw_end = function(self)
end

-- Transition when the player just lost.
-- Will draw a circle that will close on the player position
local EndIn = {
    start_radius = 256,
    radius = 256,
    target_radius = 40,
    duration = 1,
    t = 0
}

EndIn.update = function(self)
    self.t = self.t + tiny.dt
    self.radius = juice.powOut5(self.start_radius, self.target_radius, self.t / self.duration)

    if (self.radius <= 40) then
        transition = new(EndOut, { start_radius = self.radius })
    end
end

EndIn.draw_start = function(self)
    gfx.cls(1)
    gfx.draw_mode(draw_stencil)
    shape.circlef(256 * 0.5, 212, 70, 1)
    gfx.draw_mode(draw_in_stencil)
    -- simulate a gfx.cls(13)...
    shape.rectf(0, 0, 256, 256, 13)
end

EndIn.draw_end = function(self)
    gfx.draw_mode()
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

Particle.update = function(self)
    self.x = self.x + self.dir.x
    self.y = self.y + self.dir.y
    self.radius = self.radius + self.dir.r
    self.ttl = self.ttl - 1 / 60
    return self.ttl < 0
end

Particle.draw = function(self)
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

Brick.update = function(self)
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

Ball.reset = function(self)
    local r = player

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

Ball.update = function(self)
    self.accept_move_x = true
    self.accept_move_y = true

    if (ctrl.touched(0) or ctrl.pressed(keys.space)) then
        -- release the ball
        self.glue_to = false
    end

    if self.glue_to then
        local r = player
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
        if self.new_y >= player.y then
            local collision = check_collision(
                    { x = player.x, y = player.y, width = 32, height = 8 },
                    { x = self.new_x, y = self.new_y, width = self.width, height = self.height }
            )
            if collision then
                self.speed.y = -self.speed.y
                self.accept_move_y = false
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

Ball.valid_move = function(self)
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

local Paddle = {
    x = 0,
    y = 0,
    height = 8,
    width = 24
}
local Player = {
    x = 128 - 16,
    y = 216,
    width = 24,
    height = 8,
    color = 8,
    speed = 6,
    direction = 1,
    paddles = {}
}

Player.createPaddle = function(self, y)
    table.insert(self.paddles, new(Paddle, { x = self.x, y = y }))
end

Player.update = function(self)
    local touching = ctrl.touching(0)
    local touch = ctrl.touch()
    local mleft = touching and touch.x < (self.x + self.width * 0.5)
    local mright = touching and touch.x >= (self.x + self.width * 0.5)

    if ctrl.pressing(keys.left) or mleft then
        self.x = math.max(0, self.x - self.speed)
        self.direction = 0
    elseif ctrl.pressing(keys.right) or mright then
        self.x = math.min(self.x + self.speed, 256 - self.width)
        self.direction = 1
    end
    self.y = 216

    self.paddles[1].x = self.x
    self.paddles[1].y = self.y

    self.paddles[2].x = self.x

    for i = 3, #self.paddles do
        local prev = self.paddles[i - 1]
        local current = self.paddles[i]

        current.x = juice.linear(current.x, prev.x, 0.2)
    end
end

Player.draw = function(self)
    -- head
    shape.rectf(player.x + player.width, player.y + 3, 1, player.height - 3, 2)
    spr.sdraw(player.x, player.y, 0, 32, player.width, player.height, player.direction == 1)

    for i = 2, (#self.paddles - 1) do
        local current = self.paddles[i]
        local next = self.paddles[i + 1]

        for h = 0, current.height do
            local x = juice.pow2(current.x, next.x, h / current.height)
            local y = current.y + h
            spr.sdraw(x, y, 0, 32 + (i - 1) * 8 + h, current.width + 1, 1)
        end
    end
end

function _init()
    transition = new(GameOut)

    game = {
        radius_title = 0,
        started = false,
        lost = false,
        cooldown = 0
    }

    player = new(Player)
    for y = 216, 256, 8 do
        player:createPaddle(y)
    end

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

    player:update()

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

local test = false

function _draw()
    if ctrl.pressed(keys.space) then
        test = not test
    end

    gfx.cls(4)
    gfx.draw_mode(draw_stencil)
    -- draw a circle in the midle of the screen
    shape.circlef(128, 128, 128, 1)

    if test then
        gfx.draw_mode(draw_in_stencil)
    else
        gfx.draw_mode(draw_outside_stencil)
    end

    -- draw the sprite sheet. only in the circle
    for i = 0, 256, 16 do
        for j = 0, 256, 16 do
            spr.draw(0, i, j)
        end
    end
    gfx.draw_mode(1) -- erase
    shape.circlef(32, 32, 64, 8)
    gfx.draw_mode()

end

function _draw2()
    -- game
    gfx.cls(13)
    spr.sheet()

    transition:draw_start()
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

    player:draw()

    transition:draw_end()
end
