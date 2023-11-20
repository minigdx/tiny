function _init(w, h)
    dt = 0
    msg = { logo = 0, text = "", color = 0 }
    width = w
    forever = false
    letter_split = math.ceil(width / 6)
end

function _add_return_line(text)
    local msg = string.gsub(text, "\n", "")
    local new_msg = ""

    for i = 1, #msg, letter_split do
        new_msg = new_msg .. msg:sub(i, i + letter_split) .."\n"
    end
    return new_msg
end

function popup(logo, text, color, keep)
    msg = { logo = logo, text = _add_return_line(text), color = color, lines = (math.ceil( #text / letter_split) + 1)}
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
        shape.rectf(0, 0, width, 6 * msg.lines, msg.color)
        -- TODO: display the logo
        print(msg.text, 6, 1, "#FFFFFF")
    end
end
