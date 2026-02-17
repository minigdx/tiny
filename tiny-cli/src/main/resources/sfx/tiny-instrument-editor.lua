local widgets = require("widgets")
local mouse = require("mouse")
local wire = require("wire")
local ModeSwitch = require("widgets/ModeSwitch")

local all_widgets = {}
local modals_by_name = {}
local dropdown_widget = nil

local state = {
    instrument = nil,
}

function _init_buttons(entities)
    for b in all(entities["Button"]) do
        local button = widgets:create_button(b)

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
                    target:open(state.instrument.name)
                end
            end
        end

        table.insert(all_widgets, button)
    end

    -- Wire NameModal's on_validate callback
    local name_modal = modals_by_name["NameModal"]
    if name_modal then
        name_modal.on_validate = function(self, value)
            if value and state.instrument then
                state.instrument.name = value
                -- Update the dropdown label for the currently selected instrument
                if dropdown_widget then
                    local idx = dropdown_widget.selected
                    dropdown_widget.options[idx] = "[" .. (idx - 1) .. "] " .. value
                    dropdown_widget:_init()
                end
            end
        end
    end
end

function _init_dropdowns(entities)
    for d in all(entities["Dropdown"]) do
        local dropdown = widgets:create_dropdown(d)

        if #dropdown.options == 0 then
            for i = 0, 7 do
                local instr = sfx.instrument(i)
                local name = instr.name or ("Instrument " .. i)
                table.insert(dropdown.options, "[" .. i .. "] " .. name)
            end
            dropdown:_init()
        end

        dropdown.on_change = function(self)
            state.instrument = sfx.instrument(self.selected - 1)
        end

        dropdown_widget = dropdown
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
    dropdown_widget = nil
    map.level("InstrumentEditor")
    local entities = map.entities()
    state.instrument = sfx.instrument(0)

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
