local widgets = require("widgets")
local mouse = require("mouse")
local ModeSwitch = require("widgets/ModeSwitch")

local EditorBase = {}

-- Widget loading: panels
EditorBase.init_panels = function(entities, all_widgets)
    for p in all(entities["Panel"]) do
        local panel = widgets:create_panel(p)
        table.insert(all_widgets, panel)
    end
end

-- Widget loading: text buttons
-- Returns a table of text buttons keyed by Action field (e.g. { Save = button })
EditorBase.init_text_buttons = function(entities, all_widgets)
    local buttons_by_action = {}
    for tb in all(entities["TextButton"]) do
        local text_button = widgets:create_text_button(tb)
        if text_button.fields and text_button.fields.Action then
            buttons_by_action[text_button.fields.Action] = text_button
        end
        table.insert(all_widgets, text_button)
    end
    return buttons_by_action
end

-- Widget loading: speakers
EditorBase.init_speakers = function(entities, all_widgets, speaker_widgets)
    for s in all(entities["Speaker"]) do
        local speaker = widgets:create_speaker(s)
        table.insert(all_widgets, speaker)
        table.insert(speaker_widgets, speaker)
    end
end

-- Widget loading: mode switch
EditorBase.init_mode_switch = function(entities, all_widgets)
    for mode in all(entities["ModeSwitch"]) do
        local button = new(ModeSwitch, mode)
        table.insert(all_widgets, button)
    end
end

-- Modal creation from Button entities.
-- config.modal_sizes: optional table of { ModalName = {x, y, width, height} }
-- config.on_open: function(modal_name) returning the value to pass to modal:open()
-- config.on_name_validate: function(value) called when NameModal validates
-- Returns modals_by_name table
EditorBase.init_buttons = function(entities, all_widgets, config)
    local modals_by_name = {}
    local default_size = { x = 96, y = 64, width = 192, height = 128 }
    local modal_sizes = config.modal_sizes or {}

    for b in all(entities["Button"]) do
        local button = widgets:create_button(b)

        if button.fields.Modal then
            local modal_name = button.fields.Modal

            if not modals_by_name[modal_name] then
                local size = modal_sizes[modal_name] or default_size
                local modal = widgets:create_modal({
                    x = size.x,
                    y = size.y,
                    width = size.width,
                    height = size.height,
                    level_name = modal_name,
                    fields = {},
                })
                modals_by_name[modal_name] = modal
            end

            button.on_change = function()
                local target = modals_by_name[modal_name]
                if target then
                    target:open(config.on_open(modal_name))
                end
            end
        end

        table.insert(all_widgets, button)
    end

    -- Wire NameModal with dropdown rename
    local name_modal = modals_by_name["NameModal"]
    if name_modal and config.on_name_validate then
        name_modal.on_validate = function(self, value)
            config.on_name_validate(value)
        end
    end

    return modals_by_name
end

-- Dropdown populated from indexed entities.
-- config.count: number of items
-- config.fetch: function(i) returning entity with .name
-- config.label: fallback label prefix (e.g. "Instrument")
-- config.on_select: function(index) called on selection change
-- config.layer_manager: optional, for dropdown overlay management
-- Returns the dropdown widget
EditorBase.init_entity_dropdown = function(entities, all_widgets, config)
    local dropdown_widget = nil

    for d in all(entities["Dropdown"]) do
        local dropdown = widgets:create_dropdown(d)

        -- Match the right dropdown: use width filter if provided, otherwise first empty one
        local is_target = false
        if config.min_width then
            is_target = (dropdown_widget == nil and dropdown.width >= config.min_width)
        else
            is_target = (dropdown_widget == nil and #dropdown.options == 0)
        end

        if is_target then
            if #dropdown.options == 0 then
                for i = 0, config.count - 1 do
                    local entity = config.fetch(i)
                    local name = entity.name or (config.label .. " " .. i)
                    table.insert(dropdown.options, "[" .. i .. "] " .. name)
                end
                dropdown:_init()
            end

            dropdown.on_change = function(self)
                config.on_select(self.selected - 1)
            end

            if config.layer_manager then
                local layer_manager = config.layer_manager
                local original_update = dropdown._update
                dropdown._update = function(self)
                    local was_open = self.open
                    original_update(self)
                    if self.open and not was_open then
                        layer_manager:set_overlay(self)
                    elseif not self.open and was_open then
                        layer_manager:set_overlay(nil)
                    end
                end
            end

            dropdown_widget = dropdown
        end

        table.insert(all_widgets, dropdown)
    end

    return dropdown_widget
end

-- Update the dropdown display after a rename
EditorBase.update_dropdown_name = function(dropdown_widget, value)
    if dropdown_widget then
        local idx = dropdown_widget.selected
        dropdown_widget.options[idx] = "[" .. (idx - 1) .. "] " .. value
        dropdown_widget:_init()
    end
end

-- Save dirty tracking: wraps on_change on all widgets and on_validate on modals
-- to detect unsaved changes. save_button.on_change triggers save.
-- Returns a state table { dirty, next_shake_time }
EditorBase.init_save_reminder = function(all_widgets, save_button, modals_by_name)
    local save_state = { dirty = false, next_shake_time = 0 }

    local mark_dirty = function()
        save_state.dirty = true
        save_state.next_shake_time = tiny.t + 15
    end

    save_button.on_change = function()
        sfx.save()
        save_state.dirty = false
    end

    for _, w in ipairs(all_widgets) do
        if w ~= save_button then
            local orig = w.on_change
            if orig then
                w.on_change = function(self, ...)
                    mark_dirty()
                    return orig(self, ...)
                end
            end
        end
    end

    for _, modal in pairs(modals_by_name) do
        local orig_validate = modal.on_validate
        if orig_validate then
            modal.on_validate = function(self, ...)
                mark_dirty()
                return orig_validate(self, ...)
            end
        end
    end

    return save_state
end

-- Update save reminder: shakes the save button periodically when dirty
EditorBase.update_save_reminder = function(save_button, save_state)
    if save_state.dirty and save_button and tiny.t >= save_state.next_shake_time then
        save_button:shake()
        save_state.next_shake_time = tiny.t + 15
    end
end

-- Update loop: mouse + modal-or-widgets
EditorBase.update = function(modals_by_name, update_widgets_fn)
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
        update_widgets_fn()
    end
end

-- Draw loop: cls + background + widgets + modals + mouse
EditorBase.draw = function(draw_widgets_fn, modals_by_name)
    gfx.cls()
    map.draw("Background")
    draw_widgets_fn()
    for _, modal in pairs(modals_by_name) do
        modal:_draw()
    end
    mouse._draw()
end

return EditorBase
