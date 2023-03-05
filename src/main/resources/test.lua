function setState(state)
    timer = state
end

function getState()
    return timer
end
function init()
    timer = 0
end

function update()
    timer = timer + 10
    -- print("it's time to update")
end

function draw()
    print("timer ".. timer)
end
