-- DO NOT EDIT // DO NOT EDIT // DO NOT EDIT // DO NOT EDIT // DO NOT EDIT
-- Tiny stub lua file generated automatically
-- The file is used only to help Lua editors with autocomplete
-- 
-- An error, an issue? Please consult https://github.com/minigdx/tiny


--- Vector2 manipulation library.
vec2 = {}
--- Create a vector 2 as a table { x, y }.
--- @overload fun(x: any, y: any): any -- Create a vector 2 as a table { x, y }.
--- @overload fun(vec2: any): any -- Create a vector 2 as a table { x, y } using another vector 2.
vec2.create = function() end
--- Add vector2 to another vector2
--- @overload fun(v1: any, v2: any): any -- Add a vector 2 {x, y} to another vector 2 {x, y}
--- @overload fun(x1: any, y1: any, x2: any, y2: any): any -- Add a destructured vector 2 to another destructured vector 2
vec2.add = function() end
--- Subtract another vector from another vector
--- @overload fun(v1: any, v2: any): any -- Subtract a vector 2 {x, y} from another vector 2 {x, y}
--- @overload fun(x1: any, y1: any, x2: any, y2: any): any -- Subtract a destructured vector 2 from another destructured vector 2
vec2.sub = function() end
--- Dot product between two vectors
--- @overload fun(v1: any, v2: any): any -- Dot product between a vector 2 {x, y} and another vector 2 {x, y}
--- @overload fun(x1: any, y1: any, x2: any, y2: any): any -- Dot product between a destructured vector 2 and another destructured vector 2
vec2.dot = function() end
--- Calculate the magnitude (length) of a vector
--- @overload fun(x: any, y: any): any -- Calculate the magnitude (length) of a vector 2 {x, y}
--- @overload fun(v1: any): any -- Calculate the magnitude (length) of a vector 2 {x, y}
vec2.mag = function() end
--- Normalize a vector
--- @overload fun(x: any, y: any): any -- Normalize a vector 2 {x, y}
--- @overload fun(v1: any): any -- Normalize a vector 2 {x, y}
vec2.nor = function() end
--- Cross product
--- @overload fun(v1: any, v2: any): any -- Cross product between a vector 2 {x, y} and another vector 2 {x, y}
--- @overload fun(x1: any, y1: any, x2: any, y2: any): any -- Cross product between a destructured vector 2 and another destructured vector 2
vec2.crs = function() end
--- Scale a vector
--- @overload fun(x: any, y: any, scl: any): any -- Scale a vector 2 {x, y} using the factor scl
--- @overload fun(v1: any, scl: any): any -- Scale a vector 2 {x, y} using the factor scl
vec2.scl = function() end


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
--- @overload fun(progress: any): any -- Give a percentage (progress) of the interpolation
--- @overload fun(start: any, end: any, progress: any): any -- Interpolate the value given a start and an end value.
juice.pow2 = function() end
--- 
--- @overload fun(progress: any): any -- Give a percentage (progress) of the interpolation
--- @overload fun(start: any, end: any, progress: any): any -- Interpolate the value given a start and an end value.
juice.pow3 = function() end
--- 
--- @overload fun(progress: any): any -- Give a percentage (progress) of the interpolation
--- @overload fun(start: any, end: any, progress: any): any -- Interpolate the value given a start and an end value.
juice.pow4 = function() end
--- 
--- @overload fun(progress: any): any -- Give a percentage (progress) of the interpolation
--- @overload fun(start: any, end: any, progress: any): any -- Interpolate the value given a start and an end value.
juice.pow5 = function() end
--- 
--- @overload fun(progress: any): any -- Give a percentage (progress) of the interpolation
--- @overload fun(start: any, end: any, progress: any): any -- Interpolate the value given a start and an end value.
juice.powIn2 = function() end
--- 
--- @overload fun(progress: any): any -- Give a percentage (progress) of the interpolation
--- @overload fun(start: any, end: any, progress: any): any -- Interpolate the value given a start and an end value.
juice.powIn3 = function() end
--- 
--- @overload fun(progress: any): any -- Give a percentage (progress) of the interpolation
--- @overload fun(start: any, end: any, progress: any): any -- Interpolate the value given a start and an end value.
juice.powIn4 = function() end
--- 
--- @overload fun(progress: any): any -- Give a percentage (progress) of the interpolation
--- @overload fun(start: any, end: any, progress: any): any -- Interpolate the value given a start and an end value.
juice.powIn5 = function() end
--- 
--- @overload fun(progress: any): any -- Give a percentage (progress) of the interpolation
--- @overload fun(start: any, end: any, progress: any): any -- Interpolate the value given a start and an end value.
juice.powOut2 = function() end
--- 
--- @overload fun(progress: any): any -- Give a percentage (progress) of the interpolation
--- @overload fun(start: any, end: any, progress: any): any -- Interpolate the value given a start and an end value.
juice.powOut3 = function() end
--- 
--- @overload fun(progress: any): any -- Give a percentage (progress) of the interpolation
--- @overload fun(start: any, end: any, progress: any): any -- Interpolate the value given a start and an end value.
juice.powOut4 = function() end
--- 
--- @overload fun(progress: any): any -- Give a percentage (progress) of the interpolation
--- @overload fun(start: any, end: any, progress: any): any -- Interpolate the value given a start and an end value.
juice.powOut5 = function() end
--- 
--- @overload fun(progress: any): any -- Give a percentage (progress) of the interpolation
--- @overload fun(start: any, end: any, progress: any): any -- Interpolate the value given a start and an end value.
juice.sine = function() end
--- 
--- @overload fun(progress: any): any -- Give a percentage (progress) of the interpolation
--- @overload fun(start: any, end: any, progress: any): any -- Interpolate the value given a start and an end value.
juice.sineIn = function() end
--- 
--- @overload fun(progress: any): any -- Give a percentage (progress) of the interpolation
--- @overload fun(start: any, end: any, progress: any): any -- Interpolate the value given a start and an end value.
juice.sineOut = function() end
--- 
--- @overload fun(progress: any): any -- Give a percentage (progress) of the interpolation
--- @overload fun(start: any, end: any, progress: any): any -- Interpolate the value given a start and an end value.
juice.circle = function() end
--- 
--- @overload fun(progress: any): any -- Give a percentage (progress) of the interpolation
--- @overload fun(start: any, end: any, progress: any): any -- Interpolate the value given a start and an end value.
juice.circleIn = function() end
--- 
--- @overload fun(progress: any): any -- Give a percentage (progress) of the interpolation
--- @overload fun(start: any, end: any, progress: any): any -- Interpolate the value given a start and an end value.
juice.circleOut = function() end
--- 
--- @overload fun(progress: any): any -- Give a percentage (progress) of the interpolation
--- @overload fun(start: any, end: any, progress: any): any -- Interpolate the value given a start and an end value.
juice.elastic = function() end
--- 
--- @overload fun(progress: any): any -- Give a percentage (progress) of the interpolation
--- @overload fun(start: any, end: any, progress: any): any -- Interpolate the value given a start and an end value.
juice.elasticIn = function() end
--- 
--- @overload fun(progress: any): any -- Give a percentage (progress) of the interpolation
--- @overload fun(start: any, end: any, progress: any): any -- Interpolate the value given a start and an end value.
juice.elasticOut = function() end
--- 
--- @overload fun(progress: any): any -- Give a percentage (progress) of the interpolation
--- @overload fun(start: any, end: any, progress: any): any -- Interpolate the value given a start and an end value.
juice.swing = function() end
--- 
--- @overload fun(progress: any): any -- Give a percentage (progress) of the interpolation
--- @overload fun(start: any, end: any, progress: any): any -- Interpolate the value given a start and an end value.
juice.swingIn = function() end
--- 
--- @overload fun(progress: any): any -- Give a percentage (progress) of the interpolation
--- @overload fun(start: any, end: any, progress: any): any -- Interpolate the value given a start and an end value.
juice.swingOut = function() end
--- 
--- @overload fun(progress: any): any -- Give a percentage (progress) of the interpolation
--- @overload fun(start: any, end: any, progress: any): any -- Interpolate the value given a start and an end value.
juice.bounce = function() end
--- 
--- @overload fun(progress: any): any -- Give a percentage (progress) of the interpolation
--- @overload fun(start: any, end: any, progress: any): any -- Interpolate the value given a start and an end value.
juice.bounceIn = function() end
--- 
--- @overload fun(progress: any): any -- Give a percentage (progress) of the interpolation
--- @overload fun(start: any, end: any, progress: any): any -- Interpolate the value given a start and an end value.
juice.bounceOut = function() end
--- 
--- @overload fun(progress: any): any -- Give a percentage (progress) of the interpolation
--- @overload fun(start: any, end: any, progress: any): any -- Interpolate the value given a start and an end value.
juice.exp10 = function() end
--- 
--- @overload fun(progress: any): any -- Give a percentage (progress) of the interpolation
--- @overload fun(start: any, end: any, progress: any): any -- Interpolate the value given a start and an end value.
juice.expIn10 = function() end
--- 
--- @overload fun(progress: any): any -- Give a percentage (progress) of the interpolation
--- @overload fun(start: any, end: any, progress: any): any -- Interpolate the value given a start and an end value.
juice.expOut10 = function() end
--- 
--- @overload fun(progress: any): any -- Give a percentage (progress) of the interpolation
--- @overload fun(start: any, end: any, progress: any): any -- Interpolate the value given a start and an end value.
juice.exp5 = function() end
--- 
--- @overload fun(progress: any): any -- Give a percentage (progress) of the interpolation
--- @overload fun(start: any, end: any, progress: any): any -- Interpolate the value given a start and an end value.
juice.expIn5 = function() end
--- 
--- @overload fun(progress: any): any -- Give a percentage (progress) of the interpolation
--- @overload fun(start: any, end: any, progress: any): any -- Interpolate the value given a start and an end value.
juice.expOut5 = function() end
--- 
--- @overload fun(progress: any): any -- Give a percentage (progress) of the interpolation
--- @overload fun(start: any, end: any, progress: any): any -- Interpolate the value given a start and an end value.
juice.linear = function() end


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
--- @overload fun(cell: any): any -- Convert the cell coordinates from a table {cx,cy} into screen coordinates as a table {x,y}.
map.from = function() end
--- Convert screen coordinates x, y into map cell coordinates {cx, cy}.
--- For example, coordinates of the player can be converted to cell coordinates to access the flag of the tile matching the player coordinates.
--- @overload fun(x: any, y: any): any -- Convert the coordinates into cell coordinates as a table {cx = cx,cy = cy}.
--- @overload fun(coordinates: any): any -- Convert the coordinates from a table {x,y} into cell coordinates as a table {cx,cy}.
map.to = function() end
--- Get the flag from a tile, using cell coordinates.
--- @overload fun(cx: any, cy: any): any -- Get the flag from the tile at the coordinate cx,cy.
--- @overload fun(cx: any, cy: any, layer: any): any -- Get the flag from the tile at the coordinate cx,cy from a specific layer.
map.cflag = function() end
--- Get the flag from a tile, using screen coordinates.
--- @overload fun(x: any, y: any): any -- Get the flag from the tile at the coordinate x,y.
--- @overload fun(x: any, y: any, layer: any): any -- Get the flag from the tile at the coordinate x,y from a specific layer.
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


--- List all notes from C0 to B8. Please note that bemols are the note with b (ie: Gb2) while sharps are the note with s (ie: As3).
notes = {}
--- Get the name of a note regarding the note index (ie: C0 = 0, Cs0 = 1, ...)
--- @overload fun(note_index: any): any -- Get the name of a note regarding the note index (ie: C0 = 0, Cs0 = 1, ...)
notes.note = function() end


--- Helpers to debug your game by drawing or printing information on screen.
debug = {}
--- Log a message into the console.
--- @overload fun(str: any): any -- Log a message into the console.
debug.console = function() end


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


--- Math functions. Please note that standard Lua math methods are also available.
math = {}
--- value of pi (~3.14)
math.pi = any
--- positive infinity value.
math.huge = any
--- Return the sign of the number: -1 if negative. 1 otherwise.
--- @overload fun(number: any): any -- Return the sign of the number.
math.sign = function() end
--- Calculate the angle in radians between the positive x-axis and the point (x, y).
--- @overload fun(y: any, x: any): any -- Calculate the angle for the point (x, y). Please note the argument order: y then x.
math.atan2 = function() end
--- Clamp the value between 2 values.
--- @overload fun(a: any, value: any, b: any): any -- Clamp the value between a and b. If a is greater than b, then b will be returned.
math.clamp = function() end
--- Compute the distance between two points.
--- @overload fun(x1: any, y1: any, x2: any, y2: any): any -- Distance between (x1, y1) and (x2, y2).
math.dst = function() end
--- Compute the distance between two points not squared. Use this method to know if an coordinate is closer than another.
--- @overload fun(x1: any, y1: any, x2: any, y2: any): any -- Distance not squared between (x1, y1) and (x2, y2).
math.dst2 = function() end
--- Generate random values
--- @overload fun(): any -- Generate a random int (negative or positive value)
--- @overload fun(until: any): any -- Generate a random value between 1 until the argument. If a table is passed, it'll return a random element of the table.
--- @overload fun(a: any, b: any): any -- Generate a random value between a and b.
math.rnd = function() end
--- Check if two (r)ectangles overlaps.
--- @overload fun(rect1: any, rect2: any): any -- Check if the rectangle rect1 overlaps with the rectangle rect2.
math.roverlap = function() end
--- Perlin noise. The random generated value is between 0.0 and 1.0.
--- @overload fun(x: any, y: any, z: any): any -- Generate a random value regarding the parameters x,y and z.
math.perlin = function() end


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


--- Standard library.
--- Create new instance of a class by creating a new table and setting the metatable. It allow to create kind of Object Oriented Programming.
--- 
---  
--- @overload fun(class: any): any -- Create new instance of class.
--- @overload fun(class: any, default: any): any -- Create new instance of class using default values.
function new() end
--- Add *all key/value* from the table `source` to the table `dest`.
--- @overload fun(source: any, dest: any): any -- Merge source into dest.
function merge() end
--- Append *all values* from the table `source` to the table `dest`.
--- @overload fun(source: any, dest: any): any -- Copy source into dest.
function append() end
--- Iterate over values of a table.
--- 
--- - If you want to iterate over keys, use `pairs(table)`.
---  - If you want to iterate over index, use `ipairs(table)`.
---  - If you want to iterate in reverse, use `rpairs(table)`.
--- 
--- @overload fun(table: any): any -- Iterate over the values of the table
function all() end
--- Iterate over values of a table in reverse order. The iterator return an index and the value. The method is useful to remove elements from a table while iterating on it.
--- @overload fun(table: any): any -- Iterate over the values of the table
function rpairs() end
--- Print on the screen a string.
--- @overload fun(str: any): any -- print on the screen a string at (0,0) with a default color.
--- @overload fun(str: any, x: any, y: any): any -- print on the screen a string with a default color.
--- @overload fun(str: any, x: any, y: any, color: any): any -- print on the screen a string with a specific color.
function print() end


--- Sound API to play/loop/stop a sound.
--- A sound can be created using the sound editor, using the command line `tiny-cli sfx <filename>`.
--- WARNING: Because of browser behaviour, a sound can *only* be played only after the first
--- user interaction.
--- 
--- Avoid to start a music or a sound at the beginning of the game.
--- Before it, force the player to hit a key or click by adding an interactive menu
--- or by starting the sound as soon as the player is moving.
--- 
sfx = {}
--- Save the actual music using the filename.
--- @overload fun(filename: any): any -- Save the actual music using the filename
sfx.save = function() end
--- Load the actual SFX sound as the actual music.
--- @overload fun(filename: any): any -- Load the actual SFX sound as the actual music using filename or its index
sfx.load = function() end
--- Access track using its index or its name.
--- @overload fun(arg: any): any -- Access instrument using its index or its name.
sfx.track = function() end
--- Access instrument using its index or its name.
--- @overload fun(a: any): any -- Access instrument using its index or its name.
--- @overload fun(a: any, b: any): any -- Access instrument using its index or its name. Create it if the instrument is missing and the flag is true.
sfx.instrument = function() end
--- Access sfx using its index or its name.
--- @overload fun(arg: any): any -- Access sfx using its index or its name.
sfx.bar = function() end
--- Play the bar by it's index of the current sound. The index of a bar of the current music.
--- @overload fun(): any -- Play the sound at the index 0.
--- @overload fun(sound: any): any -- Play the sound by it's index.
sfx.play = function() end
--- Loop the bar by it's index of the current sound. The index of a bar of the current music.
--- @overload fun(): any -- Loop the sound at the index 0.
--- @overload fun(sound: any): any -- Loop the sound by it's index.
sfx.loop = function() end
--- Stop the bar by it's index of the current sound. The index of a bar of the current music.
--- @overload fun(): any -- Stop the sound at the index 0.
--- @overload fun(sound: any): any -- Stop the sound by it's index.
sfx.stop = function() end
--- Play the sequence by it's index of the current sound. The index of a sequence of the current music.
--- @overload fun(): any -- Play the sequence at the index 0.
--- @overload fun(sequence: any): any -- Play the sequence by it's index.
sfx.mplay = function() end
--- Loop the sequence by it's index of the current sound. The index of a sequence of the current music.
--- @overload fun(): any -- Loop the sequence at the index 0.
--- @overload fun(sequence: any): any -- Loop the sequence by it's index.
sfx.mloop = function() end
--- Stop the sequence by it's index of the current sound. The index of a sequence of the current music.
--- @overload fun(): any -- Stop the sequence at the index 0.
--- @overload fun(sequence: any): any -- Stop the sequence by it's index.
sfx.mstop = function() end


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
--- @overload fun(mode: any): any -- Switch to another draw mode. Return the previous mode.
gfx.draw_mode = function() end
--- clear the screen
--- @overload fun(): any -- Clear the screen with a default color.
--- @overload fun(color: any): any -- Clear the screen with a color.
gfx.cls = function() end
--- Set the color index at the coordinate (x,y).
--- @overload fun(x: any, y: any, color: any): any -- set the color index at the coordinate (x,y).
gfx.pset = function() end
--- Get the color index at the coordinate (x,y).
--- @overload fun(x: any, y: any): any -- get the color index at the coordinate (x,y).
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
--- @overload fun(a: any, b: any): any -- Replace the color a for the color b.
gfx.pal = function() end
--- Move the game camera.
--- @overload fun(): any -- Reset the game camera to it's default position (0,0).
--- @overload fun(x: any, y: any): any -- Set game camera to the position x, y.
gfx.camera = function() end
--- Apply a dithering pattern on every new draw call. The pattern is using the bits value of a 2 octet value. The first bits is the one on the far left and represent the pixel of the top left of a 4x4 matrix. The last bit is the pixel from the bottom right of this matrix.
--- @overload fun(): any -- Reset dithering pattern. The previous dithering pattern is returned.
--- @overload fun(pattern: any): any -- Apply dithering pattern. The previous dithering pattern is returned.
gfx.dither = function() end
--- Clip the draw surface (ie: limit the drawing area).
--- @overload fun(): any -- Reset the clip and draw on the fullscreen.
--- @overload fun(x: any, y: any, width: any, height: any): any -- Clip and limit the drawing area.
gfx.clip = function() end


--- Floppy allow you to get or save user Lua structure.
floppy = {}
--- Save the content into a local file, on desktop or in the local storage on the web platform.
--- @overload fun(name: any, content: any): any -- Save the content into the file name.
floppy.put = function() end
--- Load and get the content of the file name
--- @overload fun(name: any): any -- Load and get the content of the file name
floppy.get = function() end


--- Access to controllers like touch/mouse events or accessing which key is pressed by the user.
ctrl = {}
--- Get coordinates of the current touch/mouse. If the mouse/touch is out-of the screen, the coordinates will be the last mouse position/touch. The function return those coordinates as a table {x, y}. A sprite can be draw directly on the mouse position by passing the sprite number. 
--- @overload fun(): any -- Get the mouse coordinates.
--- @overload fun(sprN: any): any -- Get the mouse coordinate and draw a sprite on those coordinates.
ctrl.touch = function() end
--- Return true if the key was pressed during the last frame. If you need to check that the key is still pressed, see `ctrl.pressing` instead.
--- @overload fun(key: any): any -- Is the key was pressed?
ctrl.pressed = function() end
--- Return true if the key is still pressed. 
--- @overload fun(key: any): any -- Is the key is still pressed?
ctrl.pressing = function() end
--- Return the position of the touch (as `{x, y}`)if the screen was touched or the mouse button was pressed during the last frame. `nil` otherwise.
--- The touch can be : 
--- 
--- - 0: left click or one finger
--- - 1: right click or two fingers
--- - 2: middle click or three fingers
--- 
--- If you need to check that the touch/mouse button is still active, see `ctrl.touching` instead.
--- @overload fun(touch: any): any -- Is the screen was touched or mouse button was pressed?
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


--- Sprite API to draw or update sprites.
spr = {}
--- Get the color index at the coordinate (x,y) from the current spritesheet.
--- @overload fun(x: number, y: number): any -- get the color index at the coordinate (x,y) from the current spritesheet.
spr.pget = function() end
--- Set the color index at the coordinate (x,y) in the current spritesheet.
--- @overload fun(x: any, y: any, color: any): any -- Set the color index at the coordinate (x,y) in the current spritesheet.
spr.pset = function() end
--- Switch to another spritesheet. The index of the spritesheet is given by it's position in the spritesheets field from the `_tiny.json` file.The first spritesheet is at the index 0. It retuns the previous spritesheet. The spritesheet can also be referenced by its filename.
--- @overload fun(): any -- Switch to the first spritesheet
--- @overload fun(spritesheetN: any): any -- Switch to the N spritesheet
spr.sheet = function() end
--- S(uper) Draw a fragment from the spritesheet.
--- @overload fun(): any -- Draw the full spritesheet at default coordinate (0, 0)
--- @overload fun(x: any, y: any): any -- Draw the full spritesheet at coordinate (x, y)
--- @overload fun(x: any, y: any, sprX: any, sprY: any): any -- Draw the full spritesheet at coordinate (x, y) from the sprite (sprX, sprY)
--- @overload fun(x: any, y: any, sprX: any, sprY: any, width: any, height: any, flipX: any, flipY: any): any -- Draw a fragment from the spritesheet at the coordinate (x, y) from the sprite (sprX, sprY) with the width and height.
spr.sdraw = function() end
--- Draw a sprite.
--- @overload fun(sprN: any): any -- Draw a sprite at the default coordinate (0, 0).
--- @overload fun(sprN: any, x: any, y: any): any -- Draw a sprite.
--- @overload fun(sprN: any, x: any, y: any, flipX: any, flipY: any): any -- Draw a sprite and allow flip on x or y axis.
spr.draw = function() end


--- Shape API to draw...shapes. Those shapes can be circle, rectangle, line or oval.All shapes can be draw filed or not filed.
shape = {}
--- Draw a rectangle.
--- @overload fun(x: any, y: any, width: any, height: any, color: any): any -- Draw a rectangle.
--- @overload fun(rect: any): any -- Draw a rectangle.
--- @overload fun(rect: any, color: any): any -- Draw a rectangle using a rectangle and a color.
shape.rect = function() end
--- Draw a filled rectangle.
--- @overload fun(x: any, y: any, width: any, height: any, color: any): any -- Draw a filled rectangle.
--- @overload fun(rect: any): any -- Draw a filled rectangle.
--- @overload fun(rect: any, color: any): any -- Draw a filled rectangle using a rectangle and a color.
shape.rectf = function() end
--- Draw a filled circle.
--- @overload fun(centerX: any, centerY: any, radius: any, color: any): any -- Draw a circle at the coordinate (centerX, centerY) with the radius and the color.
shape.circlef = function() end
--- Draw a line.
--- @overload fun(x0: any, y0: any, x1: any, y1: any, color: any): any -- Draw a line.
--- @overload fun(x0: any, y0: any, x1: any, y1: any): any -- Draw a line with a default color.
shape.line = function() end
--- Draw a circle.
--- @overload fun(a: any, b: any, c: any): any -- Draw a circle with the default color.
--- @overload fun(centerX: any, centerY: any, radius: any, color: any): any -- Draw a circle.
shape.circle = function() end
--- Draw a filled triangle using the coordinates of (x1, y1), (x2, y2) and (x3, y3) and color.
--- @overload fun(x1: any, y1: any, x2: any, y2: any, x3: any, y3: any, color: any): any -- Draw a filled triangle using the coordinates of (x1, y1), (x2, y2) and (x3, y3).
shape.trianglef = function() end
--- Draw a triangle using the coordinates of (x1, y1), (x2, y2) and (x3, y3) and color.
--- @overload fun(x1: any, y1: any, x2: any, y2: any, x3: any, y3: any, color: any): any -- Draw a triangle using the coordinates of (x1, y1), (x2, y2) and (x3, y3).
shape.triangle = function() end
--- Draw a gradient using dithering, only from color c1 to color c2.
--- @overload fun(x: any, y: any, width: any, height: any, color1: any, color2: any, is_horizontal: any): any -- Draw a gradient using dithering, only from color c1 to color c2.
shape.gradient = function() end

