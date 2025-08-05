local wire = {}

-- Parse a string path like "a.b.c" into a table {"a", "b", "c"}
local function parse_path(path)
    if type(path) == "string" then
        local parts = {}
        for part in string.gmatch(path, "[^%.]+") do
            table.insert(parts, part)
        end
        return parts
    end
    return path -- already a table
end

-- Get a nested value from an object using a path
local function get_value(obj, path)
    local parts = parse_path(path)
    local current = obj
    
    for i = 1, #parts do
        current = current[parts[i]]
        if current == nil then
            return nil
        end
    end
    
    return current
end

-- Set a nested value in an object using a path
local function set_value(obj, path, value)
    debug.console("path", path)

    local parts = parse_path(path)
    local current = obj
    
    for i = 1, #parts - 1 do
        current = current[parts[i]]
        if current == nil then
            return
        end
    end
    
    current[parts[#parts]] = value
end

--- Bind two objects together for bidirectional data flow
-- When source changes, target is updated and vice versa
-- @param obj1 First object
-- @param path1 Path in first object (e.g. "player.health")
-- @param obj2 Second object
-- @param path2 Path in second object (e.g. "ui.healthbar.value")
-- @param transform Optional function to transform values (obj_from, obj_to, value) -> transformed_value
wire.bind = function(obj1, path1, obj2, path2, transform)
    -- obj1 -> obj2
    local to_widget = nil
    if transform then
        if type(transform) == "function" then
            to_widget = transform
        elseif transform.to_widget then
            to_widget = transform.to_widget
        end
    end
    wire.sync(obj1, path1, obj2, path2, to_widget, "update")

    -- obj2 -> obj1
    local from_widget = nil
    if transform then
        if type(transform) == "function" then
            -- Create a reverse transform that swaps the object parameters
            from_widget = function(from, to, value)
                return transform(to, from, value)
            end
        elseif transform.from_widget then
            from_widget = transform.from_widget
        end
    end
    wire.sync(obj2, path2, obj1, path1, from_widget)
end

function guessMode(target)
    if(target._update ~= nil) then
        return "update"
    else
        return "change"
    end
end
--- Sync data from source to target
-- Updates target whenever source changes (via on_change) or continuously (via _update)
-- @param source Source object
-- @param source_path Path in source object (e.g. "player.score")
-- @param target Target object
-- @param target_path Path in target object (e.g. "ui.score.text")
-- @param transform Optional transformation function (source, target, value) -> transformed_value
-- @param mode "change" (default) or "update" - how to listen for changes
wire.sync = function(source, source_path, target, target_path, transform, mode)
    mode = mode or guessMode(target)

    local update_target = function()
        local value = get_value(source, source_path)
        if transform then
            value = transform(source, target, value)
        end
        set_value(target, target_path, value)
    end
    
    if mode == "change" then
        -- Listen to on_change events
        local old_on_change = source.on_change
        source.on_change = function(self)
            update_target()
            if old_on_change then
                old_on_change(self)
            end
        end
    else
        -- Update continuously in _update loop
        local old_update = target._update
        target._update = function(self)
            update_target()
            if old_update then
                old_update(self)
            end
        end
    end
end

--- Listen to changes in an object and execute a callback
-- @param source Source object to listen to
-- @param path Path to watch (e.g. "button.clicked")
-- @param callback Function to call when value changes (source, value)
wire.listen = function(source, path, callback)
    local old_on_change = source.on_change
    
    source.on_change = function(self)
        local value = get_value(source, path)
        if callback then
            callback(source, value)
        end
        
        if old_on_change then
            old_on_change(self)
        end
    end
end

--- Find a widget in a collection by entity ID
-- @param widgets Collection of widgets
-- @param ref Reference object with entityIid field
-- @return The widget with matching iid, or nil
wire.find_widget = function(widgets, ref)
    if (not ref) then
        error("find_widget is called without a valid entity ref")
    end

    for widget in all(widgets) do
        if widget.iid == ref.entityIid then
            return widget
        end
    end
    return nil
end

return wire