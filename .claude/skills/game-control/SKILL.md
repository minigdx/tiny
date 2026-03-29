---
name: game-control
description: Control a running Tiny game engine via HTTP. Send key presses, releases, and taps to interact with the game. Use when the user asks to play, control, test, or interact with a running game.
allowed-tools: Bash(curl:*), Bash(sleep:*), Read, mcp__playwright__browser_take_screenshot, mcp__playwright__browser_navigate, mcp__playwright__browser_snapshot
argument-hint: [action] [key or instructions]
---

# Game Control Skill

You can control a running Tiny game engine by sending HTTP requests to the debug server.

## Server

The game debug server runs on `http://localhost:8081` by default (started with `tiny-cli run`).

## Available Endpoints

### List available keys
```bash
curl -s http://localhost:8081/control/keys
```

### Press a key (stays pressed until released)
```bash
curl -s -X POST "http://localhost:8081/control/press?key=KEY_NAME"
```

### Release a key
```bash
curl -s -X POST "http://localhost:8081/control/release?key=KEY_NAME"
```

### Tap a key (press + auto-release after ~50ms)
```bash
curl -s -X POST "http://localhost:8081/control/tap?key=KEY_NAME"
```

## Available Keys

Direction: `ARROW_LEFT`, `ARROW_RIGHT`, `ARROW_UP`, `ARROW_DOWN`
Action: `SPACE`, `ENTER`, `ESCAPE`, `TAB`, `BACKSPACE`, `DELETE`
Letters: `A` through `Z`
Numbers: `NUM0` through `NUM9`
Modifiers: `SHIFT`, `CTRL`, `ALT`
Function: `F1` through `F12`

## Usage Patterns

### Tap a key once (one-shot action like jump or shoot)
```bash
curl -s -X POST "http://localhost:8081/control/tap?key=SPACE"
```

### Hold a key for sustained movement
```bash
# Hold right for 500ms then release
curl -s -X POST "http://localhost:8081/control/press?key=ARROW_RIGHT"
sleep 0.5
curl -s -X POST "http://localhost:8081/control/release?key=ARROW_RIGHT"
```

### Multiple rapid taps
```bash
for i in $(seq 1 5); do
  curl -s -X POST "http://localhost:8081/control/tap?key=SPACE"
  sleep 0.1
done
```

### Simultaneous keys (e.g., diagonal movement)
```bash
curl -s -X POST "http://localhost:8081/control/press?key=ARROW_RIGHT"
curl -s -X POST "http://localhost:8081/control/press?key=ARROW_UP"
sleep 0.5
curl -s -X POST "http://localhost:8081/control/release?key=ARROW_RIGHT"
curl -s -X POST "http://localhost:8081/control/release?key=ARROW_UP"
```

## How to Use

When the user provides `$ARGUMENTS`:

1. **If arguments describe an action** (e.g., "move right", "jump", "press space 3 times"):
   - Translate the instruction into the appropriate curl commands
   - Execute them

2. **If arguments are a key name** (e.g., "SPACE", "ARROW_LEFT"):
   - Tap that key once

3. **If no arguments or "help"**:
   - List the available keys by calling `GET /control/keys`
   - Show usage examples

4. **If "play" or complex instructions** (e.g., "play the game", "explore the level"):
   - Take a screenshot first to see the game state (if possible via the serve endpoint or playwright)
   - Send appropriate inputs based on what you observe
   - Iterate: act, observe, act again

## Error Handling

- If curl fails with connection refused, the game is not running. Tell the user to start it with `tiny-cli run`.
- If the response contains `"error":"Engine not ready"`, the game is still loading. Wait a moment and retry.
- If the response contains `"error":"Unknown key"`, check the key name against the available keys list.

## Response Format

All endpoints return JSON:
- Success: `{"ok":true,"action":"press","key":"ARROW_LEFT"}`
- Error: `{"error":"Missing 'key' query parameter"}`
