local ready = false

function _init()
end

function _update()
    if ready then
        gfx.cls("#FFFFFF")
        tiny.exit(0)
    end
end

function _draw()
    gfx.cls("#FFFFFF")
end

function _resources()
    ready = true
end
