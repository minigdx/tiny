local clouds = {}
local width = 480
local height = 128
local repulsion_radius = 60

function _init()
    -- Generate clouds in a grid pattern with random jitter
    local cols = 10
    local rows = 4
    local cell_w = width / cols
    local cell_h = height / rows

    for row = 0, rows - 1 do
        for col = 0, cols - 1 do
            local cx = col * cell_w + cell_w / 2 + math.rnd(-cell_w * 0.3, cell_w * 0.3)
            local cy = row * cell_h + cell_h / 2 + math.rnd(-cell_h * 0.3, cell_h * 0.3)

            -- Each cloud has 3-7 overlapping circle "puffs"
            local puff_count = math.floor(math.rnd(3, 8))
            local puffs = {}
            for i = 1, puff_count do
                puffs[i] = {
                    ox = math.rnd(-12, 12),
                    oy = math.rnd(-6, 6),
                    r = math.rnd(6, 14),
                }
            end

            clouds[#clouds + 1] = {
                x = cx,
                y = cy,
                home_x = cx,
                home_y = cy,
                vx = 0,
                vy = 0,
                puffs = puffs,
            }
        end
    end
end

function _update()
    local touch = ctrl.touch(0)
    local mx = touch.x
    local my = touch.y

    for i = 1, #clouds do
        local c = clouds[i]

        -- Calculate distance to mouse
        local dx = c.x - mx
        local dy = c.y - my
        local dist = math.sqrt(dx * dx + dy * dy)

        -- Apply repulsion force if within radius
        if dist < repulsion_radius and dist > 0.1 then
            local factor = ((repulsion_radius - dist) / repulsion_radius)
            local force = factor * factor * 3.0
            c.vx = c.vx + (dx / dist) * force
            c.vy = c.vy + (dy / dist) * force
        end

        -- Spring force back toward home position
        c.vx = c.vx + 0.01 * (c.home_x - c.x)
        c.vy = c.vy + 0.01 * (c.home_y - c.y)

        -- Damping
        c.vx = c.vx * 0.92
        c.vy = c.vy * 0.92

        -- Update position
        c.x = c.x + c.vx
        c.y = c.y + c.vy

        -- Clamp within boundaries (with small margin)
        c.x = math.clamp(-20, c.x, width + 20)
        c.y = math.clamp(-20, c.y, height + 20)
    end
end

function _draw()
    -- Clear with sky blue (palette index 0)
    gfx.cls(0)

    -- Draw each cloud's puffs as white filled circles
    for i = 1, #clouds do
        local c = clouds[i]
        for j = 1, #c.puffs do
            local p = c.puffs[j]
            shape.circlef(c.x + p.ox, c.y + p.oy, p.r, 7)
        end
    end
end
