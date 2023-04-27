-- == master plan ==
-- afficher les briques
-- afficher la balle
-- faire une raquette avec shape.rectf
-- bouger la raquette avec les touches

function _init()

    raquettes = {
        {
            x = 128 - 16, -- center the raquette
            y = 232,
            width = 32,
            height = 4,
            color = 8,
            speed = 6
        }
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
    if ctrl.down(0) then
        raquette.x = math.max(0, raquette.x - raquette.speed)
    elseif ctrl.down(2) then
        raquette.x = math.min(raquette.x + raquette.speed, 256 - raquette.width)
    end
end

function update_raquettes()
    for index, r in rpairs(raquettes) do
        local to_delete = update_raquette(r)
        if to_delete then
            table.remove(raquettes, index)
        end
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

    update_raquettes()

    for index, b in rpairs(balls) do
        b.accept_move_x = true
        b.accept_move_y = true

        if ctrl.down(1) then
            b.glue_to = false
        end
        if b.glue_to then
            local r = raquettes[1]
            b.new_x = r.x + r.width * 0.5 - 7 * 0.5
            b.new_y = r.y - 7

            if ctrl.down(0) then
                b.speed.x = -1
            elseif ctrl.down(2) then
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

    for b in all(balls) do
        if b.accept_move_x then
            b.x = b.new_x
        end
        if b.accept_move_y then
            b.y = b.new_y
        end

        if not b.accept_move_x or not b.accept_move_y then
            for i=1,3 do
                table.insert(boobles, boobles_create(b.x + math.rnd(-2, 2), b.y + math.rnd(-2, 2), 3, 0, 1, math.rnd({8, 7, 14}), true))
            end
        end
        if b.y > 256 then
            _init() -- restart the game
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
    gfx.cls(13)

    for b in all(bricks) do
        spr.sdraw(b.x, b.y, 0, b.color * 8, 16, 8)
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

    for r in all(raquettes) do
        -- shadow
        shape.rectf(r.x + 1, r.y + 1, r.width, r.height, 2)
        -- raquette
        shape.rectf(r.x, r.y, r.width, r.height, r.color)
    end
end
