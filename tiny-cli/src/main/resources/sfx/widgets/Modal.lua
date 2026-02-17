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
                end
            end
        end
    end

    map.level(previous_level)
end

Modal.open = function(self)
    self.visible = true
end

Modal.close = function(self)
    self.visible = false
    if self.on_cancel then
        self:on_cancel()
    end
end

Modal.validate = function(self)
    self.visible = false
    if self.on_validate then
        self:on_validate()
    end
    self:fire_on_update(self)
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

    -- Click outside modal → close
    if not inside_widget(self, pos.x, pos.y) then
        self:close()
        return
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

    -- Draw child widgets
    for w in all(self.widgets) do
        w:_draw()
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
