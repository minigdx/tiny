-- 3 layered waves with bottom-to-top transition and mouse interaction

local waves = {}
local width, height
local transition_start = 0
local transition_duration = 1.5 -- seconds for all waves to fully appear

function _init(w, h)
    width = w
    height = h
    transition_start = tiny.t

    -- Define 3 waves (back to front)
    -- Colors: 0=darkest blue ... 7=white
    waves = {
        {
            base_y = h * 0.45,
            amplitude = 40,
            speed = 0.05,
            color = 5,
            layer = 1,
            delay = 0.0,
        },
        {
            base_y = h * 0.58,
            amplitude = 50,
            speed = 0.07,
            color = 3,
            layer = 2,
            delay = 0.3,
        },
        {
            base_y = h * 0.72,
            amplitude = 30,
            speed = 0.1,
            color = 1,
            layer = 3,
            delay = 0.6,
        },
    }
end

-- Ease out cubic
local function ease_out(t)
    local inv = 1 - t
    return 1 - inv * inv * inv
end

local mouse_radius = 120
local mouse_strength = 20
local step = 8 * 4
local circle_r = 48

function _update()
end

-- Compute wave Y at a given x position
local function wave_y_at(w, x, mx, my, offset_y, now)
    local wy = w.base_y
        + math.perlin(x / width, 0.5 * w.layer / 3, now * w.speed) * w.amplitude

    -- Mouse interaction: push wave up near cursor
    local dx = x - mx
    local dy = (wy + offset_y) - my
    local dist = math.sqrt(dx * dx + dy * dy)
    if dist < mouse_radius and dist > 0.1 then
        local factor = (mouse_radius - dist) / mouse_radius
        wy = wy - factor * factor * mouse_strength
    end

    return wy + offset_y
end

function _draw()
    gfx.cls(7)

    local touch = ctrl.touch()
    local mx = touch.x
    local my = touch.y
    local now = tiny.t

    for i = 1, #waves do
        local w = waves[i]

        -- Per-wave transition progress (staggered)
        local elapsed = now - transition_start - w.delay
        local progress = math.max(0, math.min(elapsed / transition_duration, 1))
        local ease = ease_out(progress)

        -- Offset: wave starts below screen, slides up
        local offset_y = (1 - ease) * height

        -- First pass: sample wave crest points, find peak (min y on screen)
        local points = {}
        local min_y = height
        local x = -circle_r
        while x <= width + circle_r do
            local fy = wave_y_at(w, x, mx, my, offset_y, now)
            points[#points + 1] = { x = x, y = fy }
            if fy < min_y then min_y = fy end
            x = x + step
        end

        -- Body: one big rectangle from just below the peak circles to bottom
        local body_top = min_y + circle_r
        if body_top < height then
            shape.rectf(0, body_top, width + 1, height - body_top + 1, w.color)
        end

        -- Tip: consecutive circles along the wave crest
        for j = 1, #points do
            local p = points[j]
            shape.circlef(p.x, p.y, circle_r, w.color)
        end
    end
end
