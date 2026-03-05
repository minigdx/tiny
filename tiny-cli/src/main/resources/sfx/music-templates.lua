local M = {}

local note_names = { "C", "Cs", "D", "Ds", "E", "F", "Fs", "G", "Gs", "A", "As", "B" }

local function pick(t)
    return t[math.random(1, #t)]
end

local function rand_int(min, max)
    return math.random(min, max)
end

local function make_note(semitone)
    semitone = math.max(0, math.min(semitone, 95))
    local octave = math.floor(semitone / 12)
    local note_index = (semitone % 12) + 1
    return note_names[note_index] .. octave
end

local function note_name_to_index(name)
    for i, n in ipairs(note_names) do
        if n == name then return i - 1 end
    end
    return 0
end

M.scales = {
    Major = { 0, 2, 4, 5, 7, 9, 11 },
    Minor = { 0, 2, 3, 5, 7, 8, 10 },
    ["Penta Maj"] = { 0, 2, 4, 7, 9 },
    ["Penta Min"] = { 0, 3, 5, 7, 10 },
    Dorian = { 0, 2, 3, 5, 7, 9, 10 },
    Mixolydian = { 0, 2, 4, 5, 7, 9, 10 },
}

M.scale_names = { "Major", "Minor", "Penta Maj", "Penta Min", "Dorian", "Mixolydian" }

M.root_notes = { "C", "Cs", "D", "Ds", "E", "F", "Fs", "G", "Gs", "A", "As", "B" }

M.progressions = {
    Classic = { 1, 5, 6, 4 },
    Melancholy = { 6, 4, 1, 5 },
    Dreamy = { 1, 4, 6, 5 },
    Tense = { 1, 7, 6, 5 },
    Upbeat = { 1, 4, 5, 4 },
    Cycle = { 2, 5, 1, 4 },
}

M.progression_names = { "Classic", "Melancholy", "Dreamy", "Tense", "Upbeat", "Cycle" }

M.drum_patterns = {
    Rock = {
        kick  = { 1, 0, 0, 0, 1, 0, 0, 0 },
        snare = { 0, 0, 1, 0, 0, 0, 1, 0 },
        hihat = { 1, 1, 1, 1, 1, 1, 1, 1 },
    },
    Dance = {
        kick  = { 1, 0, 1, 0, 1, 0, 1, 0 },
        snare = { 0, 0, 1, 0, 0, 0, 1, 0 },
        hihat = { 0, 1, 0, 1, 0, 1, 0, 1 },
    },
    Halftime = {
        kick  = { 1, 0, 0, 0, 0, 0, 0, 0 },
        snare = { 0, 0, 0, 0, 1, 0, 0, 0 },
        hihat = { 1, 0, 1, 0, 1, 0, 1, 0 },
    },
    Funky = {
        kick  = { 1, 0, 0, 1, 0, 0, 1, 0 },
        snare = { 0, 0, 1, 0, 0, 1, 0, 0 },
        hihat = { 1, 1, 0, 1, 1, 0, 1, 1 },
    },
    March = {
        kick  = { 1, 0, 1, 0, 1, 0, 1, 0 },
        snare = { 0, 1, 0, 1, 0, 1, 0, 1 },
        hihat = { 0, 0, 0, 0, 0, 0, 0, 0 },
    },
    Sparse = {
        kick  = { 1, 0, 0, 0, 0, 0, 0, 0 },
        snare = { 0, 0, 0, 0, 1, 0, 0, 0 },
        hihat = { 0, 0, 1, 0, 0, 0, 1, 0 },
    },
}

M.drum_pattern_names = { "Rock", "Dance", "Halftime", "Funky", "March", "Sparse" }

M.lead_styles = { "Stepwise", "Arpeggiated", "Bouncy", "Sparse", "Random" }

local function build_scale_notes(root_name, scale, octave)
    local root_semi = note_name_to_index(root_name) + octave * 12
    local notes = {}
    for _, interval in ipairs(scale) do
        table.insert(notes, root_semi + interval)
    end
    return notes
end

local function chord_root_semitone(root_name, scale, degree, octave)
    local root_semi = note_name_to_index(root_name) + octave * 12
    local idx = ((degree - 1) % #scale) + 1
    return root_semi + scale[idx]
end

local function build_chord_notes(root_name, scale, degree, octave)
    local root = chord_root_semitone(root_name, scale, degree, octave)
    local third_idx = ((degree - 1 + 2) % #scale) + 1
    local fifth_idx = ((degree - 1 + 4) % #scale) + 1
    local third = note_name_to_index(root_name) + octave * 12 + scale[third_idx]
    local fifth = note_name_to_index(root_name) + octave * 12 + scale[fifth_idx]
    if third <= root then third = third + 12 end
    if fifth <= root then fifth = fifth + 12 end
    return { root, third, fifth }
end

local function generate_chords(track, config)
    local scale = M.scales[config.scale_name]
    local progression = M.progressions[config.progression_name]
    track.clear()
    track.instrument = config.chord_instrument or 0
    track.volume = config.chord_volume or 0.6

    for bar = 0, 3 do
        local degree = progression[(bar % #progression) + 1]
        local chord = build_chord_notes(config.root, scale, degree, 3)
        for i = 0, 7 do
            local beat = bar * 8 + i
            if beat < 33 then
                local note_idx = (i % #chord) + 1
                track.set_note({ beat = beat, note = make_note(chord[note_idx]), volume = 0.5 })
            end
        end
    end
end

local function generate_bass(track, config)
    local scale = M.scales[config.scale_name]
    local progression = M.progressions[config.progression_name]
    track.clear()
    track.instrument = config.bass_instrument or 4
    track.volume = config.bass_volume or 0.8

    for bar = 0, 3 do
        local degree = progression[(bar % #progression) + 1]
        local root = chord_root_semitone(config.root, scale, degree, 2)
        for i = 0, 7 do
            local beat = bar * 8 + i
            if beat < 33 then
                if i == 0 or i == 4 then
                    track.set_note({ beat = beat, note = make_note(root), volume = 0.7 })
                elseif i == 2 or i == 6 then
                    local fifth_idx = ((degree - 1 + 4) % #scale) + 1
                    local fifth = note_name_to_index(config.root) + 2 * 12 + scale[fifth_idx]
                    track.set_note({ beat = beat, note = make_note(fifth), volume = 0.5 })
                end
            end
        end
    end
end

local function generate_lead(track, config)
    local scale = M.scales[config.scale_name]
    local style = config.lead_style or "Stepwise"
    track.clear()
    track.instrument = config.lead_instrument or 5
    track.volume = config.lead_volume or 0.5

    local scale_notes = build_scale_notes(config.root, scale, 4)
    local pos = rand_int(1, #scale_notes)

    for beat = 0, 32 do
        local play = false
        if style == "Stepwise" then
            play = true
            pos = pos + pick({ -1, 0, 1 })
        elseif style == "Arpeggiated" then
            play = true
            pos = pos + pick({ 1, 2 })
        elseif style == "Bouncy" then
            play = true
            pos = pos + pick({ -2, -1, 1, 2, 3 })
        elseif style == "Sparse" then
            play = (beat % 2 == 0) and (math.random() > 0.3)
            if play then pos = pos + pick({ -1, 0, 1 }) end
        elseif style == "Random" then
            play = math.random() > 0.25
            if play then pos = rand_int(1, #scale_notes) end
        end

        if pos < 1 then pos = pos + #scale_notes end
        if pos > #scale_notes then pos = ((pos - 1) % #scale_notes) + 1 end

        if play and beat < 33 then
            local semi = scale_notes[pos]
            if semi > 95 then semi = semi - 12 end
            if semi < 0 then semi = semi + 12 end
            track.set_note({ beat = beat, note = make_note(semi), volume = 0.4 + math.random() * 0.2 })
        end
    end
end

local function generate_drums(track, config)
    local pattern = M.drum_patterns[config.drum_pattern or "Rock"]
    track.clear()
    track.instrument = config.drum_instrument or 3
    track.volume = config.drum_volume or 0.7

    local kick_note = "C2"
    local snare_note = "C4"
    local hihat_note = "C6"

    for bar = 0, 3 do
        for i = 0, 7 do
            local beat = bar * 8 + i
            local pi = (i % #pattern.kick) + 1
            if beat < 33 then
                if pattern.kick[pi] == 1 then
                    track.set_note({ beat = beat, note = kick_note, volume = 0.7 })
                elseif pattern.snare[pi] == 1 then
                    track.set_note({ beat = beat, note = snare_note, volume = 0.6 })
                elseif pattern.hihat[pi] == 1 then
                    track.set_note({ beat = beat, note = hihat_note, volume = 0.35 })
                end
            end
        end
    end
end

M.generate = function(seq, config)
    seq.tempo = config.bpm or 120

    local track0 = seq.track(0)
    local track1 = seq.track(1)
    local track2 = seq.track(2)
    local track3 = seq.track(3)

    generate_chords(track0, config)
    generate_bass(track1, config)
    generate_lead(track2, config)
    generate_drums(track3, config)
end

return M
