local LayerManager = {}

LayerManager.create = function()
    return {
        layers = {},       -- ordered list of {name, tiles, widgets, always}
        active = nil,      -- name of the active layer
        overlay = nil,     -- widget drawn last (e.g., open dropdown)

        register = function(self, name, opts)
            table.insert(self.layers, {
                name = name,
                tiles = opts.tiles,
                widgets = opts.widgets or {},
                always = opts.always or false,
            })
        end,

        switch = function(self, name)
            self.active = name
        end,

        set_overlay = function(self, widget)
            self.overlay = widget
        end,

        draw_base = function(self)
            for _, layer in ipairs(self.layers) do
                if layer.always then
                    if layer.tiles then
                        map.draw(layer.tiles)
                    end
                    for widget in all(layer.widgets) do
                        if widget ~= self.overlay then
                            widget:_draw()
                        end
                    end
                end
            end
        end,

        draw_active = function(self)
            for _, layer in ipairs(self.layers) do
                if not layer.always and layer.name == self.active then
                    if layer.tiles then
                        map.draw(layer.tiles)
                    end
                    for widget in all(layer.widgets) do
                        if widget ~= self.overlay then
                            widget:_draw()
                        end
                    end
                end
            end
            if self.overlay then
                self.overlay:_draw()
            end
        end,

        update_widgets = function(self)
            for _, layer in ipairs(self.layers) do
                if layer.always or layer.name == self.active then
                    for widget in all(layer.widgets) do
                        widget:_update()
                    end
                end
            end
        end,
    }
end

return LayerManager
