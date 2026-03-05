local widgets = require("widgets")
local wire = require("wire")
local EditorBase = require("editor-base")

local all_widgets = {}
local modals_by_name = {}
local speaker_widgets = {}
local overlay_widget = nil

local save_state = nil
local save_button_ref = nil

local state = {
    seq = nil,
}

local function wrap_dropdown_overlay(dropdown)
    local original_update = dropdown._update
    dropdown._update = function(self)
        local was_open = self.open
        original_update(self)
        if self.open and not was_open then
            overlay_widget = self
        elseif not self.open and was_open then
            if overlay_widget == self then
                overlay_widget = nil
            end
        end
    end
end

function _init_fader(entities)
    for f in all(entities["Fader"]) do
        local fader = widgets:create_fader(f)
        table.insert(all_widgets, fader)
    end
end

function _init_counter(entities)
    for c in all(entities["Counter"]) do
        local counter = widgets:create_counter(c)
        table.insert(all_widgets, counter)
    end
end

function _init()
    all_widgets = {}
    modals_by_name = {}
    speaker_widgets = {}
    overlay_widget = nil
    save_state = nil
    save_button_ref = nil

    map.level("MusicEditor")

    state.seq = sfx.sequence(0)

    -- Panels first (drawn behind everything)
    local panel_entities = map.entities("Panels")
    EditorBase.init_panels(panel_entities, all_widgets)

    -- Then all interactive widgets
    local widget_entities = map.entities("Widgets")

    local buttons_by_action = EditorBase.init_text_buttons(widget_entities, all_widgets)
    save_button_ref = buttons_by_action["Save"]

    EditorBase.init_speakers(widget_entities, all_widgets, speaker_widgets)
    EditorBase.init_mode_switch(widget_entities, all_widgets)

    modals_by_name = EditorBase.init_buttons(widget_entities, all_widgets, {
        on_open = function() return "" end,
    })

    -- Create all dropdowns and wrap with overlay
    for d in all(widget_entities["Dropdown"]) do
        local dropdown = widgets:create_dropdown(d)
        wrap_dropdown_overlay(dropdown)
        table.insert(all_widgets, dropdown)
    end

    _init_fader(widget_entities)
    _init_counter(widget_entities)

    save_state = EditorBase.init_save_reminder(all_widgets, save_button_ref, modals_by_name)
end

function _update()
    EditorBase.update(modals_by_name, function()
        if overlay_widget then
            overlay_widget:_update()
        else
            for w in all(all_widgets) do
                w:_update()
            end
        end
    end)

    EditorBase.update_save_reminder(save_button_ref, save_state)
end

function _draw()
    EditorBase.draw(function()
        for w in all(all_widgets) do
            if w ~= overlay_widget then
                w:_draw()
            end
        end
        if overlay_widget then
            overlay_widget:_draw()
        end
    end, modals_by_name)
end
