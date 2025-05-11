local wire = {}

wire.set_nested_value = function(target, path, value)
    local current_table = target

    for i = 1, #path - 1 do
        local key = path[i]
        current_table = current_table[key]
    end

    local final_key = path[#path]
    current_table[final_key] = value
end

wire.get_nested_value = function(source, path)
    local current_table = source

    for i = 1, #path - 1 do
        local key = path[i]
        current_table = current_table[key]
    end

    local final_key = path[#path]
    return current_table[final_key]
end

wire.produce_to = function(source, spath, target, tpath, conv)
    local old_on_change = source.on_change

    source.on_change = function(self)
        local value = wire.get_nested_value(source, spath)
        if conv then
            value = conv(source, target, value)
        end
        wire.set_nested_value(target, tpath, value)
        if old_on_change then
            old_on_change(self)
        end
    end
end

wire.listen_to = function(source, spath, conv)
    local old_on_change = source.on_change

    source.on_change = function(self)
        local value = wire.get_nested_value(source, spath)
        if conv then
            conv(source, value)
        end

        if old_on_change then
            old_on_change(self)
        end
    end
end

wire.find_widget = function(widgets_set, ref)
    for wid in all(widgets_set) do
        if wid.iid == ref.entityIid then
            return wid
        end
    end
end


--- Get the latest value from the source.spath in the _update() loop
--- and set in in target.tpath
wire.consume_on_update = function(target, tpath, source, spath, conv)
    local old_update = target._update
    target._update = function(self)
        local value = wire.get_nested_value(source, spath)
        if conv then
            value = conv(source, target, value)
        end
        wire.set_nested_value(self, tpath, value)
        old_update(target)
    end
end

return wire