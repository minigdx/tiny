--[[
    State machine library

    Allow to manage states through a machine.
    For example, you can create a shape machine.
    This machine can has multiple states:

    - circle (which draw a circle)
    - square (which draw a square).

    When the key space is pressed in any of those state,
    it switch to another state.

state_machine = require("state_machines")

function _init()
    state_machine:add_state("square", {
        _draw = function()
            shape.rectf(128, 128, 64, 64, 1)
        end,

        _update = function()
            if(ctrl.pressed(keys.space)) then
                return "circle"
            end
        end
    })

    state_machine:add_state("circle", {
        _draw = function()
            shape.circlef(128, 128, 64, 1)
        end,

        _update = function()
            if(ctrl.pressed(keys.space)) then
                return "square"
            end
        end
    })

    state_machine:set_state("square")
end


function _update()
    state_machine:_update()
end


function _draw()
    gfx.cls()
    state_machine:_draw()
end

]]--
local State = {
    name = "", -- name of the state
    on_enter = function()
    end, -- method called when the state is entered
    on_leave = function()
    end, -- method called when the state is leaved
    _update = function()
    end, -- method called on each update
    _draw = function()
    end -- method called on each draw
}

local StateMachine = {
    states = {},
    current = nil,
}

-- Add a new state to the current state machine
function StateMachine:add_state(name, options)
    local state = new(State, {
        name = name,
        on_enter = options.on_enter or function()
        end,
        on_leave = options.on_leave or function()
        end,
        _update = options._update or function()
        end,
        _draw = options._draw or function()
        end,
    })

    self.states[name] = state

    return state
end

-- Set the current state of the state machine.
-- If there is a previous state, will leave it.
function StateMachine:set_state(name)
    if self.current ~= nil then
        local current_state = self.states[self.current]
        current_state:on_leave()
    end
    local state = self.states[name]
    if state ~= nil then
        self.current = name
        self.states[self.current]:on_enter()
    else
        error(name.." state was not configured before. Did you forgot to call add_state?")
    end
end

function StateMachine:_update()
    if self.current then
        local new_state = self.states[self.current]:_update()
        if new_state ~= nil then
            self:set_state(new_state)
        end
    end
end

function StateMachine:_draw()
    if self.current then
        self.states[self.current]:_draw()
    end
end

return new(StateMachine)
