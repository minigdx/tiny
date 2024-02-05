local mouse = require("mouse")
local widgets = require("widgets")

local labels = {"C0", "Db0", "D0", "Eb0", "E0", "F0", "Gb0", "G0", "Ab0", "A0", "Bb0", "B0", "C1", "Db1", "D1", "Eb1",
                "E1", "F1", "Gb1", "G1", "Ab1", "A1", "Bb1", "B1", "C2", "Db2", "D2", "Eb2", "E2", "F2", "Gb2", "G2",
                "Ab2", "A2", "Bb2", "B2", "C3", "Db3", "D3", "Eb3", "E3", "F3", "Gb3", "G3", "Ab3", "A3", "Bb3", "B3",
                "C4", "Db4", "D4", "Eb4", "E4", "F4", "Gb4", "G4", "Ab4", "A4", "Bb4", "B4", "C5", "Db5", "D5", "Eb5",
                "E5", "F5", "Gb5", "G5", "Ab5", "A5", "Bb5", "B5", "C6", "Db6", "D6", "Eb6", "E6", "F6", "Gb6", "G6",
                "Ab6", "A6", "Bb6", "B6", "C7", "Db7", "D7", "Eb7", "E7", "F7", "Gb7", "G7", "Ab7", "A7", "Bb7", "B7",
                "C8", "Db8", "D8", "Eb8", "E8", "F8", "Gb8", "G8", "Ab8", "A8", "Bb8", "B8"}

local waves = {{
    type = "sine",
    color = 9
}, {
    type = "noise",
    color = 4
}, {
    type = "pulse",
    color = 10
}, {
    type = "triangle",
    color = 13
}, {
    type = "saw",
    color = 11
}, {
    type = "square",
    color = 15
}}

local faders = {}
local current_wave = waves[1]

function on_fader_update(fader, value)
    fader.value = math.ceil(value)
    fader.data = {
        note = labels[fader.value],
        wave = current_wave.type,
        value = fader.value,
        color = current_wave.color
    }
    fader.label = labels[fader.value]
    fader.tip_color = current_wave.color
end

function on_active_button(current, prec)
    current_wave = current.data.wave
end

local active_tab = nil

function on_active_tab(current, prec)
    local data = {}
    -- save the current score
    for f in all(faders) do
        table.insert(data, {
            wave = f.data.wave,
            note = f.data.note,
            value = f.value,
            color = f.tip_color
        })
    end
    if prec ~= nil then
        prec.data = data
    end

    -- restore the previous score
    if current.data ~= nil then
        local data = current.data
        for k, f in ipairs(faders) do
            f.data = data[k]
            f.value = data[k].value
            f.label = labels[f.value]
            f.tip_color = data[k].color
        end
    else
        -- no data, reset to 0
        for k, f in ipairs(faders) do
            f.value = 0
            f.label = ""
            f.data = {
                wave = "",
                note = 0,
                value = 0,
                color = 0
            }
        end
    end

    active_tab = current
end

local window = {
    width = 0,
    height = 0
}

function on_new_tab(tab)
    local filename = ws.create("sfx", "sfx")
    tab.label = filename
end

function on_play_button()
    local score = generate_score()
    sfx.sfx(score, 220)
end

function on_save_button()
    local score = generate_score()
    ws.save(active_tab.label, score)
end

function on_decrease_bpm(counter)
    counter.value = math.max(10, counter.value - 5)
end

function on_increase_bpm(counter)
    counter.value = math.min(220, counter.value + 5)
end

function _init(w, h)

    widgets.on_new_tab = on_new_tab
    window.width = w
    window.height = h

    -- buttons
    widgets.createButton({
        x = 10,
        y = 16,
        overlay = 22,
        grouped = false,
        on_active_button = on_play_button
    })

    widgets.createButton({
        x = 10,
        y = 16 + 2 + 16,
        overlay = 23,
        data = {
            save = true
        },
        grouped = false,
        on_active_button = on_save_button
    })

    widgets.createButton({
        x = 10,
        y = 16 + 2 + 16 + 2 + 16,
        overlay = 24,
        grouped = false,
        on_active_button = on_play_button
    })


    widgets.createCounter({
        x = 10,
        y = 112,
        value = 1,
        label = "pattern",
        -- on_left = on_decrease_bpm,
        -- on_right = on_increase_bpm,
    })

    widgets.createCounter({
        x = 10,
        y = 112 + 24,
        value = 120,
        label = "bpm",
        on_left = on_decrease_bpm,
        on_right = on_increase_bpm,
    })

    -- faders
    for i = 1, 32 do
        local f = widgets.createFader({
            x = 10 + 16 + i * 12,
            y = 16 + 16 + 2,
            height = 256 - 18,
            label = labels[0],
            value = 0,
            max_value = #labels,
            data = {
                note = labels[0]
            },
            on_value_update = on_fader_update
        })
        table.insert(faders, f)
    end

    -- buttons
    for i = #waves - 1, 0, -1 do
        local w = widgets.createButton({
            x = 10,
            y = 250 - i * 16,
            overlay = 16 + i,
            data = {
                wave = waves[i + 1]
            },
            on_active_button = on_active_button
        })

        if i == 0 then
            w.status = 2
        end
    end

    -- tabs

    local files = ws.list()

    local tabs = {}
    local new_tab_x = 0
    if #files > 0 then
        for w in all(files) do
            local tab = widgets.createTab({
                x = new_tab_x,
                width = 2 * 16 + 8,
                status = 0,
                label = w,
                content = ws.load(w),
                on_active_tab = on_active_tab
            })
            table.insert(tabs, tab)
            new_tab_x = new_tab_x + tab.width
        end
    else
        local file = ws.create("sfx", "sfx")
        local tab = widgets.createTab({
            x = 0,
            width = 2 * 16 + 8,
            status = 0,
            label = file,
            on_active_tab = on_active_tab
        })
        table.insert(tabs, tab)
        new_tab_x = new_tab_x + tab.width
    end

    tabs[1].status = 1
    active_tab = tabs[1]

    widgets.createTab({
        x = new_tab_x,
        width = 24,
        status = 0,
        on_active_tab = on_active_tab,
        new_tab = true
    })
    --
    init_faders(tabs)
end

function extract(inputString)
    local pattern = "([a-zA-Z]+)%(([^%)]+)%)"
    local wave, note = inputString:match(pattern)
    return wave, note
end

function split(inputString)
    local result = {}
    for token in string.gmatch(inputString, "[^%-]+") do
        table.insert(result, token)
    end
    return result
end

function init_faders(tabs)
    local index = 1

    local notes = {}
    for k, v in pairs(labels) do
        notes[v] = k
    end

    local colors = {}
    for v in all(waves) do
        colors[v.type] = v.color
    end

    for t in all(tabs) do
        local content = t.content
        if content then
            local saved = split(content)
            local result = {}
            for index, f in ipairs(faders) do
                local s = saved[index]

                local data = {
                    wave = "",
                    note = 0,
                    value = 0,
                    color = 0
                }

                if s ~= "(0)" and s ~= "*" then
                    local wave, note = extract(s)
                    data = {
                        wave = wave,
                        note = note,
                        value = notes[note],
                        color = colors[wave]
                    }
                end

                table.insert(result, data)
            end
            t.data = result
        end
    end
    on_active_tab(tabs[1])

end

function generate_score()
    local score = ""

    for f in all(faders) do
        if f.data ~= nil and f.data.note ~= nil then
            score = score .. f.data.wave .. "(" .. f.data.note .. ")-"
        else
            score = score .. "*-"
        end
    end
    return score
end

function _update()
    mouse._update(widgets.on_update, widgets.on_click, widgets.on_clicked)
    widgets._update()

    if ctrl.pressed(keys.space) then
        local score = generate_score()
        sfx.sfx(score, 220)
    end

    local new_wave = current_wave
end
--
function _draw()
    gfx.cls(2)
    -- background for tabs
    shape.rectf(0, 0, window.width, 8, 1)
    -- octave limits
    local per_octave = math.floor((256 - 18) / 9) -- height / nb octaves
    for octave = 9, 0, -1 do
        local y = 34 + (256 - 18) - octave * per_octave
        gfx.dither(0x1010)
        shape.line(36, y - 2, 36 + 32 * 12, y - 2, 3)
        gfx.dither()
        print("<C" .. octave, 40 + 32 * 12, y - 4, 3)
    end
    gfx.dither()

    widgets._draw()
    mouse._draw(current_wave.color)
end
