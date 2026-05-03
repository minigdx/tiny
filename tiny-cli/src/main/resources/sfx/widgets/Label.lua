local Label = {
    x = 0,
    y = 0,
    width = 8,
    height = 8,
    text = "",
}

Label._init = function(self)
    self.text = (self.fields and self.fields.Default) or ""
end

Label._update = function(self)
end

Label._draw = function(self)
    if self.text and #self.text > 0 then
        text.font("monogram")
        text.print(self.text, self.x, self.y, 1)
        text.font()
    end
end

return Label
