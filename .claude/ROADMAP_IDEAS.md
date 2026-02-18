# Tiny Engine - Roadmap Ideas

> Feature ideas and improvement proposals for the Tiny game engine.
> Generated from a comprehensive analysis of the current codebase (2026-02-18).

---

## Table of Contents

1. [Audio & Music](#1-audio--music)
2. [Graphics & Rendering](#2-graphics--rendering)
3. [Physics & Collisions](#3-physics--collisions)
4. [Animation System](#4-animation-system)
5. [Particle System](#5-particle-system)
6. [Camera System](#6-camera-system)
7. [UI Framework](#7-ui-framework)
8. [Networking](#8-networking)
9. [Input & Controls](#9-input--controls)
10. [Tilemaps & Level Design](#10-tilemaps--level-design)
11. [Resource Management](#11-resource-management)
12. [Scripting & Lua API](#12-scripting--lua-api)
13. [Debugging & Profiling](#13-debugging--profiling)
14. [CLI & Tooling](#14-cli--tooling)
15. [Web Editor](#15-web-editor)
16. [Export & Distribution](#16-export--distribution)
17. [Documentation & Learning](#17-documentation--learning)
18. [Testing & Quality](#18-testing--quality)
19. [Performance](#19-performance)
20. [Accessibility](#20-accessibility)

---

## 1. Audio & Music

### 1.1 Complete Music Playback System
**Priority: High**
The music playback framework is partially in place but not fully wired (`SoundLib.kt` contains a `TODO later :P`). This is one of the most impactful missing features.

- Implement full music sequencer with pattern-based playback
- Support multi-channel music tracks (melody, bass, drums, etc.)
- Add tempo control (BPM) and time signature support
- Wire the existing `MusicalBar` and `MusicalScore` infrastructure to actual playback
- Complete the pattern editor in the SFX editor (`sfx-editor.lua` has a TODO for this)

### 1.2 Audio Mixing & Effects
- Add a mixer with per-channel volume control
- Support audio effects: reverb, delay, chorus, distortion
- Add low-pass / high-pass filter controls exposed to Lua
- Support crossfading between music tracks

### 1.3 Music Import
- Allow importing simple music formats (MIDI, tracker formats like .mod/.xm)
- Provide a converter from standard formats to the engine's SFX format

### 1.4 Enhanced Sound API
- Add `sound.volume()` - global and per-sound volume control
- Add `sound.pitch()` - pitch shifting for sound effects
- Add `sound.pan()` - stereo panning
- Add `sound.pause()` / `sound.resume()` on sound handlers
- Add `sound.queue()` - queue sounds for sequential playback

---

## 2. Graphics & Rendering

### 2.1 Text Rendering System
**Priority: High**
Currently there is no built-in text rendering. Games must draw text manually via sprites.

- Built-in bitmap font renderer with a default font
- Support for custom bitmap fonts (loaded as spritesheets)
- `gfx.print(text, x, y, color)` - draw text on screen
- `gfx.printf(text, x, y, color, width)` - draw text with word wrapping
- Text alignment options (left, center, right)
- Character spacing and line height configuration

### 2.2 Sprite Animation Support
- `spr.anim(frames, speed)` - create an animation from a list of sprite indices
- `spr.play(anim, x, y)` - play an animation at a position
- Auto-advancing frame counter based on delta time
- Looping, ping-pong, and one-shot animation modes

### 2.3 Sprite Transformations
- Sprite rotation support (currently only flip is supported)
- Sprite scaling (individual sprite scale, not just global zoom)
- Sprite origin/pivot point for rotation and scaling

### 2.4 Post-Processing Effects
- Screen-wide shader effects accessible from Lua
- Predefined effects: CRT scanlines, vignette, chromatic aberration, pixelation
- `gfx.effect(name, params)` - apply a post-processing effect
- Allow toggling effects at runtime for retro aesthetic

### 2.5 Layers / Draw Order
- Named rendering layers with explicit z-ordering
- `gfx.layer(name, z)` - set current drawing layer
- Ability to sort and reorder layers at runtime
- Per-layer camera offsets for parallax scrolling

### 2.6 Transparency & Blending Improvements
- Per-sprite alpha/opacity values
- Additional blending modes: additive, multiply, screen
- Configurable transparent color index (currently palette-based)

### 2.7 Tilemap Rendering Optimizations
- Frustum culling for large maps (only render visible tiles)
- Cached tile batching for static layers
- Animated tiles support (auto-cycling through tile frames)

### 2.8 Screen Transitions
- Built-in transition effects between scenes
- Fade in/out, wipe, circle close/open, dissolve
- `gfx.transition(type, duration, callback)` - trigger a screen transition

---

## 3. Physics & Collisions

### 3.1 Built-in Collision Detection
**Priority: High**
Currently there is no physics or collision system. `math.roverlap()` only checks rectangle overlap.

- AABB collision detection and resolution
- Circle-circle and circle-rectangle collisions
- `physics.overlap(a, b)` - check if two shapes overlap
- `physics.collide(a, b)` - check overlap and return collision normal + penetration depth
- Collision response helpers (push-out, bounce)

### 3.2 Simple Physics Engine
- Lightweight rigid body system for common game physics
- Gravity, velocity, acceleration per entity
- `physics.body(x, y, w, h)` - create a physics body
- `physics.step(dt)` - advance simulation
- Static vs dynamic bodies
- Basic friction and restitution

### 3.3 Raycasting
- `physics.raycast(x1, y1, x2, y2)` - cast a ray against collision shapes
- Returns hit point, normal, and distance
- Useful for line-of-sight, projectiles, and platformer ground detection

### 3.4 Tilemap Collision Integration
- Automatic collision generation from LDtk tile flags
- `map.solid(x, y)` - check if a tile is solid
- Tile-based collision resolution for platformers and top-down games

---

## 4. Animation System

### 4.1 Tween Library
**Priority: Medium**
The `juice` library provides easing functions, but there is no higher-level tween system.

- `tween.to(target, duration, {property = value}, easing)` - animate properties over time
- `tween.sequence({...})` - chain tweens sequentially
- `tween.parallel({...})` - run tweens simultaneously
- `tween.delay(duration)` - insert a delay in a sequence
- Auto-update system integrated with the game loop

### 4.2 Keyframe Animation
- Define keyframed animations with multiple properties
- Bezier curve interpolation between keyframes
- Export/import animations from simple JSON format

### 4.3 State Machine
- `anim.state_machine()` - create a finite state machine for animation states
- Define states (idle, walk, jump, attack) with transitions
- Condition-based automatic transitions
- Useful for character animation management

### 4.4 Timer Utilities
- `timer.after(duration, callback)` - execute after delay
- `timer.every(interval, callback)` - repeat at interval
- `timer.tween(duration, ...)` - shorthand for tween creation
- Pause/resume/cancel timer operations

---

## 5. Particle System

### 5.1 Built-in Particle Emitter
**Priority: Medium**
No particle system exists. This is commonly needed for explosions, fire, smoke, sparks, etc.

- `particles.emitter(config)` - create a particle emitter
- Configurable properties: lifetime, speed, direction, spread angle
- Color evolution over lifetime (using palette colors)
- Size variation and fade out
- Gravity and wind forces
- Emission rate and burst mode

### 5.2 Predefined Particle Presets
- Common presets: explosion, fire, smoke, rain, snow, sparks, bubbles, dust
- `particles.preset(name, x, y)` - quick particle effects
- Customizable presets as starting points

### 5.3 Sprite-Based Particles
- Use sprite indices as particle shapes (not just pixels/shapes)
- Animated sprite particles
- Rotation and scaling per particle

---

## 6. Camera System

### 6.1 Enhanced Camera Controls
**Priority: Medium**
Currently the camera only supports panning via `gfx.camera(x, y)`. No zoom or rotation.

- `gfx.camera_zoom(level)` - runtime zoom control (independent of game resolution)
- `gfx.camera_rotation(angle)` - camera rotation
- `gfx.camera_shake(intensity, duration)` - screen shake effect
- `gfx.camera_bounds(x, y, w, h)` - constrain camera to level bounds

### 6.2 Camera Follow
- `gfx.camera_follow(x, y, speed)` - smooth camera following
- Configurable dead zone (area around target where camera doesn't move)
- Lookahead (camera leads in the direction of movement)
- Configurable smoothing/damping

### 6.3 Multiple Viewports
- Split-screen support with multiple cameras
- Minimap viewport rendering
- Picture-in-picture for UI elements

---

## 7. UI Framework

### 7.1 Built-in UI Library
**Priority: Medium**
The SFX editor has a custom widget system (`widget.lua`) that could be generalized.

- Extract and generalize the SFX editor widget system into a reusable UI library
- Core widgets: Button, Label, Checkbox, Slider, TextInput, Panel, List
- Layout system: vertical/horizontal stacking, grid
- Event handling: on_click, on_change, on_hover
- Theming with palette colors

### 7.2 Dialog System
- `ui.dialog(text, options)` - display dialog boxes
- Typewriter text effect
- Character portraits
- Choice selection with callbacks
- Dialog scripting format for conversation trees

### 7.3 HUD Helpers
- Health bar / progress bar widget
- Inventory grid display
- Score display with animated number changes
- Toast notifications / popups

---

## 8. Networking

### 8.1 Simple Multiplayer
**Priority: Low**
No networking exists in the engine.

- WebSocket-based client for simple multiplayer
- `net.connect(url)` - connect to a game server
- `net.send(data)` - send data to server
- `net.on("message", callback)` - receive data
- Focus on turn-based or low-frequency updates (not real-time action)

### 8.2 Leaderboard / High Scores
- Simple HTTP API client for leaderboards
- `net.post_score(url, name, score)` - submit a score
- `net.get_scores(url, callback)` - fetch leaderboard
- Works with free services like Firebase or custom backends

### 8.3 Share & Replay
- Record game inputs for replay
- Export replay as shareable data
- Import and playback replays

---

## 9. Input & Controls

### 9.1 Gamepad Support
**Priority: Medium**
Only keyboard and mouse/touch are supported.

- Gamepad/joystick detection and input reading
- `ctrl.gamepad(index)` - access gamepad by index
- Analog stick values, D-pad, buttons, triggers
- Button mapping configuration
- Vibration/rumble support (where available)

### 9.2 Input Mapping System
- Named input actions: `ctrl.action("jump")` instead of `ctrl.pressed(keys.space)`
- Configurable key bindings per action
- Multiple keys/buttons per action
- Support for input combos (e.g., down + jump for drop-through)

### 9.3 Gesture Recognition
- Swipe detection (direction, speed)
- Pinch-to-zoom for mobile/touch
- Long press detection
- Double-tap detection
- Drag and drop helpers

### 9.4 Fix Key Handling Issues
- Fix FIXME in `KeyCode.kt`: distinguish left/right Shift, Ctrl, Alt
- Improve key repeat behavior for text input scenarios
- Add `ctrl.text_input()` for proper text entry support

---

## 10. Tilemaps & Level Design

### 10.1 Runtime Tilemap Editing
**Priority: Medium**
LDtk levels are read-only at runtime.

- `map.set(layer, x, y, tile)` - modify tiles at runtime
- `map.fill(layer, x, y, w, h, tile)` - fill rectangular area
- Enable dynamic level generation and destruction
- Save modified tilemaps to floppy

### 10.2 Auto-Tiling
- Rule-based auto-tiling (e.g., terrain auto-connects to neighbors)
- Predefined auto-tile templates (blob, corner, edge)
- Custom auto-tile rules defined in Lua

### 10.3 Procedural Generation Helpers
- `map.gen_noise(layer, config)` - generate maps from Perlin noise
- `map.gen_cellular(layer, config)` - cellular automata generation
- `map.gen_bsp(config)` - binary space partition for dungeons
- `map.gen_maze(config)` - maze generation algorithms

### 10.4 Pathfinding
- `map.pathfind(x1, y1, x2, y2)` - A* pathfinding on tilemap
- Configurable cost per tile type
- Diagonal movement option
- Path smoothing

---

## 11. Resource Management

### 11.1 Asset Packing
- Automatic texture atlas generation from individual sprites
- Optimized sprite sheet packing (bin-packing algorithm)
- CLI command: `tiny-cli pack` to generate optimized spritesheets

### 11.2 Asynchronous Resource Loading
- `resources.load(path, callback)` - non-blocking resource loading
- Loading progress tracking
- Loading screen support
- Lazy loading for large games

### 11.3 Resource Versioning
- Version-aware resource caching for web builds
- Cache busting for updated assets
- Differential updates (only reload changed resources)

### 11.4 Extended Format Support
- Aseprite (.ase/.aseprite) import for sprites and animations
- Tiled (.tmx) map support in addition to LDtk
- OGG/WAV audio file import and conversion

---

## 12. Scripting & Lua API

### 12.1 Coroutine Support
**Priority: High**
Coroutines make game scripting significantly easier for cutscenes, dialogs, and sequences.

- Expose Lua coroutines properly with engine integration
- `yield` / `resume` support integrated with the game loop
- `wait(seconds)` - pause execution for a duration
- `wait_until(condition)` - pause until a condition is met
- Useful for cutscenes, dialog sequences, and multi-frame operations

### 12.2 Scene / State Management
- `scene.switch(name)` - switch between game scenes
- `scene.push(name)` / `scene.pop()` - scene stack
- Scene lifecycle callbacks: `_init()`, `_enter()`, `_exit()`, `_update()`, `_draw()`
- Transition effects between scenes

### 12.3 Entity Component System (Lightweight)
- Simple ECS for organizing game objects
- `ecs.entity()` - create entity
- `ecs.component(name, data)` - attach components
- `ecs.system(filter, update)` - create systems that operate on matching entities
- Keep it simple and Lua-friendly, not over-engineered

### 12.4 Module System
- `require("module")` - import Lua modules from project files
- Namespace isolation between modules
- Standard library of reusable modules (math helpers, data structures, etc.)

### 12.5 Hot-Reload Improvements
- Preserve game state across script reloads
- Selective script reloading (only reload changed files)
- Error recovery: revert to last working script on Lua errors

### 12.6 Type Checking / Validation
- Optional runtime type checking for Lua function arguments
- `assert_type(value, "number")` - validate argument types
- Better error messages with line numbers and stack traces

---

## 13. Debugging & Profiling

### 13.1 In-Game Debug Overlay
**Priority: Medium**
The debugger exists but is external (web-based). An in-game overlay would be faster for common tasks.

- Toggle with a key (e.g., F12)
- Show FPS, draw call count, entity count
- Display collision boxes and physics shapes
- Grid overlay for tile-based games
- Variable watcher panel

### 13.2 Performance Profiler
- Frame time graph showing game update vs draw time
- Per-function execution time tracking in Lua
- Memory usage monitoring
- `debug.profile(function)` - profile a specific function
- Flame chart visualization in the web debugger

### 13.3 Console / REPL
- In-game console for executing Lua commands at runtime
- Command history and auto-completion
- Print game state, modify variables on the fly
- `console.show()` / `console.hide()` - toggle console

### 13.4 Recording & Playback
- Record game frames for bug reproduction
- Save and load game state snapshots
- Step-through frame-by-frame replay
- Useful for debugging intermittent bugs

---

## 14. CLI & Tooling

### 14.1 Project Templates
- `tiny-cli create --template platformer` - create from templates
- Templates: platformer, top-down RPG, puzzle, shoot-em-up, card game
- Each template includes starter code, sprites, and configuration
- Community template repository

### 14.2 Sprite Editor
**Priority: Medium**
A built-in sprite editor (like the existing SFX editor) would complete the tooling.

- Pixel art editor with palette constraint
- Sprite sheet editing with grid view
- Animation preview
- Basic tools: pencil, eraser, fill, line, rectangle, circle
- Undo/redo
- `tiny-cli sprite` - launch sprite editor

### 14.3 Map Editor
- Simple built-in tile map editor for quick prototyping
- Place tiles, set flags, define collision
- Export to LDtk-compatible format
- `tiny-cli map` - launch map editor

### 14.4 Dependency Management
- `tiny-cli add-lib <name>` - add community Lua libraries
- Central repository of reusable Lua modules
- Version management for dependencies
- `libs.json` or similar manifest file

### 14.5 Game Bundling Improvements
- Minify Lua scripts in production builds
- Compress assets in web exports
- Generate app icons from spritesheet
- Better web export with proper HTML template customization

---

## 15. Web Editor

### 15.1 Complete Web Editor
**Priority: High**
The web editor has multiple `TODO("Not yet implemented")` placeholders.

- Full code editor with Lua syntax highlighting
- Sprite sheet viewer/editor in browser
- Game preview with hot-reload
- File management (create, rename, delete scripts)
- Integration with the debugger
- Share games via URL

### 15.2 Collaborative Editing
- Real-time collaborative editing (like Google Docs)
- Multiple users editing the same game project
- Chat/comments system
- Useful for game jams and education

### 15.3 Game Gallery
- Public gallery of games created with Tiny
- Play games directly in the browser
- Fork/remix existing games
- Rating and commenting system

---

## 16. Export & Distribution

### 16.1 Mobile Export
**Priority: Low** (significant platform work required)

- Android APK/AAB export
- iOS build support (via Kotlin/Native or hybrid approach)
- Touch control configuration for mobile
- Screen orientation settings

### 16.2 Itch.io Integration
- `tiny-cli publish itch <game>` - direct publish to itch.io
- Automatic butler upload
- Version tagging
- Update existing published games

### 16.3 PWA Support
- Progressive Web App configuration for web exports
- Offline play support via service workers
- Install-to-home-screen prompt
- Manifest generation

### 16.4 Executable Size Reduction
- Minimize JDK bundling for desktop exports
- GraalVM native image compilation for smaller desktop builds
- WebAssembly target for web builds (future)

---

## 17. Documentation & Learning

### 17.1 Interactive Tutorials
- Step-by-step tutorials built into the CLI or web editor
- "Make a Pong game", "Make a Platformer", "Make a Puzzle Game"
- Progressive difficulty with each tutorial
- Code snippets with explanations

### 17.2 API Playground
- Interactive API documentation where users can try functions
- Live code examples for each Lua function
- "Run this example" buttons in docs

### 17.3 Cheat Sheet
- One-page printable reference card
- All Lua functions organized by library
- Common patterns and recipes
- Keyboard shortcuts reference

### 17.4 Video/GIF Generation
- `tiny-cli record` - record gameplay as GIF or video
- Configurable duration, resolution, and framerate
- Useful for documentation, marketing, and sharing

---

## 18. Testing & Quality

### 18.1 Lua Test Framework
- Built-in testing framework for game logic
- `test(name, function)` - define a test
- `assert_eq(a, b)` - assertion helpers
- `tiny-cli test` - run game tests from CLI
- Test coverage report

### 18.2 Headless Game Execution
- Run games without rendering for automated testing
- Simulate input sequences
- Capture game state for verification
- CI/CD integration for game testing

### 18.3 Expand Engine Test Coverage
- Add tests for `GfxLib`, `SprLib`, `ShapeLib`, `MapLib`, `CtrlLib`
- Add tests for rendering pipeline stages
- Platform-specific test suites for JVM and JS
- Integration tests for full game scenarios

---

## 19. Performance

### 19.1 Rendering Optimizations
- Sprite batching optimization (reduce draw calls)
- Dirty rectangle tracking (only redraw changed regions)
- Object pooling for frequently created/destroyed entities
- Texture atlas caching

### 19.2 Lua Performance
- Profile and optimize hot Lua paths
- Reduce `WrapperLuaTable` allocations in `SfxLib` (known issue)
- Cache frequently accessed Lua values
- Consider LuaJIT-compatible optimizations where possible

### 19.3 LWJGL Input Optimization
- Address the known LWJGL input polling performance issue
- Batch input queries per frame
- Cache cursor position (partially done, can be improved)

### 19.4 Memory Management
- Monitor and cap memory usage
- Asset streaming for large games
- Garbage collection tuning hints for Lua
- Resource unloading for scenes no longer in use

---

## 20. Accessibility

### 20.1 Screen Reader Support (Web)
- ARIA annotations for web builds
- Describe game state changes for screen readers
- Navigation mode for menus

### 20.2 Input Accessibility
- Fully remappable controls
- One-button mode support
- Adjustable input timing (hold duration, repeat rate)
- Switch/adaptive controller support

### 20.3 Visual Accessibility
- High-contrast palette option
- Configurable UI scaling
- Colorblind-friendly palette alternatives
- Flash/flicker reduction settings

### 20.4 Game Speed Control
- Global game speed adjustment
- Slow-motion mode for action games
- Pause and step-through for precise timing

---

## Summary by Priority

### High Priority
- Complete music playback system (1.1)
- Built-in text rendering (2.1)
- Collision detection (3.1)
- Coroutine support (12.1)
- Complete web editor (15.1)

### Medium Priority
- Tween library (4.1)
- Particle system (5.1)
- Camera enhancements (6.1, 6.2)
- Gamepad support (9.1)
- Runtime tilemap editing (10.1)
- In-game debug overlay (13.1)
- Sprite editor (14.2)
- Scene management (12.2)

### Low Priority
- Networking / multiplayer (8.1)
- Mobile export (16.1)
- UI framework (7.1)
- Collaborative editing (15.2)
- Accessibility (20.x)

---

## Bug Fixes (from codebase analysis)

These are existing issues found in the code that should be addressed:

1. **Line drawing bug** (`PrimitiveBatchStage.kt`) - Right-to-left lines may not render correctly
2. **Key distinction** (`KeyCode.kt` FIXME) - Left/right Shift, Ctrl, Alt not distinguished
3. **Grid size calculation** (`MapLib.kt` FIXME) - Incorrect grid size for some layer configurations
4. **Shader parameter binding** (`ShaderParameter.kt` TODO) - Missing bind/unbind for "In" parameters
5. **Mouse projection** - `MouseProject` interface returns `TODO` in some paths
