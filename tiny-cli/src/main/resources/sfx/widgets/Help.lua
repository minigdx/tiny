local Help = {
    _type = "Help",
    label = ""
}

Help._update = function(self)

end

Help._draw = function(self)
    print(self.label, self.x, self.y + 2)
    shape.rect(self.x, self.y, self.width, self.height)
end

return Help