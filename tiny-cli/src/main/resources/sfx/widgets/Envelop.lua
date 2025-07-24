local Envelop = {
    label = "",
    value = 0,
    x = 0,
    y = 0,
    width = 128,
    height = 64,
    enabled = true,
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
    release_start_y = 0
}

Envelop._update = function(self)
    self.decay = math.min(self.decay, 1 - self.attack)
    self.release = math.min(self.release, 1 - (self.decay + self.attack))

    self.attack_end_x = self.x + self.attack * self.width
    self.attack_end_y = self.y

    self.decay_end_x = self.attack_end_x + self.decay * self.width
    self.decay_end_y = self.y + self.height * (1 - self.sustain)

    self.release_start_x = self.x + self.width - self.release * self.width
    self.release_start_y = self.y + self.height * (1 - self.sustain)
end

Envelop._draw = function(self)
    shape.rect(self.x, self.y, self.width + 1, self.height + 1, 9)

    -- attack
    shape.line(self.x, self.y + self.height, self.attack_end_x, self.attack_end_y, 8)
    shape.circle(self.attack_end_x, self.attack_end_y, 2, 8)

    -- decay
    shape.line(self.attack_end_x, self.attack_end_y, self.decay_end_x, self.decay_end_y, 10)
    shape.circle(self.decay_end_x, self.decay_end_y, 2, 10)

    -- release
    shape.line(self.release_start_x, self.release_start_y, self.x + self.width, self.y + self.height, 9)
    shape.circle(self.release_start_x, self.release_start_y, 2, 9)

    shape.line(self.decay_end_x, self.decay_end_y, self.release_start_x, self.release_start_y, 9)

    -- sustain
    local width = 8
    local height = 4
    shape.rect(self.decay_end_x + (self.release_start_x - self.decay_end_x - width) * 0.5, self.y + (1 - self.sustain) * self.height - height * 0.5, width, height, 8)
end

return Envelop