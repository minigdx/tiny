local M = {}

-- Note names and musical scales
local note_names = { "C", "Cs", "D", "Ds", "E", "F", "Fs", "G", "Gs", "A", "As", "B" }
local major_scale = { 0, 2, 4, 5, 7, 9, 11 }
local minor_scale = { 0, 2, 3, 5, 7, 8, 10 }
local major_triad = { 0, 4, 7 }

-- Helper: pick a random element from a table
local function pick(t)
    return t[math.random(1, #t)]
end

-- Helper: random integer in [min, max]
local function rand_int(min, max)
    return math.random(min, max)
end

-- Helper: random float in [min, max]
local function rand_float(min, max)
    return min + math.random() * (max - min)
end

-- Helper: build a note name string from a semitone index (0-based from C0)
local function make_note(semitone)
    semitone = math.max(0, math.min(semitone, 95))
    local octave = math.floor(semitone / 12)
    local note_index = (semitone % 12) + 1
    return note_names[note_index] .. octave
end

-- Helper: find an instrument whose wave matches one of the compatible types
local function find_instrument(compatible_waves)
    local candidates = {}
    for i = 0, 7 do
        local inst = sfx.instrument(i)
        if inst then
            for _, w in ipairs(compatible_waves) do
                if inst.wave == w then
                    table.insert(candidates, i)
                    break
                end
            end
        end
    end
    if #candidates > 0 then
        return pick(candidates)
    end
    return 0
end

-- Helper: clear all notes from an SFX bar
local function clear_notes(sfx_bar)
    local notes = sfx_bar.notes
    for i = #notes, 1, -1 do
        local n = notes[i]
        sfx_bar.remove_note({ beat = n.beat, note = n.note })
    end
end

-- Template definitions
M.definitions = {}

-- 1. Shoot: fast descending chromatic, short durations, fading volume
M.definitions["Shoot"] = {
    bpm = { 300, 400 },
    waves = { "NOISE", "SQUARE", "SAW_TOOTH" },
    generate = function(sfx_bar, inst)
        local count = rand_int(4, 8)
        local start_semi = rand_int(48, 72)
        local beat = 0
        for i = 1, count do
            local semi = start_semi - (i - 1) * rand_int(1, 3)
            local vol = 1.0 - (i - 1) / count * 0.7
            sfx_bar.set_note({ beat = beat, note = make_note(semi), duration = 0.5, unique = true })
            sfx_bar.set_volume({ beat = beat, volume = vol })
            beat = beat + 0.5
        end
    end,
}

-- 2. Jump: ascending whole-tone steps
M.definitions["Jump"] = {
    bpm = { 240, 360 },
    waves = { "SQUARE", "TRIANGLE", "PULSE" },
    generate = function(sfx_bar, inst)
        local count = rand_int(3, 6)
        local start_semi = rand_int(36, 48)
        local beat = 0
        for i = 1, count do
            local semi = start_semi + (i - 1) * 2
            local vol = 0.6 + (i - 1) / count * 0.4
            sfx_bar.set_note({ beat = beat, note = make_note(semi), duration = 0.5, unique = true })
            sfx_bar.set_volume({ beat = beat, volume = vol })
            beat = beat + 0.5
        end
    end,
}

-- 3. Confirm: 2-3 ascending major intervals
M.definitions["Confirm"] = {
    bpm = { 180, 240 },
    waves = { "TRIANGLE", "SINE", "SQUARE" },
    generate = function(sfx_bar, inst)
        local count = rand_int(2, 3)
        local root = rand_int(48, 60)
        local intervals = { 0, 4, 7 }
        local beat = 0
        for i = 1, count do
            local semi = root + intervals[i]
            sfx_bar.set_note({ beat = beat, note = make_note(semi), duration = 0.5, unique = true })
            sfx_bar.set_volume({ beat = beat, volume = 0.8 })
            beat = beat + 0.5
        end
    end,
}

-- 4. Coin: quick ascending major triad arpeggio
M.definitions["Coin"] = {
    bpm = { 300, 400 },
    waves = { "TRIANGLE", "SINE" },
    generate = function(sfx_bar, inst)
        local count = rand_int(3, 5)
        local root = rand_int(60, 72)
        local beat = 0
        for i = 1, count do
            local triad_index = ((i - 1) % #major_triad) + 1
            local octave_offset = math.floor((i - 1) / #major_triad) * 12
            local semi = root + major_triad[triad_index] + octave_offset
            sfx_bar.set_note({ beat = beat, note = make_note(semi), duration = 0.5, unique = true })
            sfx_bar.set_volume({ beat = beat, volume = 0.9 })
            beat = beat + 0.5
        end
    end,
}

-- 5. Hit: sharp descending notes, high volume
M.definitions["Hit"] = {
    bpm = { 240, 360 },
    waves = { "NOISE", "DRUM" },
    generate = function(sfx_bar, inst)
        local count = rand_int(1, 3)
        local start_semi = rand_int(48, 60)
        local beat = 0
        for i = 1, count do
            local semi = start_semi - (i - 1) * rand_int(4, 8)
            sfx_bar.set_note({ beat = beat, note = make_note(semi), duration = 0.5, unique = true })
            sfx_bar.set_volume({ beat = beat, volume = 1.0 })
            beat = beat + 0.5
        end
    end,
}

-- 6. Door Open: ascending stepped, longer duration
M.definitions["Door Open"] = {
    bpm = { 120, 180 },
    waves = { "SQUARE", "PULSE", "TRIANGLE" },
    generate = function(sfx_bar, inst)
        local count = rand_int(3, 5)
        local root = rand_int(36, 48)
        local beat = 0
        for i = 1, count do
            local scale_index = ((i - 1) % #major_scale) + 1
            local semi = root + major_scale[scale_index]
            sfx_bar.set_note({ beat = beat, note = make_note(semi), duration = 1.0, unique = true })
            sfx_bar.set_volume({ beat = beat, volume = 0.7 })
            beat = beat + 1.0
        end
    end,
}

-- 7. Door Close: descending stepped, longer duration
M.definitions["Door Close"] = {
    bpm = { 120, 180 },
    waves = { "SQUARE", "PULSE", "TRIANGLE" },
    generate = function(sfx_bar, inst)
        local count = rand_int(3, 5)
        local root = rand_int(48, 60)
        local beat = 0
        for i = 1, count do
            local scale_index = ((count - i) % #major_scale) + 1
            local semi = root + major_scale[scale_index]
            sfx_bar.set_note({ beat = beat, note = make_note(semi), duration = 1.0, unique = true })
            sfx_bar.set_volume({ beat = beat, volume = 0.7 })
            beat = beat + 1.0
        end
    end,
}

-- 8. Win: ascending major scale fanfare
M.definitions["Win"] = {
    bpm = { 160, 220 },
    waves = { "TRIANGLE", "SQUARE", "SAW_TOOTH" },
    generate = function(sfx_bar, inst)
        local count = rand_int(5, 8)
        local root = rand_int(48, 60)
        local beat = 0
        for i = 1, count do
            local scale_index = ((i - 1) % #major_scale) + 1
            local octave_offset = math.floor((i - 1) / #major_scale) * 12
            local semi = root + major_scale[scale_index] + octave_offset
            local vol = 0.6 + (i - 1) / count * 0.4
            sfx_bar.set_note({ beat = beat, note = make_note(semi), duration = 0.5, unique = true })
            sfx_bar.set_volume({ beat = beat, volume = vol })
            beat = beat + 0.5
        end
    end,
}

-- 9. Lose: descending minor scale, fading
M.definitions["Lose"] = {
    bpm = { 100, 160 },
    waves = { "SINE", "TRIANGLE" },
    generate = function(sfx_bar, inst)
        local count = rand_int(3, 5)
        local root = rand_int(48, 60)
        local beat = 0
        for i = 1, count do
            local scale_index = ((count - i) % #minor_scale) + 1
            local semi = root + minor_scale[scale_index]
            local vol = 1.0 - (i - 1) / count * 0.6
            sfx_bar.set_note({ beat = beat, note = make_note(semi), duration = 1.0, unique = true })
            sfx_bar.set_volume({ beat = beat, volume = vol })
            beat = beat + 1.0
        end
    end,
}

-- 10. Explosion: random noise bursts, varied volume
M.definitions["Explosion"] = {
    bpm = { 240, 400 },
    waves = { "NOISE", "DRUM" },
    generate = function(sfx_bar, inst)
        local count = rand_int(6, 12)
        local beat = 0
        for i = 1, count do
            local semi = rand_int(24, 48)
            local vol = rand_float(0.5, 1.0)
            sfx_bar.set_note({ beat = beat, note = make_note(semi), duration = 0.5, unique = true })
            sfx_bar.set_volume({ beat = beat, volume = vol })
            beat = beat + 0.5
        end
    end,
}

-- 11. Power Up: ascending chromatic sweep
M.definitions["Power Up"] = {
    bpm = { 200, 300 },
    waves = { "SAW_TOOTH", "SQUARE", "PULSE" },
    generate = function(sfx_bar, inst)
        local count = rand_int(6, 10)
        local start_semi = rand_int(36, 48)
        local beat = 0
        for i = 1, count do
            local semi = start_semi + (i - 1)
            local vol = 0.5 + (i - 1) / count * 0.5
            sfx_bar.set_note({ beat = beat, note = make_note(semi), duration = 0.5, unique = true })
            sfx_bar.set_volume({ beat = beat, volume = vol })
            beat = beat + 0.5
        end
    end,
}

-- 12. Power Down: descending chromatic sweep
M.definitions["Power Down"] = {
    bpm = { 200, 300 },
    waves = { "SAW_TOOTH", "SQUARE", "PULSE" },
    generate = function(sfx_bar, inst)
        local count = rand_int(6, 10)
        local start_semi = rand_int(48, 72)
        local beat = 0
        for i = 1, count do
            local semi = start_semi - (i - 1)
            local vol = 1.0 - (i - 1) / count * 0.5
            sfx_bar.set_note({ beat = beat, note = make_note(semi), duration = 0.5, unique = true })
            sfx_bar.set_volume({ beat = beat, volume = vol })
            beat = beat + 0.5
        end
    end,
}

-- 13. Menu Select: 1-2 short high blips
M.definitions["Menu Select"] = {
    bpm = { 200, 300 },
    waves = { "TRIANGLE", "SINE", "SQUARE" },
    generate = function(sfx_bar, inst)
        local count = rand_int(1, 2)
        local root = rand_int(60, 72)
        local beat = 0
        for i = 1, count do
            local semi = root + (i - 1) * rand_int(3, 5)
            sfx_bar.set_note({ beat = beat, note = make_note(semi), duration = 0.5, unique = true })
            sfx_bar.set_volume({ beat = beat, volume = 0.7 })
            beat = beat + 0.5
        end
    end,
}

-- 14. Menu Back: 1-2 short descending blips
M.definitions["Menu Back"] = {
    bpm = { 200, 300 },
    waves = { "TRIANGLE", "SINE" },
    generate = function(sfx_bar, inst)
        local count = rand_int(1, 2)
        local root = rand_int(60, 72)
        local beat = 0
        for i = 1, count do
            local semi = root - (i - 1) * rand_int(3, 5)
            sfx_bar.set_note({ beat = beat, note = make_note(semi), duration = 0.5, unique = true })
            sfx_bar.set_volume({ beat = beat, volume = 0.7 })
            beat = beat + 0.5
        end
    end,
}

-- 15. Footstep: 1-2 very short percussive notes
M.definitions["Footstep"] = {
    bpm = { 120, 180 },
    waves = { "NOISE", "DRUM" },
    generate = function(sfx_bar, inst)
        local count = rand_int(1, 2)
        local beat = 0
        for i = 1, count do
            local semi = rand_int(24, 36)
            sfx_bar.set_note({ beat = beat, note = make_note(semi), duration = 0.5, unique = true })
            sfx_bar.set_volume({ beat = beat, volume = 0.6 })
            beat = beat + 0.5
        end
    end,
}

-- 16. Laser: fast descending chromatic
M.definitions["Laser"] = {
    bpm = { 300, 480 },
    waves = { "SAW_TOOTH", "PULSE", "SQUARE" },
    generate = function(sfx_bar, inst)
        local count = rand_int(6, 12)
        local start_semi = rand_int(60, 84)
        local beat = 0
        for i = 1, count do
            local semi = start_semi - (i - 1) * rand_int(1, 2)
            local vol = 0.9 - (i - 1) / count * 0.4
            sfx_bar.set_note({ beat = beat, note = make_note(semi), duration = 0.5, unique = true })
            sfx_bar.set_volume({ beat = beat, volume = vol })
            beat = beat + 0.5
        end
    end,
}

-- Ordered list for dropdown display
M.list = {
    "Shoot", "Jump", "Confirm", "Coin",
    "Hit", "Door Open", "Door Close", "Win",
    "Lose", "Explosion", "Power Up", "Power Down",
    "Menu Select", "Menu Back", "Footstep", "Laser",
}

-- Main generate function
M.generate = function(sfx_bar, template_name)
    local def = M.definitions[template_name]
    if not def then
        return
    end

    -- Clear existing notes
    clear_notes(sfx_bar)

    -- Set BPM
    sfx_bar.bpm = rand_int(def.bpm[1], def.bpm[2])

    -- Find a compatible instrument
    local inst_index = find_instrument(def.waves)
    sfx_bar.set_instrument(inst_index)

    -- Generate notes
    def.generate(sfx_bar, inst_index)

    -- Clamp notes to span at most 2 octaves
    local notes = sfx_bar.notes
    if #notes > 0 then
        local min_octave = 7
        local max_octave = 0
        for note in all(notes) do
            if note.octave < min_octave then min_octave = note.octave end
            if note.octave > max_octave then max_octave = note.octave end
        end

        if max_octave - min_octave > 1 then
            local saved = {}
            for note in all(notes) do
                table.insert(saved, {
                    beat = note.beat,
                    note = note.note,
                    octave = note.octave,
                    duration = note.duration,
                    volume = note.volume,
                })
            end
            clear_notes(sfx_bar)
            for _, note in ipairs(saved) do
                local clamped_octave = math.clamp(min_octave, note.octave, min_octave + 1)
                local pitch_class = string.sub(note.note, 1, #note.note - 1)
                local new_note_name = pitch_class .. clamped_octave
                sfx_bar.set_note({
                    beat = note.beat,
                    note = new_note_name,
                    duration = note.duration,
                    unique = true,
                })
                sfx_bar.set_volume({ beat = note.beat, volume = note.volume })
            end
        end

        return min_octave
    end
end

return M
