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
    color = 9,
    index = 1,
    overlay = 16
},  {
    type = "square",
    color = 15,
    index = 2,
    overlay = 21,
}, {
    type = "triangle",
    color = 13,
    index = 3,
    overlay = 19
}, {
    type = "noise",
    color = 4,
    index = 4,
    overlay = 17
}, {
    type = "pulse",
    color = 10,
    index = 5,
    overlay = 18
}, {
    type = "saw",
    color = 11,
    overlay = 20,
    index = 6
},}

local bpm = nil
local patterns = nil
local faders = {}
local current_wave = waves[1]

function on_fader_update(fader, value)
    widgets.setFaderValue(fader, current_wave.index, math.ceil(value), current_wave.color)
end

function on_active_button(current, prec)
    current_wave = current.data.wave
end

local active_tab = nil

function on_active_tab(current, prec)
    if prec ~= nil then
        local score = generate_score()
        prec.content = sfx.to_table(score)
    end

    -- restore the previous score of the current tab
    if current.content ~= nil then
        local data = current.content
        bpm.value = data["bpm"]
        -- always get the first pattern
        local beats = data["patterns"][1]
        for k, f in ipairs(faders) do
            widgets.resetFaderValue(f)
            if beats[k] ~= nil then
                for b in all(beats[k]) do
                    if b.index > 0 then
                        local w = waves[b.index]
                        widgets.setFaderValue(f, b.index, b.note, w.color)
                    else
                        -- set silence value
                        widgets.resetFaderValue(f)
                    end
                end
            else
                -- set silence value
                widgets.resetFaderValue(f)
            end
        end
    else
        bpm.value = 120
        -- no data, reset to 0
        for k, f in ipairs(faders) do
            widgets.resetFaderValue(f)
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
    sfx.sfx(score)
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

    patterns = widgets.createCounter({
        x = 10,
        y = 112,
        value = 1,
        label = "pattern"
        -- on_left = on_decrease_bpm,
        -- on_right = on_increase_bpm,
    })

    bpm = widgets.createCounter({
        x = 10,
        y = 112 + 24,
        value = 120,
        label = "bpm",
        on_left = on_decrease_bpm,
        on_right = on_increase_bpm
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
            overlay = waves[i + 1].overlay,
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
                content = sfx.to_table(ws.load(w)),
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

    on_active_tab(tabs[1])
end

function to_hex(number)
    local hexString = string.format("%X", number)

    -- Add a leading zero if the number is below 16
    if number < 16 then
        hexString = "0" .. hexString
    end

    return hexString
end

function generate_score()
    local score = "tiny-sfx 1 " .. bpm.value .. " 255\n"

    -- write patterns

    local strip = ""
    for f in all(faders) do
        local beat = ""
        if f.values ~= nil and next(f.values) then
            for k, v in pairs(f.values) do
                if #beat > 0 then
                    beat = beat .. ":"
                end
                beat = beat .. to_hex(k) .. to_hex(v.value) .. to_hex(255)
            end
        else
            beat = "0000FF"
        end

        strip = strip .. beat .. " "
    end

    score = score .. strip .. "\n"
    -- write patterns order
    score = score .. "1"
    return score
end

function _update()
    mouse._update(widgets.on_update, widgets.on_click, widgets.on_clicked)
    widgets._update()

    if ctrl.pressed(keys.space) then
        local score = generate_score()
        sfx.sfx(score)
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
