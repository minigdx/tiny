function init()
    index = 0
    -- palette = {0, 13, 2, 4, 8, 9, 15}
    palette = {
        0, 5, 13, 11,
        8, 13, 7, 4,
        9, 10, 7, 7,
        7, 4,  15, 7
    }
    cls()
    for x = 0, 126 do
        for y = 0, 126 do
            pset(x, y, x + y + index)
        end
    end
end

--function draw()
--    index = index + 1
 --   for x = 0, 126 do
  --      for y = 0, 126 do
   --         pset(x, y, x + y + index)
    --    end
    -- end
-- end
