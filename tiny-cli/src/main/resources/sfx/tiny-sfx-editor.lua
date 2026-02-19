local widgets = require("widgets")
local mouse = require("mouse")
local wire = require("wire")
local ModeSwitch = require("widgets/ModeSwitch")
local LayerManager = require("layers")

local all_widgets = {}
local modals_by_name = {}
local dropdown_widget = nil
local layer_manager = nil

local state = {
    sfx = nil,
}

function _init_mode_switch(entities)
    for mode in all(entities["ModeSwitch"]) do
        local button = new(ModeSwitch, mode)
        table.insert(all_widgets, button)
    end
end

function _init_dropdowns(entities)
    for d in all(entities["Dropdown"]) do
        local dropdown = widgets:create_dropdown(d)

        if #dropdown.options == 0 then
            for i = 0, 31 do
                local s = sfx.sfx(i)
                local name = s.name or ("SFX " .. i)
                table.insert(dropdown.options, "[" .. i .. "] " .. name)
            end
            dropdown:_init()
        end

        dropdown.on_change = function(self)
            state.sfx = sfx.sfx(self.selected - 1)
        end

        dropdown_widget = dropdown

        local original_update = dropdown._update
        dropdown._update = function(self)
            local was_open = self.open
            original_update(self)
            if layer_manager then
                if self.open and not was_open then
                    layer_manager:set_overlay(self)
                elseif not self.open and was_open then
                    layer_manager:set_overlay(nil)
                end
            end
        end

        table.insert(all_widgets, dropdown)
    end
end

function _init_buttons(entities)
    for b in all(entities["Button"]) do
        local button = widgets:create_button(b)

        if button.fields.Modal then
            local modal_name = button.fields.Modal

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
                    target:open(state.sfx.name)
                end
            end
        end

        table.insert(all_widgets, button)
    end

    local name_modal = modals_by_name["NameModal"]
    if name_modal then
        name_modal.on_validate = function(self, value)
            if value and state.sfx then
                state.sfx.name = value
                if dropdown_widget then
                    local idx = dropdown_widget.selected
                    dropdown_widget.options[idx] = "[" .. (idx - 1) .. "] " .. value
                    dropdown_widget:_init()
                end
            end
        end
    end
end

function _init()
    all_widgets = {}
    modals_by_name = {}
    dropdown_widget = nil
    layer_manager = nil

    map.level("SfxEditor")
    local widget_entities = map.entities("Widgets")

    state.sfx = sfx.sfx(0)

    _init_mode_switch(widget_entities)
    _init_dropdowns(widget_entities)
    _init_buttons(widget_entities)

    layer_manager = LayerManager.create()
    layer_manager:register("Widgets", { tiles = "WidgetsTiles", widgets = all_widgets, always = true })
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
        layer_manager:update_widgets()
    end
end

function _draw()
    layer_manager:draw_base()
    layer_manager:draw_active()
    for _, modal in pairs(modals_by_name) do
        modal:_draw()
    end
    mouse._draw()
end
