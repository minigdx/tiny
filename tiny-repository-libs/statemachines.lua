--[[
    State machine library

    Allow to manage states through a machine.
    For example, you can create a shape machine.
    This machine can has multiple states:

    - circle (which draw a circle)
    - square (which draw a square).

    When the key space is pressed in any of those state,
    it switch to another state.

state_machine = require("statemachines")

function _init()
    state_machine:addState("square", {
        _draw = function()
            shape.rectf(128, 128, 64, 64, 1)
        end,

        _update = function()
            if(ctrl.pressed(keys.space)) then
                return "circle"
            end
        end
    })

    state_machine:addState("circle", {
        _draw = function()
            shape.circlef(128, 128, 64, 1)
        end,

        _update = function()
            if(ctrl.pressed(keys.space)) then
                return "square"
            end
        end
    })

    state_machine:init("square")
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
    onEnter = function() end, -- method called when the state is entered
    onLeave = function() end, -- method called when the state is leaved
    _update = function() end, -- method called on each update
    _draw = function() end -- method called on each draw
}

local StateMachine = {
    states = {},
    current = nil,
}

-- Add a new state to the current state machine
function StateMachine:addState(name, options)
    local state = new(State, {
        name = name,
        onEnter = options.onEnter or function() end,
        onLeave = options.onLeave or function() end,
        _update = options._update or function() end,
        _draw = options._draw or function() end,
    })

    self.states[name] = state

    return state
end

-- Set the initial state of the state machine
function StateMachine:init(name)
    if selt.current == nil then
        self.current = name
    end
end

function StateMachine:_update()
    if self.current then
        local currentState = self.states[self.current]
        local newState = self.states[self.current]:_update()
        if newState then
            currentState:onLeave()
            self.current = newState
            currentState = self.states[self.current]:onEnter()
        end
    end
end

function StateMachine:_draw()
    if self.current then
        self.states[self.current]:_draw()
    end
end

return new(StateMachine)
