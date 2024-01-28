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
    prec.data = data

    -- restore the previous score
    if current.data ~= nil then
        local data = current.data
        for k,f in ipairs(faders) do
            f.data = data[k]
            f.value = data[k].value
            f.label = labels[f.value]
            f.tip_color = data[k].color
        end
    else
        -- no data, reset to 0
        for k,f in ipairs(faders) do
            f.value = 0
            f.label = ""
            f.data = {
                wave = "",
                note = 0,
                value = 0,
                color = 0
            }
            -- f.tip_color = data[k].color
        end
    end
end


local window = {
    width = 0,
    height = 0
}
function _init(w, h)

    window.width = w
    window.height = h

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
    for i = 0, #waves - 1 do
        local w = widgets.createButton({
            x = 10,
            y = 16 + i * 16,
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
    local tab = widgets.createTab({
        x = 0,
        width = 2 * 16 + 8,
        status = 1,
        label = "hello",
        on_active_tab = on_active_tab
    })

    widgets.createTab({
        x = tab.width,
        width = 24,
        status = 0,
        on_active_tab = on_active_tab,
        new_tab = true
    })
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
        debug.console(score)
        sfx.sfx(score, 220)
    end

    local new_wave = current_wave
    if ctrl.pressed(keys.up) then
        for i = 1, #waves do
            if waves[i].type == current_wave.type then
                local next_index = (i % #waves) + 1
                new_wave = waves[next_index]
            end
        end
        current_wave = new_wave
    end
end
--
function _draw()
    gfx.cls(2)
    -- background for tabs
    shape.rectf(0, 0, window.width, 8, 1)
    widgets._draw()
    mouse._draw(current_wave.color)
end
