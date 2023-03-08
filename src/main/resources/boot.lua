function init()
    index = 0
    -- palette = {0, 13, 2, 4, 8, 9, 15}
    palette = {
        0, 5, 13, 11,
        8, 13, 7, 4,
        9, 10, 7, 7,
        7, 4,  15, 7
    }
    cls()
    cls()
    for x = 0, 126 do
        pset(x, 0, 2)
    end
end

function draw()
    color = 7
    for i = 0, 56 do
            x = rnd(126)
            y = rnd(26)
            p = pget(x, y)
            -- print(palette[p + 1])
            pset(x, y, palette[p + 1])
            pset(x + rnd(1), y + 1, p)
    end

    -- T
    pset(100, 100, 0)
    pset(101, 100, 0)
    pset(102, 100, 0)
    pset(101, 101, 0)
    pset(101, 102, 0)

    -- I
    pset(104, 100, 0)
    pset(104, 101, 0)
    pset(104, 102, 0)

    -- N
    pset(106, 100, 0)
    pset(106, 101, 0)
    pset(106, 102, 0)
    pset(107, 101, 0)
    pset(108, 102, 0)
    pset(108, 101, 0)
    pset(108, 100, 0)

    -- Y
    pset(110, 100, 0)
    pset(110, 101, 0)
    pset(111, 101, 0)
    pset(112, 101, 0)
    pset(112, 100, 0)
    pset(111, 102, 0)


end
