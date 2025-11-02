local Envelop = {
    label = "",
    value = 0,
    x = 0,
    y = 0,
    width = 128,
    height = 64,
    enabled = true,
    padding = {left = 2, up = 24, down = 4, right = 4},
    -- values --
    attack = 0,
    decay = 0.2,
    sustain = 0.5,
    release = 0,

    attack_end_x = 0,
    attack_end_y = 0,
    decay_end_x = 0,
    decay_end_y = 0,
    release_start_x = 0,
    release_start_y = 0,
    padded_width = 0,
    padded_height = 0,
    padded_x = 0,
    padded_y = 0
}

Envelop._init = function(self)
    self.padded_x = self.x + self.padding.left
    self.padded_y = self.y + self.padding.up
    self.padded_height = self.height - (self.padding.down + self.padding.up)
    self.padded_width = (self.width - (self.padding.left + self.padding.right))
end

Envelop._update = function(self)
    self.decay = math.min(self.decay, 1 - self.attack)
    self.release = math.min(self.release, 1 - (self.decay + self.attack))

    self.attack_end_x = self.padded_x + self.attack * self.padded_width
    self.attack_end_y = self.padded_y

    self.decay_end_x = self.attack_end_x + self.decay * self.padded_width
    self.decay_end_y = self.padded_y + self.padded_height * (1 - self.sustain)

    self.release_start_x = self.padded_x + self.padded_width - self.release * self.padded_width
    self.release_start_y = self.padded_y + self.padded_height * (1 - self.sustain)
end

local blue = 9
local green = 13
local purple = 7
local red = 5

Envelop._draw = function(self)

    -- attack
    shape.line(
        self.padded_x, self.padded_y + self.padded_height,
        self.attack_end_x, self.attack_end_y,
        blue
    )
    shape.circle(self.attack_end_x, self.attack_end_y, 2, blue)

    -- decay
    shape.line(self.attack_end_x, self.attack_end_y, self.decay_end_x, self.decay_end_y, green)
    shape.circle(self.decay_end_x, self.decay_end_y, 2, green)

    -- release
    shape.line(
        self.release_start_x, self.release_start_y,
        self.padded_x + self.padded_width, self.padded_y + self.padded_height,
        red
    )
    shape.circle(self.release_start_x, self.release_start_y, 2, red)

    shape.line(self.decay_end_x, self.decay_end_y, self.release_start_x, self.release_start_y, purple)

    -- sustain
    local width = 8
    local height = 4
    shape.rect(
        self.decay_end_x + (self.release_start_x - self.decay_end_x - width) * 0.5,
        self.padded_y + (1 - self.sustain) * self.padded_height - height * 0.5,
        width,
        height,
        purple
    )

    -- attack limits
    local limit = function(name, x1, x2, color)
        local line_y = self.y + self.padding.up * 0.5

        shape.line(x1, line_y, x2, line_y, color)
        shape.line(x1, line_y, x1, line_y + 2, color)
        shape.line(x2, line_y, x2, line_y + 2, color)

        local w = (x2 - x1)
        local nb_letters = w / 4 -- size of a letter

        print(string.sub(name, 1, nb_letters), x1, line_y - 6, color)
    end

    limit("attack", self.padded_x + 1, self.attack_end_x - 1, blue)
    limit("decay", self.attack_end_x + 1, self.decay_end_x - 1, green)
    limit("sustain", self.decay_end_x + 1, self.release_start_x - 1, purple)
    limit("release", self.release_start_x + 1, self.padded_x + self.padded_width - 1, red)


end

return Envelop