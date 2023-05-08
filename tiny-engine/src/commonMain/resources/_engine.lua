function _init(w, h)
    dt = 0
    msg = { logo = 0, text = "", color = 0 }
    width = w
    forever = false
end

function popup(logo, text, color, keep)
    msg = { logo = logo, text = text, color = color }
    dt = 2
    forever = keep
end

function clear()
    dt = 0
    forever = false
end

function _update()
    if dt > 0 then
        dt = dt - 1 / 60
    end
end

function _draw()
    if forever or dt > 0 then
        shape.rectf(0, 0, width, 6, msg.color)
        -- TODO: display the logo
        print(msg.text, 6, 1, "#FFFFFF")
    end
end
