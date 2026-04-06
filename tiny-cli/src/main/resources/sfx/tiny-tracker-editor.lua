local widgets = require("widgets")
local EditorBase = require("editor-base")

local all_widgets = {}
local modals_by_name = {}
local save_state = nil
local save_button_ref = nil
local tracker_widget = nil

function _init()
    all_widgets = {}
    modals_by_name = {}
    save_state = nil
    save_button_ref = nil
    tracker_widget = nil

    map.level("TrackerEditor")

    -- Panels first (drawn behind everything)
    local panel_entities = map.entities("Panels")
    EditorBase.init_panels(panel_entities, all_widgets)

    -- Interactive widgets
    local widget_entities = map.entities("Widgets")

    local buttons_by_action = EditorBase.init_text_buttons(widget_entities, all_widgets)
    save_button_ref = buttons_by_action["Save"]

    -- Create tracker widget from entity
    for t in all(widget_entities["Tracker"]) do
        tracker_widget = widgets:create_tracker(t)
        table.insert(all_widgets, tracker_widget)
    end

    modals_by_name = EditorBase.init_buttons(widget_entities, all_widgets, {
        on_open = function()
            return ""
        end,
        on_name_validate = function(value)
        end,
    })

    save_state = EditorBase.init_save_reminder(all_widgets, save_button_ref, modals_by_name)
end

function _update()
    -- TAB switches back to music editor
    if ctrl.pressed(keys.tab) then
        tiny.exit("tiny-music-editor.lua")
        return
    end

    EditorBase.update(modals_by_name, function()
        for w in all(all_widgets) do
            w:_update()
        end
    end)

    EditorBase.update_save_reminder(save_button_ref, save_state)
end

function _draw()
    EditorBase.draw(function()
        for w in all(all_widgets) do
            w:_draw()
        end
    end, modals_by_name)
end
