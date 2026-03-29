-- DO NOT EDIT // DO NOT EDIT // DO NOT EDIT // DO NOT EDIT // DO NOT EDIT
-- Tiny stub lua file generated automatically
-- The file is used only to help Lua editors with autocomplete
--
-- An error, an issue? Please consult https://github.com/minigdx/tiny
--
-- COLOR SYSTEM:
--   Color 0 = TRANSPARENT. Never use 0 if you want something visible.
--   Colors 1..N are defined in the _tiny.json configuration file ("colors" array).
--   The first hex color in the array is index 1, the second is index 2, etc.
--   Typical palette example (PICO-8 style, 16 colors):
--     1 = black, 2 = dark blue, 3 = dark purple, 4 = dark green,
--     5 = brown, 6 = dark grey, 7 = light grey, 8 = white,
--     9 = red, 10 = orange, 11 = yellow, 12 = green,
--     13 = blue, 14 = lavender, 15 = pink, 16 = peach
--   Check _tiny.json "colors" array for the actual palette of the current game.
--
-- DEFAULT COLORS:
--   gfx.cls() without arguments clears to the closest color to black (#000000).
--   print() without a color argument uses the closest color to white (#FFFFFF).
--   To ensure visibility: use a color index DIFFERENT from the cls() color.
--   Common safe pattern: gfx.cls(1) then draw with colors >= 2.
--
-- COMMON MISTAKES:
--   WRONG: shape.circlef(10, 10, 10, 0)   -- color 0 is transparent, nothing visible!
--   WRONG: gfx.cls(1) then shape.circlef(10, 10, 10, 1) -- same color as background!
--   RIGHT: gfx.cls(1) then shape.circlef(10, 10, 10, 8) -- visible: white circle on black


--- Vector2 manipulation library.
vec2 = {}
--- Create a vector 2 as a table { x, y }.
--- @overload fun(x: number, y: number): any -- Create a vector 2 as a table { x, y }.
--- @overload fun(vec2: table): any -- Create a vector 2 as a table { x, y } using another vector 2.
vec2.create = function() end
--- Add vector2 to another vector2
--- @overload fun(v1: table, v2: table): any -- Add a vector 2 {x, y} to another vector 2 {x, y}
--- @overload fun(x1: number, y1: number, x2: number, y2: number): any -- Add a destructured vector 2 to another destructured vector 2
vec2.add = function() end
--- Subtract another vector from another vector
--- @overload fun(v1: table, v2: table): any -- Subtract a vector 2 {x, y} from another vector 2 {x, y}
--- @overload fun(x1: number, y1: number, x2: number, y2: number): any -- Subtract a destructured vector 2 from another destructured vector 2
vec2.sub = function() end
--- Dot product between two vectors
--- @overload fun(v1: table, v2: table): any -- Dot product between a vector 2 {x, y} and another vector 2 {x, y}
--- @overload fun(x1: number, y1: number, x2: number, y2: number): any -- Dot product between a destructured vector 2 and another destructured vector 2
vec2.dot = function() end
--- Calculate the magnitude (length) of a vector
--- @overload fun(x: number, y: number): any -- Calculate the magnitude (length) of a vector 2 {x, y}
--- @overload fun(v1: table): any -- Calculate the magnitude (length) of a vector 2 {x, y}
vec2.mag = function() end
--- Normalize a vector
--- @overload fun(x: number, y: number): any -- Normalize a vector 2 {x, y}
--- @overload fun(v1: table): any -- Normalize a vector 2 {x, y}
vec2.nor = function() end
--- Cross product
--- @overload fun(v1: table, v2: table): any -- Cross product between a vector 2 {x, y} and another vector 2 {x, y}
--- @overload fun(x1: number, y1: number, x2: number, y2: number): any -- Cross product between a destructured vector 2 and another destructured vector 2
vec2.crs = function() end
--- Scale a vector
--- @overload fun(x: number, y: number, scl: number): any -- Scale a vector 2 {x, y} using the factor scl
--- @overload fun(v1: table, scl: number): any -- Scale a vector 2 {x, y} using the factor scl
vec2.scl = function() end


--- Math functions. Please note that standard Lua math methods are also available.
math = {}
--- value of pi (~3.14)
math.pi = any
--- positive infinity value.
math.huge = any
--- Return the sign of the number: -1 if negative. 1 otherwise.
--- @overload fun(number: number): any -- Return the sign of the number.
math.sign = function() end
--- Calculate the angle in radians between the positive x-axis and the point (x, y).
--- @overload fun(y: number, x: number): any -- Calculate the angle for the point (x, y). Please note the argument order: y then x.
math.atan2 = function() end
--- Clamp the value between 2 values.
--- @overload fun(a: number, value: number, b: number): any -- Clamp the value between a and b. If a is greater than b, then b will be returned.
math.clamp = function() end
--- Compute the distance between two points.
--- @overload fun(x1: number, y1: number, x2: number, y2: number): any -- Distance between (x1, y1) and (x2, y2).
math.dst = function() end
--- Compute the distance between two points not squared. Use this method to know if an coordinate is closer than another.
--- @overload fun(x1: number, y1: number, x2: number, y2: number): any -- Distance not squared between (x1, y1) and (x2, y2).
math.dst2 = function() end
--- Generate random values
--- @overload fun(): any -- Generate a random int (negative or positive value)
--- @overload fun(until: any): any -- Generate a random value between 1 until the argument. If a table is passed, it'll return a random element of the table.
--- @overload fun(a: number, b: number): any -- Generate a random value between a and b.
math.rnd = function() end
--- Check if two (r)ectangles overlaps.
--- @overload fun(rect1: table, rect2: table): any -- Check if the rectangle rect1 overlaps with the rectangle rect2.
math.roverlap = function() end
--- Perlin noise. The random generated value is between 0.0 and 1.0.
--- @overload fun(x: number, y: number, z: number): any -- Generate a random value regarding the parameters x,y and z.
math.perlin = function() end


--- Sprite API to draw or update sprites.
spr = {}
--- Get the color index at the coordinate (x,y) from the current spritesheet.
--- @overload fun(x: number, y: number): any -- get the color index at the coordinate (x,y) from the current spritesheet.
spr.pget = function() end
--- Set the color index at the coordinate (x,y) in the current spritesheet.
--- @overload fun(x: number, y: number, color: number): any -- Set the color index at the coordinate (x,y) in the current spritesheet.
spr.pset = function() end
--- Switch to another spritesheet. The index of the spritesheet is given by it's position in the spritesheets field from the `_tiny.json` file.The first spritesheet is at the index 0. It retuns the previous spritesheet. The spritesheet can also be referenced by its filename.
--- @overload fun(): any -- Switch to the first spritesheet
--- @overload fun(spritesheetN: any): any -- Switch to the N spritesheet
spr.sheet = function() end
--- S(uper) Draw a fragment from the spritesheet.
--- @overload fun(): any -- Draw the full spritesheet at default coordinate (0, 0)
--- @overload fun(x: number, y: number): any -- Draw the full spritesheet at coordinate (x, y)
--- @overload fun(x: number, y: number, sprX: number, sprY: number): any -- Draw the full spritesheet at coordinate (x, y) from the sprite (sprX, sprY)
--- @overload fun(x: number, y: number, sprX: number, sprY: number, width: number, height: number, flipX: boolean, flipY: boolean): any -- Draw a fragment from the spritesheet at the coordinate (x, y) from the sprite (sprX, sprY) with the width and height.
spr.sdraw = function() end
--- Draw a sprite.
--- @overload fun(sprN: any): any -- Draw a sprite at the default coordinate (0, 0).
--- @overload fun(sprN: any, x: any, y: any): any -- Draw a sprite.
--- @overload fun(sprN: number, x: number, y: number, flipX: boolean, flipY: boolean): any -- Draw a sprite and allow flip on x or y axis.
spr.draw = function() end


--- Tiny Lib which offer offer the current frame (`tiny.frame`), the current time (`tiny.time`), delta time (`tiny.dt`), game dimensions (`tiny.width`, `tiny.height`), platform information (`tiny.platform`) and to switch to another script using `exit`.
tiny = {}
--- Delta time between two frame. As Tiny is a fixed frame engine, it's always equal to 1/60
tiny.dt = any
--- Time elapsed since the start of the game.
tiny.t = any
--- Number of frames elapsed since the start of the game.
tiny.frame = any
--- Width of the game in pixels.
tiny.width = any
--- Height of the game in pixels.
tiny.height = any
--- Current platform: 1 for desktop, 2 for web.
tiny.platform = any
--- Exit the actual script to switch to another one. The next script to use is identified by it's index. The index of the script is the index of it in the list of scripts from the `_tiny.json` file.The first script is at the index 0.
--- @overload fun(scriptIndex: any): any -- Exit the actual script to switch to another one.
tiny.exit = function() end


--- Helpers to log information in the console.
console = {}
--- Log a message into the console.
--- @overload fun(str: any): any -- Log a message into the console.
console.log = function() end


--- Access map created with LDTk ( https://ldtk.io/ ).
map = {}
--- Set the current level to use.
--- @overload fun(): any -- Return the index of the current level.
--- @overload fun(level: any): any -- Set the current level to use. The level can be an index, the name or the id defined by LDTK. Return the previous index level or NIL if the new level is invalid.
map.level = function() end
--- Get the list of layers from the actual level.
--- @overload fun(layer_index: any): any -- Get the layer at the specified index or name from the actual level. The layer in the front is 0.
--- @overload fun(): any -- Get the list of layers from the actual level.
map.layer = function() end
--- Convert cell coordinates cx, cy into map screen coordinates x, y.
--- @overload fun(arg1: any, arg2: any): any -- Convert the cell coordinates into coordinates as a table [x,y].
--- @overload fun(cell: table): any -- Convert the cell coordinates from a table {cx,cy} into screen coordinates as a table {x,y}.
map.from = function() end
--- Convert screen coordinates x, y into map cell coordinates {cx, cy}.
--- For example, coordinates of the player can be converted to cell coordinates to access the flag of the tile matching the player coordinates.
--- @overload fun(x: any, y: any): any -- Convert the coordinates into cell coordinates as a table {cx = cx,cy = cy}.
--- @overload fun(coordinates: table): any -- Convert the coordinates from a table {x,y} into cell coordinates as a table {cx,cy}.
map.to = function() end
--- Get the flag from a tile, using cell coordinates.
--- @overload fun(cx: number, cy: number): any -- Get the flag from the tile at the coordinate cx,cy.
--- @overload fun(cx: number, cy: number, layer: any): any -- Get the flag from the tile at the coordinate cx,cy from a specific layer.
map.cflag = function() end
--- Get the flag from a tile, using screen coordinates.
--- @overload fun(x: number, y: number): any -- Get the flag from the tile at the coordinate x,y.
--- @overload fun(x: number, y: number, layer: any): any -- Get the flag from the tile at the coordinate x,y from a specific layer.
map.flag = function() end
--- Table with all entities by type (ie: `map.entities["player"]`).
---             
--- ```
--- local entities = map.entities()
--- local players = entities["Player"]
--- for entity in all(players) do 
---     shape.rectf(entity.x, entity.y, entity.width, entity.height, 8) -- display an entity using a rectangle
--- end
--- [...]
--- entity.fields -- access custom field of the entity
--- ```
---         
--- @overload fun(): any -- Get all entities from all entities layer as a table, with an entry per type.
--- @overload fun(a: any): any -- Get all entities from the specific layer as a table, with an entry per type.
map.entities = function() end
--- Draw map tiles on the screen.
--- @overload fun(): any -- Draw all active layers on the screen.
--- @overload fun(index: any): any -- Draw the layer with the name or the index on the screen.
map.draw = function() end


--- Sound API to play/loop/stop a sound.
--- A sound can be created using the sound editor, using the command line `tiny-cli sfx <filename>`.
--- 
--- WARNING: Because of browser behaviour, a sound can *only* be played only after the first
--- user interaction.
--- 
--- Avoid to start a music or a sound at the beginning of the game.
--- Before it, force the player to hit a key or click by adding an interactive menu
--- or by starting the sound as soon as the player is moving.
--- 
sound = {}
--- Play a sfx.
--- @overload fun(sfx_index: number, loop: boolean): any -- Play a sfx at sfx_index. The sfx can be looped.
sound.sfx = function() end
--- Play a music
--- @overload fun(music_index: number, loop: boolean): any -- Play the music at the index music_index. The music can be looped.
sound.music = function() end
--- Play a note by an instrument until it's stopped
--- @overload fun(note_name: string, instrument_index: number): any -- Play the note note_name using the instrument at instrument_index
sound.note = function() end


--- Standard library.
--- Create new instance of a class by creating a new table and setting the metatable. It allow to create kind of Object Oriented Programming.
--- 
---  
--- @overload fun(class: table): any -- Create new instance of class.
--- @overload fun(class: table, default: table): any -- Create new instance of class using default values.
function new() end
--- Add *all key/value* from the table `source` to the table `dest`.
--- @overload fun(source: table, dest: table): any -- Merge source into dest.
function merge() end
--- Append *all values* from the table `source` to the table `dest`.
--- @overload fun(source: table, dest: table): any -- Copy source into dest.
function append() end
--- Iterate over values of a table.
--- 
--- - If you want to iterate over keys, use `pairs(table)`.
---  - If you want to iterate over index, use `ipairs(table)`.
---  - If you want to iterate in reverse, use `rpairs(table)`.
--- 
--- @overload fun(table: table): any -- Iterate over the values of the table
function all() end
--- Iterate over values of a table in reverse order. The iterator return an index and the value. The method is useful to remove elements from a table while iterating on it.
--- @overload fun(table: table): any -- Iterate over the values of the table
function rpairs() end
--- Print on the screen a string. Default color is the closest to white (#FFFFFF) in the palette. To ensure visibility, use a color that contrasts with the cls() background color.
--- @overload fun(str: string): any -- print on the screen a string at (0,0) with the default color (closest to white).
--- @overload fun(str: string, x: number, y: number): any -- print on the screen a string with the default color (closest to white).
--- @overload fun(str: string, x: number, y: number, color: number): any -- print on the screen a string with a specific color index (1 to N).
function print() end


--- Floppy allow you to get or save user Lua structure.
floppy = {}
--- Save the content into a local file, on desktop or in the local storage on the web platform.
--- @overload fun(name: string, content: any): any -- Save the content into the file name.
floppy.put = function() end
--- Load and get the content of the file name
--- @overload fun(name: string): any -- Load and get the content of the file name
floppy.get = function() end


--- Access to controllers like touch/mouse events or accessing which key is pressed by the user.
ctrl = {}
--- Get coordinates of the current touch/mouse. If the mouse/touch is out-of the screen, the coordinates will be the last mouse position/touch. The function return those coordinates as a table {x, y}. A sprite can be draw directly on the mouse position by passing the sprite number. 
--- @overload fun(): any -- Get the mouse coordinates.
--- @overload fun(sprN: number): any -- Get the mouse coordinate and draw a sprite on those coordinates.
ctrl.touch = function() end
--- Return true if the key was pressed during the last frame. When called without argument, return a table of all keys pressed during the last frame (values matching the keys lib), or false if no key was pressed. If you need to check that the key is still pressed, see `ctrl.pressing` instead.
--- @overload fun(): any -- Get all keys just pressed as a table of key values, or false if none.
--- @overload fun(key: number): any -- Is the key was pressed?
ctrl.pressed = function() end
--- Return true if the key is still pressed. 
--- @overload fun(key: number): any -- Is the key is still pressed?
ctrl.pressing = function() end
--- Return the position of the touch (as `{x, y}`)if the screen was touched or the mouse button was pressed during the last frame. `nil` otherwise.
--- The touch can be : 
--- 
--- - 0: left click or one finger
--- - 1: right click or two fingers
--- - 2: middle click or three fingers
--- 
--- If you need to check that the touch/mouse button is still active, see `ctrl.touching` instead.
--- @overload fun(touch: number): any -- Is the screen was touched or mouse button was pressed?
ctrl.touched = function() end
--- Return the position of the touch (as `{x, y}`)if the screen is still touched or the mouse button is still pressed. `nil` otherwise.
--- The touch can be : 
--- 
--- - 0: left click or one finger
--- - 1: right click or two fingers
--- - 2: middle click or three fingers
--- 
--- 
--- @overload fun(touch: number): any -- Is the screen is still touched or mouse button is still pressed?
ctrl.touching = function() end


--- Access to graphical API like updating the color palette or applying a dithering pattern.
gfx = {}
--- Switch to another draw mode. 
--- 
--- - 0: default.
---  - 1: drawing with transparent (ie: can erase part of the screen)
---   - 2: drawing a stencil that will be use with the next mode
---   - 3: drawing using a stencil test (ie: drawing only in the stencil)
---   - 4: drawing using a stencil test (ie: drawing everywhere except in the stencil)
--- 
--- @overload fun(): any -- Return the actual mode. Switch back to the default mode.
--- @overload fun(mode: number): any -- Switch to another draw mode. Return the previous mode.
gfx.draw_mode = function() end
--- Clear the screen. When called without arguments, clears with the color closest to black (#000000) in the palette. To ensure visibility, always draw with a color index DIFFERENT from the cls() color.
--- @overload fun(): any -- Clear the screen with the color closest to black (#000000) in the palette.
--- @overload fun(color: number): any -- Clear the screen with the given color index (1 to N). Color 0 clears to transparent.
gfx.cls = function() end
--- Set the color index at the coordinate (x,y).
--- @overload fun(x: number, y: number, color: number): any -- set the color index at the coordinate (x,y).
gfx.pset = function() end
--- Get the color index at the coordinate (x,y).
--- @overload fun(x: number, y: number): any -- get the color index at the coordinate (x,y).
gfx.pget = function() end
--- Transform the current frame buffer into a spritesheeet. 
--- 
--- - If the index of the spritesheet already exist, the spritesheet will be replaced
--- - If the index of the spritesheet doesn't exist, a new spritesheet at this index will be created
--- - If the index of the spritesheet is negative, a new spritesheet will be created at the last positive index.
--- 
--- @overload fun(sheet: any): any -- Copy the current frame buffer to an new or existing sheet index.
gfx.to_sheet = function() end
--- Change a color from the palette to another color.
--- @overload fun(): any -- Reset all previous color changes.
--- @overload fun(a: number, b: number): any -- Replace the color a for the color b.
gfx.pal = function() end
--- Move the game camera.
--- @overload fun(): any -- Reset the game camera to it's default position (0,0).
--- @overload fun(x: number, y: number): any -- Set game camera to the position x, y.
gfx.camera = function() end
--- Apply a dithering pattern on every new draw call. The pattern is using the bits value of a 2 octet value. The first bits is the one on the far left and represent the pixel of the top left of a 4x4 matrix. The last bit is the pixel from the bottom right of this matrix.
--- @overload fun(): any -- Reset dithering pattern. The previous dithering pattern is returned.
--- @overload fun(pattern: number): any -- Apply dithering pattern. The previous dithering pattern is returned.
gfx.dither = function() end
--- Clip the draw surface (ie: limit the drawing area).
--- @overload fun(): any -- Reset the clip and draw on the fullscreen.
--- @overload fun(x: number, y: number, width: number, height: number): any -- Clip and limit the drawing area.
gfx.clip = function() end


--- Easing functions to 'juice' a game. Interpolation to juice your game.
--- All interpolations available: 
--- 
--- - pow2, pow3, pow4, pow5,
--- - powIn2, powIn3, powIn4, powIn5,
--- - powOut2, powOut3, powOut4, powOut5,
--- - sine, sineIn, sineOut,
--- - circle, circleIn, circleOut,
--- - elastic, elasticIn, elasticOut,
--- - swing, swingIn, swingOut,
--- - bounce, bounceIn, bounceOut,
--- - exp10, expIn10, expOut10,
--- - exp5, expIn5, expOut5,
--- - linear 
juice = {}
--- 
--- @overload fun(progress: number): any -- Give a percentage (progress) of the interpolation
--- @overload fun(start: number, end: number, progress: number): any -- Interpolate the value given a start and an end value.
juice.pow2 = function() end
--- 
--- @overload fun(progress: number): any -- Give a percentage (progress) of the interpolation
--- @overload fun(start: number, end: number, progress: number): any -- Interpolate the value given a start and an end value.
juice.pow3 = function() end
--- 
--- @overload fun(progress: number): any -- Give a percentage (progress) of the interpolation
--- @overload fun(start: number, end: number, progress: number): any -- Interpolate the value given a start and an end value.
juice.pow4 = function() end
--- 
--- @overload fun(progress: number): any -- Give a percentage (progress) of the interpolation
--- @overload fun(start: number, end: number, progress: number): any -- Interpolate the value given a start and an end value.
juice.pow5 = function() end
--- 
--- @overload fun(progress: number): any -- Give a percentage (progress) of the interpolation
--- @overload fun(start: number, end: number, progress: number): any -- Interpolate the value given a start and an end value.
juice.powIn2 = function() end
--- 
--- @overload fun(progress: number): any -- Give a percentage (progress) of the interpolation
--- @overload fun(start: number, end: number, progress: number): any -- Interpolate the value given a start and an end value.
juice.powIn3 = function() end
--- 
--- @overload fun(progress: number): any -- Give a percentage (progress) of the interpolation
--- @overload fun(start: number, end: number, progress: number): any -- Interpolate the value given a start and an end value.
juice.powIn4 = function() end
--- 
--- @overload fun(progress: number): any -- Give a percentage (progress) of the interpolation
--- @overload fun(start: number, end: number, progress: number): any -- Interpolate the value given a start and an end value.
juice.powIn5 = function() end
--- 
--- @overload fun(progress: number): any -- Give a percentage (progress) of the interpolation
--- @overload fun(start: number, end: number, progress: number): any -- Interpolate the value given a start and an end value.
juice.powOut2 = function() end
--- 
--- @overload fun(progress: number): any -- Give a percentage (progress) of the interpolation
--- @overload fun(start: number, end: number, progress: number): any -- Interpolate the value given a start and an end value.
juice.powOut3 = function() end
--- 
--- @overload fun(progress: number): any -- Give a percentage (progress) of the interpolation
--- @overload fun(start: number, end: number, progress: number): any -- Interpolate the value given a start and an end value.
juice.powOut4 = function() end
--- 
--- @overload fun(progress: number): any -- Give a percentage (progress) of the interpolation
--- @overload fun(start: number, end: number, progress: number): any -- Interpolate the value given a start and an end value.
juice.powOut5 = function() end
--- 
--- @overload fun(progress: number): any -- Give a percentage (progress) of the interpolation
--- @overload fun(start: number, end: number, progress: number): any -- Interpolate the value given a start and an end value.
juice.sine = function() end
--- 
--- @overload fun(progress: number): any -- Give a percentage (progress) of the interpolation
--- @overload fun(start: number, end: number, progress: number): any -- Interpolate the value given a start and an end value.
juice.sineIn = function() end
--- 
--- @overload fun(progress: number): any -- Give a percentage (progress) of the interpolation
--- @overload fun(start: number, end: number, progress: number): any -- Interpolate the value given a start and an end value.
juice.sineOut = function() end
--- 
--- @overload fun(progress: number): any -- Give a percentage (progress) of the interpolation
--- @overload fun(start: number, end: number, progress: number): any -- Interpolate the value given a start and an end value.
juice.circle = function() end
--- 
--- @overload fun(progress: number): any -- Give a percentage (progress) of the interpolation
--- @overload fun(start: number, end: number, progress: number): any -- Interpolate the value given a start and an end value.
juice.circleIn = function() end
--- 
--- @overload fun(progress: number): any -- Give a percentage (progress) of the interpolation
--- @overload fun(start: number, end: number, progress: number): any -- Interpolate the value given a start and an end value.
juice.circleOut = function() end
--- 
--- @overload fun(progress: number): any -- Give a percentage (progress) of the interpolation
--- @overload fun(start: number, end: number, progress: number): any -- Interpolate the value given a start and an end value.
juice.elastic = function() end
--- 
--- @overload fun(progress: number): any -- Give a percentage (progress) of the interpolation
--- @overload fun(start: number, end: number, progress: number): any -- Interpolate the value given a start and an end value.
juice.elasticIn = function() end
--- 
--- @overload fun(progress: number): any -- Give a percentage (progress) of the interpolation
--- @overload fun(start: number, end: number, progress: number): any -- Interpolate the value given a start and an end value.
juice.elasticOut = function() end
--- 
--- @overload fun(progress: number): any -- Give a percentage (progress) of the interpolation
--- @overload fun(start: number, end: number, progress: number): any -- Interpolate the value given a start and an end value.
juice.swing = function() end
--- 
--- @overload fun(progress: number): any -- Give a percentage (progress) of the interpolation
--- @overload fun(start: number, end: number, progress: number): any -- Interpolate the value given a start and an end value.
juice.swingIn = function() end
--- 
--- @overload fun(progress: number): any -- Give a percentage (progress) of the interpolation
--- @overload fun(start: number, end: number, progress: number): any -- Interpolate the value given a start and an end value.
juice.swingOut = function() end
--- 
--- @overload fun(progress: number): any -- Give a percentage (progress) of the interpolation
--- @overload fun(start: number, end: number, progress: number): any -- Interpolate the value given a start and an end value.
juice.bounce = function() end
--- 
--- @overload fun(progress: number): any -- Give a percentage (progress) of the interpolation
--- @overload fun(start: number, end: number, progress: number): any -- Interpolate the value given a start and an end value.
juice.bounceIn = function() end
--- 
--- @overload fun(progress: number): any -- Give a percentage (progress) of the interpolation
--- @overload fun(start: number, end: number, progress: number): any -- Interpolate the value given a start and an end value.
juice.bounceOut = function() end
--- 
--- @overload fun(progress: number): any -- Give a percentage (progress) of the interpolation
--- @overload fun(start: number, end: number, progress: number): any -- Interpolate the value given a start and an end value.
juice.exp10 = function() end
--- 
--- @overload fun(progress: number): any -- Give a percentage (progress) of the interpolation
--- @overload fun(start: number, end: number, progress: number): any -- Interpolate the value given a start and an end value.
juice.expIn10 = function() end
--- 
--- @overload fun(progress: number): any -- Give a percentage (progress) of the interpolation
--- @overload fun(start: number, end: number, progress: number): any -- Interpolate the value given a start and an end value.
juice.expOut10 = function() end
--- 
--- @overload fun(progress: number): any -- Give a percentage (progress) of the interpolation
--- @overload fun(start: number, end: number, progress: number): any -- Interpolate the value given a start and an end value.
juice.exp5 = function() end
--- 
--- @overload fun(progress: number): any -- Give a percentage (progress) of the interpolation
--- @overload fun(start: number, end: number, progress: number): any -- Interpolate the value given a start and an end value.
juice.expIn5 = function() end
--- 
--- @overload fun(progress: number): any -- Give a percentage (progress) of the interpolation
--- @overload fun(start: number, end: number, progress: number): any -- Interpolate the value given a start and an end value.
juice.expOut5 = function() end
--- 
--- @overload fun(progress: number): any -- Give a percentage (progress) of the interpolation
--- @overload fun(start: number, end: number, progress: number): any -- Interpolate the value given a start and an end value.
juice.linear = function() end


--- List all notes from C0 to B8. Please note that bemols are the note with b (ie: Gb2) while sharps are the note with s (ie: As3).
notes = {}
--- Get the name of a note regarding the note index (ie: C0 = 0, Cs0 = 1, ...)
--- @overload fun(note_index: number): any -- Get the name of a note regarding the note index (ie: C0 = 0, Cs0 = 1, ...)
notes.note = function() end


--- Shape API to draw...shapes. Those shapes can be circle, rectangle, line or oval.All shapes can be draw filed or not filed.
shape = {}
--- Draw a rectangle.
--- @overload fun(x: number, y: number, width: number, height: number, color: number): any -- Draw a rectangle.
--- @overload fun(rect: table): any -- Draw a rectangle.
--- @overload fun(rect: table, color: number): any -- Draw a rectangle using a rectangle and a color.
shape.rect = function() end
--- Draw a filled rectangle.
--- @overload fun(x: number, y: number, width: number, height: number, color: number): any -- Draw a filled rectangle.
--- @overload fun(rect: table): any -- Draw a filled rectangle.
--- @overload fun(rect: table, color: number): any -- Draw a filled rectangle using a rectangle and a color.
shape.rectf = function() end
--- Draw a filled circle.
--- @overload fun(centerX: number, centerY: number, radius: number, color: number): any -- Draw a circle at the coordinate (centerX, centerY) with the radius and the color.
shape.circlef = function() end
--- Draw a line.
--- @overload fun(x0: number, y0: number, x1: number, y1: number, color: number): any -- Draw a line.
--- @overload fun(x0: number, y0: number, x1: number, y1: number): any -- Draw a line with a default color.
shape.line = function() end
--- Draw a circle.
--- @overload fun(a: number, b: number, c: number): any -- Draw a circle with the default color.
--- @overload fun(centerX: number, centerY: number, radius: number, color: number): any -- Draw a circle.
shape.circle = function() end
--- Draw a filled triangle using the coordinates of (x1, y1), (x2, y2) and (x3, y3) and color.
--- @overload fun(x1: number, y1: number, x2: number, y2: number, x3: number, y3: number, color: number): any -- Draw a filled triangle using the coordinates of (x1, y1), (x2, y2) and (x3, y3).
shape.trianglef = function() end
--- Draw a triangle using the coordinates of (x1, y1), (x2, y2) and (x3, y3) and color.
--- @overload fun(x1: number, y1: number, x2: number, y2: number, x3: number, y3: number, color: number): any -- Draw a triangle using the coordinates of (x1, y1), (x2, y2) and (x3, y3).
shape.triangle = function() end
--- Draw a gradient using dithering, only from color c1 to color c2.
--- @overload fun(x: number, y: number, width: number, height: number, color1: number, color2: number, is_horizontal: boolean): any -- Draw a gradient using dithering, only from color c1 to color c2.
shape.gradient = function() end


--- List of the available keys. To be used with ctrl.
--- 
--- - `keys.up`, `keys.down`, `keys.left`, `keys.right` for directions.
--- - `keys.a` to `keys.z` and `keys.0` to `keys.9` for letters and numbers.
--- - `keys.space` and `keys.enter` for other keys.
--- 
keys = {}
--- the key a
keys.a = any
--- the key b
keys.b = any
--- the key c
keys.c = any
--- the key d
keys.d = any
--- the key e
keys.e = any
--- the key f
keys.f = any
--- the key g
keys.g = any
--- the key h
keys.h = any
--- the key i
keys.i = any
--- the key j
keys.j = any
--- the key k
keys.k = any
--- the key l
keys.l = any
--- the key m
keys.m = any
--- the key n
keys.n = any
--- the key o
keys.o = any
--- the key p
keys.p = any
--- the key q
keys.q = any
--- the key r
keys.r = any
--- the key s
keys.s = any
--- the key t
keys.t = any
--- the key u
keys.u = any
--- the key v
keys.v = any
--- the key w
keys.w = any
--- the key x
keys.x = any
--- the key y
keys.y = any
--- the key z
keys.z = any
--- the key space
keys.space = any
--- the key arrow up
keys.up = any
--- the key arrow down
keys.down = any
--- the key left down
keys.left = any
--- the key right down
keys.right = any
--- the key enter down
keys.enter = any
--- the key shift down
keys.shift = any
--- the key ctrl down
keys.ctrl = any
--- the key alt down
keys.alt = any
--- the key delete
keys.delete = any


--- Text rendering library for custom fonts. Allows selecting fonts configured in `_tiny.json` and rendering text with them. When no font is selected, uses the default boot font (same as `print()`).
text = {}
--- Select a font to use for text rendering. Call without arguments to reset to the default boot font.
--- @overload fun(): any -- Reset to the default boot font.
--- @overload fun(index: number): any -- Select a font by index.
text.font = function() end
--- Print text on the screen using the currently selected font.
--- @overload fun(args: any): any -- Print text at the given position with an optional color.
text.print = function() end
--- Measure the width in pixels of a string using the currently selected font.
--- @overload fun(str: string): any -- Returns the width in pixels of the text.
text.width = function() end


--- TODO
sfx = {}
--- Save the actual music in the current sfx file.
--- @overload fun(): any -- Save the actual music in the current sfx file.
sfx.save = function() end
--- Access instrument using its index or its name.
--- @overload fun(a: any): any -- Access instrument using its index or its name.
--- @overload fun(a: any, b: any): any -- Access instrument using its index or its name. Create it if the instrument is missing and the flag is true.
sfx.instrument = function() end
--- Access sfx using its index or its name.
--- @overload fun(arg: any): any -- Access sfx using its index or its name.
sfx.sfx = function() end
--- Access musical sequence using its index.
--- @overload fun(arg: any): any -- Access musical sequence using its index.
sfx.sequence = function() end

