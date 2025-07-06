-- Test script for conditional breakpoints
local x = 5
local y = 10

-- This line should show: -- 🐛 x > 3 (when condition is valid)
print("x is " .. x)

-- This line should show: -- 💥 z > 5 (error: z is undefined) (when condition fails)
print("y is " .. y)

-- This line should show: -- 🐛 x < y (when condition is valid)
if x < y then
    print("x is less than y")
end

-- This line should show: -- 💥 invalid_var == 10 (error: invalid_var is undefined)
print("End of test")