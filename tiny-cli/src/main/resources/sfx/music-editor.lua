local widgets = require("widgets")
local mouse = require("mouse")
local wire = require("wire")

local m = {
    widgets = {}
}

local Cursor = {
    tracki = 1,
    fieldi = 1,
    fields = {
        { x = 0, width = 11, name = "notei" },
        { x = 12, width = 8, name = "octave" },
        { x = 22, width = 14, name = "volume" },
        { x = 39, width = 9, name = "mode" },
        { x = 51, width = 13, name = "instrument" },
    },
    track = nil, -- widget of the track
    tracks = {}, -- all tracks, to pass to the next one, ...
    beati = 0,
    beat_max = 20, -- number of beats displayed
    height = 10,
    width = 72,
    key_repeat_count = {
        [keys.down] = 0,
        [keys.up] = 0,
        [keys.left] = 0,
        [keys.right] = 0
    },
    editor = false, -- editor mode activation
}

Cursor._update = function(self)
    self:_move_cursor()
    self.track = self.tracks[self.tracki]

    local step = self.track.height / self.beat_max
    self.x = self.track.x
    self.y = self.track.y + math.floor(self.beati) * step
end

Cursor._move_cursor = function(self)
    local key_repeat_delay_frames = 10
    function easeKeys(key, on_pressed, on_pressing)
        if ctrl.pressed(key) then
            on_pressed()
            self.key_repeat_count[key] = key_repeat_delay_frames
        elseif ctrl.pressing(key) then
            if self.key_repeat_count[key] > 0 then
                self.key_repeat_count[key] = self.key_repeat_count[key] - 1
            else
                on_pressed()
                self.key_repeat_count[key] = key_repeat_delay_frames / 5
            end
        else
            self.key_repeat_count[key] = 0
        end
    end

    if self.editor then
        easeKeys(keys.down, function()
            if self.fields[self.fieldi].name == "notei" then
                -- Special handling for note field
                local beat = self.track.track.beats[self.beati + 1]
                local current_notei = beat.notei or 0

                if current_notei == 1 then
                    -- C0 (lowest note)
                    -- Set to repeat previous note (null)
                    beat.notei = nil
                elseif beat.notei == nil then
                    -- Set to note off (silence)
                    beat.notei = -1
                else
                    -- Normal decrement
                    self.track:change(self.beati + 1, self.fields[self.fieldi].name, -1)
                end
            else
                -- Normal field handling
                self.track:change(self.beati + 1, self.fields[self.fieldi].name, -1)
            end
        end)
        easeKeys(keys.up, function()
            if self.fields[self.fieldi].name == "notei" then
                -- Special handling for note field
                local beat = self.track.track.beats[self.beati + 1]

                if beat.notei == -1 then
                    -- Note off (silence)
                    -- Set to repeat previous note (null)
                    beat.notei = nil
                elseif beat.notei == nil then
                    -- Set to C0 (first note)
                    beat.notei = 1
                else
                    -- Normal increment
                    self.track:change(self.beati + 1, self.fields[self.fieldi].name, 1)
                end
            else
                -- Normal field handling
                self.track:change(self.beati + 1, self.fields[self.fieldi].name, 1)
            end
        end)

        -- Handle delete key to set note to null (repeat previous)
        if ctrl.pressed(keys.delete) and self.fields[self.fieldi].name == "notei" then
            local beat = self.track.track.beats[self.beati + 1]
            beat.notei = nil
        end
    else
        easeKeys(keys.down, function()
            self.beati = math.floor(self.beati) + 1
        end)
        easeKeys(keys.up, function()
            self.beati = math.floor(self.beati) - 1
        end)
    end

    easeKeys(keys.left, function()
        self.fieldi = math.floor(self.fieldi) - 1
    end)
    easeKeys(keys.right, function()
        self.fieldi = math.floor(self.fieldi) + 1
    end)

    -- switch tracks
    if (self.fieldi > #self.fields) then
        self.tracki = self.tracki + 1
        if (self.tracki <= #self.tracks) then
            self.fieldi = 1
        end
    elseif (self.fieldi < 1) then
        self.tracki = self.tracki - 1
        if (self.tracki >= 1) then
            self.fieldi = #self.fields
        end
    end
    self.fieldi = math.clamp(1, self.fieldi, #self.fields)
    self.tracki = math.clamp(1, self.tracki, #self.tracks)
    self.beati = math.clamp(0, self.beati, self.beat_max - 1)

    self.track = self.tracks[self.tracki]

    if ctrl.pressed(keys.enter) then
        -- editor mode
        self.editor = not self.editor
    end
end

Cursor._draw = function(self)

    local index = math.floor(self.fieldi)
    local field_x = self.x + self.fields[index].x
    local field_w = self.fields[index].width

    shape.rect(field_x, self.y, field_w, self.height, 9)
    shape.rect(self.x, self.y, self.width, self.height, 9)

    if self.editor then
        spr.sdraw(field_x + 2, self.y - 8, 240, 40, 8, 8)
        spr.sdraw(field_x + 2, self.y + 8, 240, 40, 8, 8, false, true)
    end
end

local TrackEditor = {
    track = nil, -- the actual dictionary of the track
    beat_offset = 1
}

TrackEditor.change = function(self, beat, name, inc)
    self.track.beats[beat][name] = (self.track.beats[beat][name] or 0) + inc
end

TrackEditor._update = function(self)

end

TrackEditor._draw = function(self)
    print("N  O  VV  M  I", self.x + 2, self.y - 8)
    local offset = self.beat_offset

    -- todo: introduce offset if the user is going down in the list
    for i = 1, 20 do
        local y = (i - offset) * 10 + (self.y + 3)
        print(string.format("%02x", i), self.x - 10, y)
    end
    for i, beat in ipairs(self.track.beats) do
        if i >= offset and i < offset + 20 then
            local y = (i - offset) * 10 + (self.y + 3)

            -- Check note state
            if beat.notei == nil then
                -- Repeat previous note (null)
                print("-- .  ..  .  .", self.x + 2, y)
            elseif beat.notei == -1 then
                -- Note off (silence)
                print("== .  ..  .  .", self.x + 2, y)
            elseif beat.note == nil then
                -- Empty beat
                print(".. .  ..  .  .", self.x + 2, y)
            else
                -- Normal note
                local note = beat.note
                if (#note == 1) then
                    note = note .. " "
                end
                local mode
                if beat.mode >= 1 then
                    mode = "R"
                else
                    mode = "L"
                end

                local instrument = beat.instrument or "."
                if instrument ~= "." then
                    instrument = string.format("%01x", instrument)
                end

                print(
                        note ..
                                " " .. beat.octave ..
                                "  " .. string.format("%02x", beat.volume) ..
                                "  " .. mode .. "  " .. instrument, self.x + 2, y
                )
            end

        end
    end

    -- border
    shape.rect(self.x, self.y, self.width, self.height, 10)
end

function _init()
    m.widgets = {}

    map.level("MusicEditor")

    local entities = map.entities()
    for mode in all(entities["EditorMode"]) do
        local button = widgets:create_mode_switch(mode)
        table.insert(m.widgets, button)
    end

    for k in all(entities["Knob"]) do
        local knob = widgets:create_knob(k)
        -- knob.on_hover = on_menu_item_hover
        table.insert(m.widgets, knob)

        if knob.fields.Label == "BPM" then

        end
    end

    for c in all(entities["Button"]) do
        local button = widgets:create_button(c)

        if button.fields.Type == "SINE" then
            button.on_change = function()
                sfx.export()
            end
        end
        table.insert(m.widgets, button)
    end

    local tracks = {}
    for mode in all(entities["TrackEditor"]) do
        local track = new(TrackEditor, mode)
        local volume = wire.find_widget(m.widgets, track.fields.Volume)

        track.track = sfx.track(track.fields.Track)

        wire.produce_to(volume, { "value" }, track, { "track", "volume" })
        wire.consume_on_update(volume, { "value" }, track, { "track", "volume" })

        table.insert(m.widgets, track)
        table.insert(tracks, track)
    end

    local cursor = new(Cursor)
    cursor.track = tracks[1]
    cursor.tracks = tracks
    table.insert(m.widgets, cursor)

end

function _draw()
    map.draw()

    for w in all(m.widgets) do
        w:_draw()
    end
    mouse._draw()
end

local mhandler = nil

function _update()
    mouse._update(function()
    end, function()
    end, function()
    end)

    if (ctrl.pressed(keys.space)) then
        if mhandler then
            mhandler.stop()
        end

        mhandler = sfx.music(0)
    end

    for w in all(m.widgets) do
        w:_update()
    end
end