function restoreState()

end

-- function saveState()

-- end

function init()
    print("BOOT init / game reloaded !")
    frame = 0
end

function update()
    print("BOOT it's time to update ")
    print(frame)
    frame = frame + 1
    -- End of the animation. Time to quit the boot sequence.
    if frame > 60 then
        print("bye bye")
        exit()
    end
end

function draw()
    print("BOOT it's time to draw")
end
