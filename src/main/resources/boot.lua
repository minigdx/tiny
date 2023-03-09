function init()
    boom = {}
    dt = 0
    x = -5
    acc = 1
    startColor = {9, 10, 11}
    animationColor = {
        1, 1, 2, 8,
        11, 14, 6, 7,
        10, 11, 8, 1,
        1, 3, 7, 2,
    }
end

function update()
    x = x + acc
    dt = dt + 1/60
    -- x = 64
    if(dt > 0.05) then
        c = rnd(startColor)
        table.insert(boom, {x=x, y=64 + rnd(-2, 2), size=6, color=9, ttl=1})
        dt = dt - 0.05
    end
    if x > 128 then
        acc = -1
    elseif x < 0 then
        acc = 1
    end
    for k,v in pairs(boom) do
        v.size = max(v.size - 0.2, 0)
        v.ttl = v.ttl - 1/60
        -- v.color = animationColor[v.color]
        if v.ttl <= 0 then
            table.remove(boom, k)
        end
    end
end

function draw()
    cls(3)
    for k,i in pairs(boom) do
        circlef(i.x, i.y, i.size, i.color)
    end
 end
