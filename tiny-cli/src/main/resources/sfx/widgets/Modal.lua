local utils = require("widgets.utils")
local inside_widget = utils.inside_widget

local widget_factories = {
    Knob = "create_knob",
    Button = "create_button",
    Fader = "create_fader",
    Checkbox = "create_checkbox",
    Envelop = "create_envelop",
    Keyboard = "create_keyboard",
    Help = "create_help",
    MenuItem = "create_menu_item",
    Dropdown = "create_dropdown",
    TextInput = "create_text_input",
    TextButton = "create_text_button",
}

local Modal = {
    x = 0,
    y = 0,
    width = 128,
    height = 128,
    level_name = nil,
    visible = false,
    widgets = {},
    close_button = { x = 0, y = 0, width = 7, height = 7 },
    border_color = 18,
    background_color = 1,
    listeners = {},
    on_update = utils.on_update,
    fire_on_update = utils.fire_on_update,
}

Modal._init = function(self, widget_factory)
    self.close_button = {
        x = self.x + self.width - 11,
        y = self.y + 4,
        width = 7,
        height = 7,
    }

    if self.level_name and widget_factory then
        self:_load_widgets(widget_factory)
    end
end

Modal._load_widgets = function(self, widget_factory)
    self.widgets = {}
    self.text_input = nil
    self.dropdown = nil
    local buttons = {}

    local previous_level = map.level()
    map.level(self.level_name)
    local entities = map.entities()

    for entity_type, factory_method in pairs(widget_factories) do
        if entities[entity_type] then
            for entity in all(entities[entity_type]) do
                if widget_factory[factory_method] then
                    local widget = widget_factory[factory_method](widget_factory, entity)
                    widget.x = widget.x + self.x
                    widget.y = widget.y + self.y
                    table.insert(self.widgets, widget)

                    if entity_type == "TextInput" and not self.text_input then
                        self.text_input = widget
                    elseif entity_type == "Dropdown" and not self.dropdown then
                        self.dropdown = widget
                    elseif entity_type == "Button" then
                        table.insert(buttons, widget)
                    elseif entity_type == "TextButton" then
                        table.insert(buttons, widget)
                    end
                end
            end
        end
    end

    -- Wire buttons in the modal to validate
    for _, button in ipairs(buttons) do
        button.on_change = function()
            self:validate()
        end
    end

    map.level(previous_level)
end

Modal.open = function(self, initial_value)
    self.visible = true
    if initial_value and self.text_input then
        self.text_input:set_value(initial_value)
        self.text_input.focused = true
    end
end

Modal.close = function(self)
    self.visible = false
    if self.on_cancel then
        self:on_cancel()
    end
end

Modal.validate = function(self)
    self.visible = false
    local value = nil
    if self.text_input then
        value = self.text_input.value
    end
    local dropdown_value = nil
    if self.dropdown then
        dropdown_value = self.dropdown.selected
    end
    if self.on_validate then
        self:on_validate(value, dropdown_value)
    end
    self:fire_on_update(value)
end

Modal._update = function(self)
    if not self.visible then
        return
    end

    -- Update child widgets
    for w in all(self.widgets) do
        w:_update()
    end

    local pos = ctrl.touched(0)
    if pos == nil then
        return
    end

    -- Close button click
    if inside_widget(self.close_button, pos.x, pos.y) then
        self:close()
        return
    end

    -- Click outside modal → close (unless dropdown is open)
    if not inside_widget(self, pos.x, pos.y) then
        if self.dropdown and self.dropdown.open then
            -- Let dropdown handle the click
        else
            self:close()
            return
        end
    end
end

Modal._draw = function(self)
    if not self.visible then
        return
    end

    -- Draw the modal level as background
    if self.level_name then
        local previous_level = map.level()
        map.level(self.level_name)
        gfx.camera(-self.x, -self.y)
        map.draw()
        gfx.camera()
        map.level(previous_level)
    end

    -- Draw child widgets (open dropdown drawn last so it renders on top)
    local open_dropdown = nil
    for w in all(self.widgets) do
        if w == self.dropdown and self.dropdown.open then
            open_dropdown = w
        else
            w:_draw()
        end
    end
    if open_dropdown then
        open_dropdown:_draw()
    end

    -- Close button (X) in top-right corner
    local bx = self.close_button.x
    local by = self.close_button.y
    local bw = self.close_button.width
    local bh = self.close_button.height
    shape.rect(bx, by, bw, bh, self.border_color)
    shape.line(bx + 2, by + 2, bx + bw - 2, by + bh - 2, self.border_color)
    shape.line(bx + bw - 2, by + 2, bx + 2, by + bh - 2, self.border_color)
end

return Modal
