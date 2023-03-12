function init()
    x = 10
end
function draw()
    cls(2)
    pset(0, 0, 7)
    pset(128, 128, 7)
    pset(0, 128, 7)
    pset(128, 0, 7)

    -- center
    pset(64, 64, 7)
    pset(1, 1, 7)
    pset(124, 124, 7)
    -- bord gauche
    pset(2, 64, 9)
    pset(2, 66, 9)
    pset(2, 68, 9)
    pset(2, 69, 9)
    pset(2, 70, 9)

    pset(2, 72, 9)
    pset(2, 73, 9)
    pset(2, 74, 9)
    pset(2, 75, 9)  pset(3, 75, 9) pset(4, 75, 9)

    -- bord droit

    pset(124, 64, 10)
    pset(124, 66, 10)
    pset(124, 68, 10)
    pset(124, 69, 10)
    pset(124, 70, 10)

    pset(124, 72, 10) pset(125, 72, 10)
    pset(124, 73, 10)                   pset(126, 73, 10)
    pset(124, 74, 10) pset(125, 74, 10)
    pset(124, 75, 10)                    pset(126, 75, 10)


    x = (x + 1) % 127
    circle(x, 64, 4, 9)
end
