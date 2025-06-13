# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Architecture Overview

Tiny is a Kotlin Multiplatform game engine with Lua scripting support that compiles to desktop (JVM) and web (JavaScript) platforms. The project is structured as a multi-module Gradle build:

- **tiny-engine**: Core multiplatform game engine (commonMain, jvmMain, jsMain)
- **tiny-cli**: JVM-based CLI tool for development workflows
- **tiny-doc**: Documentation generation using Asciidoctor
- **tiny-doc-annotations**: Annotations for documentation generation
- **tiny-doc-generator**: KSP-based documentation processor
- **tiny-web-editor**: Web-based editor interface
- **tiny-sample**: Sample games and examples

## Key Technologies

- **Kotlin Multiplatform**: Shared code between JVM and JS platforms
- **Lua**: Game scripting via luak library
- **OpenGL**: Graphics rendering via kgl and LWJGL
- **Ktor**: HTTP server for CLI serve command
- **KSP**: Documentation generation from code annotations

## Development Commands

### Building
```bash
./gradlew build                    # Build all modules
./gradlew test                     # Run all tests
./gradlew publishToMavenLocal      # Deploy to local maven
```

### Linting
```bash
make lint          # or ./gradlew ktlintCheck
make lintfix       # or ./gradlew ktlintFormat
```

### CLI Installation
```bash
make install       # Build and install CLI to ~/.bin/tiny-cli
```

### Documentation
```bash
make docs          # Generate documentation (requires CLI install)
```

## Architecture Details

### Platform Abstraction
The engine uses a Platform interface to abstract platform-specific functionality:
- `GlfwPlatform` for desktop (LWJGL/GLFW)
- `WebGlPlatform` for web (WebGL)

### Resource Management
Games are structured around:
- `_tiny.json`: Game configuration
- `game.lua`: Main game script
- Assets: sprites, sounds, levels (LDtk support)

### Lua API Organization
The engine exposes functionality through organized Lua libraries:
- `gfx`: Graphics operations
- `spr`: Sprite management  
- `sfx`: Sound effects
- `shape`: Drawing primitives
- `ctrl`: Input handling
- `map`: Level/tilemap operations

### Build Artifacts
The build produces several specialized artifacts:
- `tinyWebEngine`: JS engine for web deployment
- `tinyApiAsciidoctor`: Generated API documentation
- `tinyApiLuaStub`: Lua API stubs
- `tinyResources`: Packaged engine resources

## Development Workflow

1. Engine changes go in `tiny-engine/src/commonMain/kotlin`
2. CLI commands are in `tiny-cli/src/main/kotlin/com/github/minigdx/tiny/cli/command/`
3. Lua API libraries are in `tiny-engine/src/commonMain/kotlin/com/github/minigdx/tiny/lua/`
4. Tests follow the pattern `src/commonTest/kotlin` for shared tests
5. Platform-specific code uses `src/jvmMain` and `src/jsMain` directories

The project uses hot-reload for rapid development - games can be updated without restarting the engine.