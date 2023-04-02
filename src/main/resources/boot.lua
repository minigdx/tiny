function _init(width, height)
    current = loading

    center = { x = width * 0.5, y = height * 0.5 }

    dots = {}
    for i=1,width do
        table.insert(dots, {x = i, y = center.y, c = i})
    end    
    loadingTime = 3

    dt = 0
    flash = 8

    txt = {"t", "i", "n", "y"}
end

function _update()
    dt = dt + 1/60
    loadingTime = loadingTime - 1/60
    current()
end

function _draw()
    cls(2)

    for k, d in pairs(dots) do
        pset(d.x, d.y + cos((k * 3 + dt) * 3) * 4, d.c)
    end

    local text = {"b", "u", "i", "l", "d", " ", "w", "i", "t", "h"}

    local prec = center.x
    center.x = center.x - 20

    for k,v in pairs(text) do
        print(v, center.x + k*4 + 1, center.y + cos(4 * dt + k) * 8, "#FF0000")
        print(v, center.x + k*4 - 1, center.y + cos(4 * dt + k) * 8, "#FF0000")
        print(v, center.x + k*4, center.y + cos(4 * dt + k) * 8 + 1, "#FF0000")
        print(v, center.x + k*4, center.y + cos(4 * dt + k) * 8 - 1, "#FF0000")

        print(v, center.x + k*4, center.y + cos(4 * dt + k) * 8, rnd(8))
    end

    center.x = prec

end

function _resources()
    -- current = loaded
end

function loading()
    
end

function loaded()
    flash = flash + 1
end    
