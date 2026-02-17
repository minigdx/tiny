local widgets = require("widgets")
local mouse = require("mouse")
local wire = require("wire")
local ModeSwitch = require("widgets/ModeSwitch")

local icons = {
    -- Waveform icons (16x16, row y=16)
    Sine      = { x = 16, y = 16 },
    Pulse     = { x = 32, y = 16 },
    Noise     = { x = 48, y = 16 },
    Sawtooth  = { x = 64, y = 16 },
    Triangle  = { x = 80, y = 16 },
    Square    = { x = 96, y = 16 },
    -- Editor mode icons (16x16, row y=80)
    Instrument = { x = 16, y = 80 },
    Sfx        = { x = 32, y = 80 },
    Music      = { x = 48, y = 80 },
    -- Action icons (8x8, row y=40)
    Play   = { x = 48, y = 40 },
    Save   = { x = 56, y = 40 },
    Export = { x = 64, y = 40 },
    Gear = { x = 0, y = 80 },
    Random = { x = 64, y = 80 },
    Envelope = { x = 80, y = 80 },
    Harmonics = { x = 96, y = 80 },
    Modulations = { x = 112, y = 80 },
}

local all_widgets = {}
local modals_by_name = {}

local state = {
    instrument = nil,
}

function _init_buttons(entities)
    for b in all(entities["Button"]) do
        local button = widgets:create_button(b)
        button.overlay = icons[button.fields.IconName]

        if button.fields.Modal then
            local modal_name = button.fields.Modal

            -- Create the modal if it doesn't exist yet
            if not modals_by_name[modal_name] then
                local modal = widgets:create_modal({
                    x = 96,
                    y = 64,
                    width = 192,
                    height = 128,
                    level_name = modal_name,
                    fields = {},
                })
                modals_by_name[modal_name] = modal
            end

            button.on_change = function()
                local target = modals_by_name[modal_name]
                if target then
                    target:open()
                end
            end
        end

        table.insert(all_widgets, button)
    end
end

function _init_dropdowns(entities)
    for d in all(entities["Dropdown"]) do
        local dropdown = widgets:create_dropdown(d)

        if #dropdown.options == 0 then
            for i = 1, 8 do
                table.insert(dropdown.options, "[" .. i .. "] Instrument " .. i)
            end
            dropdown:_init()
        end

        dropdown.on_change = function(self)
            state.instrument = sfx.instrument(self.selected)
        end

        table.insert(all_widgets, dropdown)
    end
end

function _init_mode_switch(entities)
    for mode in all(entities["ModeButton"]) do
        local button = new(ModeSwitch, mode)
        table.insert(all_widgets, button)
    end
end

function _init_keyboard(entities)
    local currentNote
    local playNote = function(_, value)
        if value and currentNote == nil then
            state.instrument.note_on(value)
            currentNote = value
        elseif value and currentNote ~= nil then
            state.instrument.note_on(value)
            state.instrument.note_off(currentNote)
            currentNote = value
        elseif not value then
            state.instrument.note_off(currentNote)
            currentNote = nil
        end
    end

    for k in all(entities["Keyboard"]) do
        local keyboard = widgets:create_keyboard(k)
        wire.listen(keyboard, "value", playNote)
        table.insert(all_widgets, keyboard)
    end
end

function _init()
    all_widgets = {}
    modals_by_name = {}
    map.level("InstrumentEditor")
    local entities = map.entities()
    state.instrument = sfx.instrument(1)

    _init_buttons(entities)
    _init_dropdowns(entities)
    _init_mode_switch(entities)
    _init_keyboard(entities)
end

function _update()
    mouse._update(function() end, function() end, function() end)

    local active_modal
    for _, modal in pairs(modals_by_name) do
        if modal.visible then
            active_modal = modal
            break
        end
    end

    if active_modal then
        active_modal:_update()
    else
        for widget in all(all_widgets) do
            widget:_update()
        end
    end
end

function _draw()
    map.draw()
    for widget in all(all_widgets) do
        widget:_draw()
    end
    -- Draw modals last so they appear on top of everything
    for _, modal in pairs(modals_by_name) do
        modal:_draw()
    end
    mouse._draw()
end
