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
}, {
    type = "square",
    color = 15,
    index = 2,
    overlay = 21
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
}}

local bpm = nil
local patterns = nil
local volume = nil

local faders = {}
local current_wave = waves[1]

local active_tab = nil

local fader_mode = true
local switch_mode = nil

local fader_widgets = {}
local music_widgets = {}


function on_active_button(current, prec)
    current_wave = current.data.wave
end


local window = {
    width = 0,
    height = 0
}

function on_play_button()
    local score = nil
    if fader_mode then
        score = generate_score(patterns.value)
    else
        score = generate_score()
    end
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

function on_previous_patterns(counter)
    counter.value = math.max(counter.value - 1, 1)
    active_pattern(counter.value, active_tab.content)
end

function on_next_patterns(counter)
    counter.value = math.min(counter.value + 1, 10)
    active_pattern(counter.value, active_tab.content)
end

function on_decrease_pattern(counter)
    counter.value = math.max(counter.value - 1, 1)
end

function on_increase_pattern(counter)
    counter.value = math.min(counter.value + 1, #active_tab.content["patterns"])
end

function on_decrease_volume(counter)
    counter.value = math.max(counter.value - 1, 1)
end

function on_increase_volume(counter)
    counter.value = math.min(counter.value + 1, 10)
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

local editor = {
    mode = 0, -- 0 -> sound editor ; 1 -> pattern editor ; 2 -> music editor
    play_button = nil,
    save_button = nil,
    switch_button = nil,
    pattern_counter = nil,
    bpm_counter = nil,
    volume_counter = nil,
    active_tab = nil, -- current active (displayed) tab.
    sound_editor_widgets = {}, -- all widgets used only in the sound editor mode
    patterns_editor_widgets = {}, -- all widgets used only in the patterns editor mode
    fader_widgets = {}, -- all faders (used only in the sound editor mode)
    wave_widgets = {}, -- all waves button (used only in the sound editor mode)
    tabs_widgets = {} -- all the tabs
}

--[[
    enable widgets regarding the mode selected.
]]
editor.switch_to_mode = function(mode) 
    editor.mode = mode

    local enabled_sound_widgets = mode == 0
    local enabled_patterns_widgets = mode == 1

    for w in all(editor.sound_editor_widgets) do
        w.enabled = enabled_sound_widgets
    end

    for w in all(editor.patterns_editor_widgets) do
        w.enabled = enabled_patterns_widgets
    end
end

editor.activate_pattern = function(index, data)
    local beats = data["patterns"][index]
    
    for k, f in ipairs(editor.fader_widgets) do
        local beat = beats[k]
        if beat ~= nil and beat.index > 0 then
            -- set fader value
            f.value = beat.note
            f.tip_color = waves[beat.index].color
        else
            -- set fader value to 0
            f.value = 0
            f.tip_color = 0
        end
    end
end

editor.generate_score = function(content, pattern_selector)
    local p = content["patterns"]
    local v = math.floor((editor.volume_counter.value * 25.5))
    local bpm = editor.bpm_counter.value

    local score = "tiny-sfx " .. #p .. " " .. bpm .. " " .. v .. "\n"

    -- write patterns
    for patterns in all(content["patterns"]) do
        local strip = ""
        for index = 1, 32 do
            local beatStr = ""
            local beat = patterns[index]
            if beat == nil then
                beatStr = beatStr .. "0000FF"
            else
                beatStr = beatStr .. to_hex(beat.index) .. to_hex(beat.note) .. to_hex(beat.volume)
            end
            strip = strip .. beatStr .. " "
        end
        --
        score = score .. strip .. "\n"
    end

    -- write patterns order
    if pattern_selector == nil then
        pattern_selector = ""
        local stop = false
        for w in all(music_widgets) do
            if w.value == 0 then
                stop = true
            end
            if (not stop) then
                pattern_selector = pattern_selector .. w.value .. " "
            end
        end
    end
    score = score .. pattern_selector

    return score
end

--[[
    Callback when a new tab is created.
]]
editor.on_new_tab = function(tab)
    -- create a new file and assign the name to the new tab.
    local filename = ws.create("sfx", "sfx")
    tab.label = filename
    tab.content = sfx.to_table(sfx.empty_score())
    table.insert(editor.tabs_widgets, tab)
end

--[[
    Callback when a new tab is active. 
]]
editor.on_active_tab = function(current, prev)
    editor.switch_to_mode(0)

    if prev ~= nil then
        -- update the model of the previous tab before switching.
        local score = editor.generate_score(prev.content)
        prev.content = sfx.to_table(score)
    end

    local data = current.content

    editor.bpm_counter.value = data["bpm"]
    editor.volume_counter.value = math.floor((data["volume"] / 255) * 10)

    -- always get the first pattern
    editor.activate_pattern(1, data)
    -- set faders value regarding the first patterns
    editor.active_tab = current
end

editor.create_widgets = function()
    
    -- buttons
    editor.play_button = widgets.createButton({
        x = 10,
        y = 16,
        overlay = 22,
        grouped = false,
        on_active_button = on_play_button
    })

    editor.save_button = widgets.createButton({
        x = 10,
        y = 16 + 2 + 16,
        overlay = 23,
        data = {
            save = true
        },
        grouped = false,
        on_active_button = on_save_button
    })

    editor.switch_button = widgets.createButton({
        x = 10,
        y = 16 + 2 + 16 + 2 + 16,
        overlay = 24,
        grouped = false,
        on_active_button = on_switch_mode
    })

    editor.pattern_counter = widgets.createCounter({
        x = 10,
        y = 90,
        value = 1,
        label = "pattern",
        on_left = on_previous_patterns,
        on_right = on_next_patterns
    })

    table.insert(editor.sound_editor_widgets, editor.pattern_counter)

    editor.bpm_counter = widgets.createCounter({
        x = 10,
        y = 90 + 24,
        value = 120,
        label = "bpm",
        on_left = on_decrease_bpm,
        on_right = on_increase_bpm
    })

    table.insert(editor.sound_editor_widgets, editor.bpm_counter)

    editor.volume_counter = widgets.createCounter({
        x = 10,
        y = 90 + 24 + 24,
        value = 10,
        label = "volume",
        on_left = on_decrease_volume,
        on_right = on_increase_volume
    })

    table.insert(editor.sound_editor_widgets, editor.volume_counter)

    local on_fader_update = function(fader, value)
        fader.value = math.ceil(value)
        fader.tip_color = current_wave.color
        local current_pattern = editor.active_tab.content["patterns"][editor.pattern_counter.value]

        current_pattern[fader.index] = {
            type = current_wave.type,
            volume = 1.0,
            index = current_wave.index,
            note = fader.value
        } 
    end

    -- faders
    for i = 1, 32 do
        local fader = widgets.createFader({
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
        table.insert(editor.fader_widgets, fader)
        table.insert(editor.sound_editor_widgets, fader)
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

        table.insert(editor.sound_editor_widgets, w)
        table.insert(editor.wave_widgets, w)

        -- activate the first button
        if i == 0 then
            w.status = 2
        end
    end

    -- music buttons
    for x = 1, 8 do
        for y = 1, 8 do
            local w = widgets.createCounter({
                x = 28 + x * 48,
                y = y * 32,
                value = nil,
                enabled = false,
                index = x + (y - 1) * 8,
                label = "pattern",
                on_left = on_decrease_pattern,
                on_right = on_increase_pattern
            })

            editor.patterns_editor_widgets[x + (y - 1) * 8] = w

            if x == 1 and y == 1 then
                w.value = 1
            end
        end
    end

    -- tabs
    local files = ws.list()

    local new_tab_x = 0
    if #files > 0 then
        for w in all(files) do
            local tab = widgets.createTab({
                x = new_tab_x,
                width = 2 * 16 + 8,
                status = 0,
                label = w,
                content = sfx.to_table(ws.load(w)),
                on_active_tab = editor.on_active_tab,
                on_new_tab = editor.on_new_tab
            })
            table.insert(editor.tabs_widgets, tab)
            new_tab_x = new_tab_x + tab.width
        end
    else
        local file = ws.create("sfx", "sfx")
        local tab = widgets.createTab({
            x = 0,
            width = 2 * 16 + 8,
            status = 0,
            label = file,
            content = sfx.to_table(sfx.empty_score()),
            on_active_tab = editor.on_active_tab,
            on_new_tab = editor.on_new_tab
        })
        table.insert(editor.tabs_widgets, tab)
        new_tab_x = new_tab_x + tab.width
    end

    -- activate the first tab
    editor.tabs_widgets[1].status = 1
    editor.active_tab = editor.tabs_widgets[1]

    local w = widgets.createTab({
        x = new_tab_x,
        width = 24,
        status = 0,
        on_active_tab = editor.on_active_tab,
        on_new_tab = editor.on_new_tab,
        new_tab = true
    })
end

function _init(w, h)
    window.width = w
    window.height = h

    --
    editor.create_widgets()
end

function _update()
    mouse._update(widgets.on_update, widgets.on_click, widgets.on_clicked)
    widgets._update()

    if ctrl.pressed(keys.space) then
        on_play_button()
    end

    local new_wave = current_wave
end
--
function _draw()
    gfx.cls(2)
    -- background for tabs
    shape.rectf(0, 0, window.width, 8, 1)

    if fader_mode then
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

    end
    widgets._draw()
    mouse._draw(current_wave.color)
end
