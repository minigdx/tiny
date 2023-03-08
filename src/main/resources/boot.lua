function init()
    index = 0
    draw()
end


function update()
    index = (index + 1) % 3
end

function draw()
    for x = 0, 256 do
        for y = 0, 256 do
            pset(x, y, index)
        end
    end

end
