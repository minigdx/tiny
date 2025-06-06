-- DO NOT EDIT // DO NOT EDIT // DO NOT EDIT // DO NOT EDIT // DO NOT EDIT
-- Tiny stub lua file generated automatically
-- The file is used only to help Lua editors with autocomplete
-- 
-- An error, an issue? Please consult https://github.com/minigdx/tiny


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
--- @overload fun(touch: any): any -- Is the screen is still touched or mouse button is still pressed?
ctrl.touching = function() end


--- Helpers to debug your game by drawing or printing information on screen.
debug = {}
--- Enable or disable debug feature.
--- @overload fun(enabled: any): any -- Enable or disable debug by passing true to enable, false to disable.
--- @overload fun(): any -- Return true if debug is enabled. False otherwise.
debug.enabled = function() end
--- Display a table.
--- @overload fun(table: any): any -- Display a table.
debug.table = function() end
--- Log a message on the screen.
--- @overload fun(str: any): any -- Log a message on the screen.
debug.log = function() end
--- Log a message into the console.
--- @overload fun(str: any): any -- Log a message into the console.
debug.console = function() end
--- Draw a rectangle on the screen
--- @overload fun(x: any, y: any, width: any, height: any, color: any): any -- Draw a debug rectangle.
--- @overload fun(rect: any): any -- Draw a debug rectangle.
--- @overload fun(rect: any, color: any): any -- Draw a debug rectangle using a rectangle and a color.
debug.rect = function() end
--- Draw a point on the screen
--- @overload fun(x: any, y: any, color: any): any -- Draw a debug point.
--- @overload fun(point: any): any -- Draw a debug point.
--- @overload fun(point: any, color: any): any -- Draw a debug point.
debug.point = function() end
--- Draw a point on the screen
--- @overload fun(x1: any, y1: any, x2: any, y2: any, color: any): any -- Draw a debug line.
--- @overload fun(v1: any, v2: any): any -- Draw a debug line.
--- @overload fun(v1: any, v2: any, color: any): any -- Draw a debug line.
debug.line = function() end


--- Access to graphical API like updating the color palette or applying a dithering pattern.
gfx = {}
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


--- List of the available keys. To be used with ctrl.
--- 
--- - `keys.up`, `keys.down`, `keys.left`, `keys.right` for directions.
--- - `keys.a` to `keys.z` and `keys.0` to `keys.9` for letters and numbers.
--- - `keys.space` and `keys.enter` for other keys.
--- 
keys = {}


--- Access map created with LDTk ( https://ldtk.io/ ).
map = {}
--- Set the current level to use.
--- @overload fun(): any -- Return the index of the current level.
--- @overload fun(level: any): any -- Set the current level to use. The level can be an index or the id defined by LDTK. Return the previous index level.
map.level = function() end
--- Get the list of layers from the actual level.
--- @overload fun(layer_index: any): any -- Get the layer at the specified index or name from the actual level. The layer in the front is 0.
--- @overload fun(): any -- Get the list of layers from the actual level.
map.layer = function() end
--- Convert cell coordinates cx, cy into map screen coordinates x, y.
--- @overload fun(arg1: any, arg2: any): any -- Convert the cell coordinates into coordinates as a table [x,y].
--- @overload fun(cell: any): any -- Convert the cell coordinates from a table [cx,cy] into screen coordinates as a table [x,y].
map.from = function() end
--- Convert screen coordinates x, y into map cell coordinates cx, cy.
--- For example, coordinates of the player can be converted to cell coordinates to access the flag of the tile matching the player coordinates.
--- @overload fun(x: any, y: any): any -- Convert the coordinates into cell coordinates as a table [cx,cy].
--- @overload fun(coordinates: any): any -- Convert the coordinates from a table [x,y] into cell coordinates as a table [cx,cy].
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


--- Math functions. Please note that standard Lua math methods are also available.
math = {}
--- Return the sign of the number: -1 if negative. 1 otherwise.
--- @overload fun(number: any): any -- Return the sign of the number.
math.sign = function() end
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


--- List all notes from C0 to B8. Please note that bemols are the note with b (ie: Gb2) while sharps are the note with s (ie: As3).
notes = {}


--- Sound API to play/loop/stop a sound.
--- A sound can be an SFX sound, generated using the tiny-cli sfx command or a MIDI file.
--- Please note that a SFX sound will produce the same sound whatever platform and whatever computer
--- as the sound is generated. 
--- 
--- A MIDI sound will depend of the MIDI synthesizer available on the machine.
---   
--- WARNING: Because of browser behaviour, a sound can *only* be played only after the first 
--- user interaction. 
--- 
--- Avoid to start a music or a sound at the beginning of the game.
--- Before it, force the player to hit a key or click by adding an interactive menu 
--- or by starting the sound as soon as the player is moving.
--- 
sfx = {}
--- Access instrument using its index or its name.
--- @overload fun(arg: any): any -- Access instrument using its index or its name.
sfx.instrument = function() end
--- Generate and play a sine wave sound.
--- @overload fun(note: any, duration: any, volume: any): any -- Generate and play a sound using one note.
sfx.sine = function() end
--- Generate and play a sawtooth wave sound.
--- @overload fun(note: any, duration: any, volume: any): any -- Generate and play a sound using one note.
sfx.sawtooth = function() end
--- Generate and play a square wave sound.
--- @overload fun(note: any, duration: any, volume: any): any -- Generate and play a sound using one note.
sfx.square = function() end
--- Generate and play a triangle wave sound.
--- @overload fun(note: any, duration: any, volume: any): any -- Generate and play a sound using one note.
sfx.triangle = function() end
--- Generate and play a noise wave sound.
--- @overload fun(note: any, duration: any, volume: any): any -- Generate and play a sound using one note.
sfx.noise = function() end
--- Generate and play a pulse wave sound.
--- @overload fun(note: any, duration: any, volume: any): any -- Generate and play a sound using one note.
sfx.pulse = function() end
--- Play a sound by it's index. The index of a sound is given by it's position in the sounds field from the `_tiny.json` file.The first sound is at the index 0.
--- @overload fun(): any -- Play the sound at the index 0.
--- @overload fun(sound: any): any -- Play the sound by it's index.
sfx.play = function() end
--- Play a sound and loop over it.
--- @overload fun(): any -- Play the sound at the index 0.
--- @overload fun(sound: any): any -- Play the sound by it's index.
sfx.loop = function() end
--- Stop a sound.
--- @overload fun(): any -- Stop the sound at the index 0.
--- @overload fun(sound: any): any -- Stop the sound by it's index.
sfx.stop = function() end


--- Shape API to draw...shapes. Those shapes can be circle, rectangle, line or oval.All shapes can be draw filed or not filed.
shape = {}
--- Draw a rectangle.
--- @overload fun(x: any, y: any, width: any, height: any, color: any): any -- Draw a rectangle.
--- @overload fun(rect: any): any -- Draw a rectangle.
--- @overload fun(rect: any, color: any): any -- Draw a rectangle using a rectangle and a color.
shape.rect = function() end
--- Draw an oval.
--- @overload fun(centerX: any, centerY: any, radiusX: any, radiusY: any): any -- Draw an oval using the default color.
--- @overload fun(centerX: any, centerY: any, radiusX: any, radiusY: any, color: any): any -- Draw an oval using the specified color.
shape.oval = function() end
--- Draw an oval filled.
--- @overload fun(centerX: any, centerY: any, radiusX: any, radiusY: any): any -- Draw a filled oval using the default color.
--- @overload fun(centerX: any, centerY: any, radiusX: any, radiusY: any, color: any): any -- Draw a filled oval using the specified color.
shape.ovalf = function() end
--- Draw a filled rectangle.
--- @overload fun(x: any, y: any, width: any, height: any, color: any): any -- Draw a filled rectangle.
--- @overload fun(rect: any): any -- Draw a filled rectangle.
--- @overload fun(rect: any, color: any): any -- Draw a filled rectangle using a rectangle and a color.
shape.rectf = function() end
--- Draw a filled circle.
--- @overload fun(centerX: any, centerY: any, radius: any, color: any): any -- Draw a circle at the coordinate (centerX, centerY) with the radius and the color.
shape.circlef = function() end
--- Draw a line.
--- @overload fun(x0: any, y0: any, x1: any, y2: any, color: any): any -- Draw a line.
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


--- Sprite API to draw or update sprites.
spr = {}
--- Get the color index at the coordinate (x,y) from the current spritesheet.
--- @overload fun(x: any, y: any): any -- get the color index at the coordinate (x,y) from the current spritesheet.
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


--- Test method utilities used when tests are run. See link:#_the_tiny_cli_run_command[Run command]
test = {}
--- Assert that `expected` and `actual` are equals
--- @overload fun(expected: any, actual: any): any -- Assert that `expected` and `actual` are equals
test.eq = function() end
--- Assert that `expected` and `actual` are __not__ equals
--- @overload fun(expected: any, actual: any): any -- Assert that `expected` and `actual` are not equals
test.neq = function() end
--- Assert that `actual` is true
--- @overload fun(actual: any): any -- Assert that `actual` is true
test.t = function() end
--- Assert that `actual` is false
--- @overload fun(actual: any): any -- Assert that `actual` is false
test.t = function() end
--- Create a new `test` named `name`
--- @overload fun(name: any, test: any): any -- Create a new `test` named `name`
test.create = function() end


--- Tiny Lib which offer offer the current frame (`tiny.frame`), the current time (`tiny.time`), delta time (`tiny.dt`) and to switch to another script using `exit`.
tiny = {}
--- Exit the actual script to switch to another one. The next script to use is identified by it's index. The index of the script is the index of it in the list of scripts from the `_tiny.json` file.The first script is at the index 0.
--- @overload fun(scriptIndex: any): any -- Exit the actual script to switch to another one.
tiny.exit = function() end


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


--- Workspace manipulation library. It allows you to save/load/download files
ws = {}
--- Save the content into a local file, on desktop or in the local storage on the web platform.
--- @overload fun(name: any, content: any): any -- Save the content into the file name.
ws.save = function() end
--- Load and get the content of the file name
--- @overload fun(name: any): any -- Load and get the content of the file name
ws.load = function() end
--- Create a local file. The name is generated so the name is unique.
--- @overload fun(prefix: any, extension: any): any -- Create a local file with the prefix and the extension. The name of the file created.
ws.create = function() end
--- List all files available in the workspace.
--- @overload fun(): any -- List all files available in the workspace.
--- @overload fun(extension: any): any -- List all files available in the workspace and filter by the file extension.
ws.list = function() end

