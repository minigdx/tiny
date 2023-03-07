function init()

    for x = 0, 256 do
        for y = 0, 256 do
         pset(x, y, 1)
        end
    end

    xx = 0
    yy = 0
    acc = 1
end


function update()
    xx = xx + 1
    yy = yy  + acc
    if yy >= 128 then acc = acc * -1 end
end

function draw()
    pset(xx, yy, xx % 2)

end
