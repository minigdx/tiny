function init()
    boom = {}
    dt = 0
end

function update()
    dt = dt + 1/60
    if(dt > 0.5) then
        table.insert(boom, {x=rnd(128), y=rnd(128), size=1, color=rnd(15)})
        dt = dt - 0.5
    end
    for k,v in pairs(boom) do
        v.size = v.size + 0.5
        if v.size > 128 then
            table.remove(boom, k)
        end
    end
end

function draw()
    cls(3)
    for k,i in pairs(boom) do
        circlef(i.x, i.y, i.size, i.color)
        --print(cos())
    end
 end
