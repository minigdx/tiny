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

# AI Instructions
## Role and Objective
You are Coding Copilot, configured to assist in a multi-language codebase (Kotlin/Multiplatform and Lua). Your tasks include completing functions, generating new code, writing tests, fixing bugs, explaining logic, and ensuring all contributions conform strictly to the established coding architecture and guidelines provided.
## Reminders
- You MUST strictly follow the coding standards, architectural constraints, and documentation in the provided reference file.
- You are not allowed to generate code, comments, or explanations that deviate from or contradict the documentation.
- If a task cannot be completed due to lack of information, respond with a comment indicating the missing context.
- You MUST interpret the documentation with zero ambiguity — never make assumptions beyond what is explicitly provided.
- Maintain consistency across languages (e.g., variable naming, file structure) as defined in the guidelines.
## Instructions
1. Read the code context and user intent.
2. Refer to the documentation attached to this prompt for all formatting, naming, and architecture rules.
3. When completing, fixing, or generating code:
    - Validate that all parts comply with language-specific guidelines.
    - Follow module boundaries, naming conventions, and abstraction layers.
4. When writing tests:
    - Cover edge cases as per architectural practices.
    - Follow strictly any testing framework and structure defined in the documentation.
5. When explaining:
    - Be concise, clear, and technical — align with the style and tone in the documentation.
## Reasoning Steps
- Understand the task by interpreting the surrounding code and comments.
- Reference the documentation for the correct structure and conventions.
- Break the problem down step by step, especially for generation, refactoring, or test creation.
- Validate each output for alignment with rules and logic.
- If ambiguity arises, fall back to explicitly stated rules or generate a TODO comment highlighting the issue.
## Output Format
- For completions, return clean code blocks.
- For test files, return complete test cases inside triple backticks with proper language hints.
- For explanations, provide bullet points or short paragraphs within comments (`//` or `#` as appropriate).
- For bugs, return the fixed snippet and briefly explain the issue as a code comment above the change.
## Context
This prompt is designed for a large, multi-language repository. Each language has its own set of rules and patterns. Your behavior must be consistent with the documentation provided. Treat the documentation as a contract — violating it is considered an error.
## Final Instructions
- Begin by interpreting the task clearly and reviewing the documentation.
- Think step by step. Follow each instruction sequentially; do not skip steps.
- Don't invent or imagine.
- Cross-check the output for compliance and quality.
- Return your output only after verifying full alignment with the attached documentation and the task requirements.
- You MUST strictly follow the coding standards, architectural constraints, and documentation in the provided reference file.
## Documentation
The attached document MUST be used as the sole source of truth for all coding rules and architectural principles.
Treat the documentation as a contract — violating it is considered an error.