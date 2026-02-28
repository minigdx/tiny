# Contributing to Tiny

Thanks for your interest in contributing to Tiny! This guide will help you get started.

## Prerequisites

- **JDK 17** or later
- **Gradle** (wrapper included in the project)
- **Make** (optional, for convenience commands)

## Setup

```bash
git clone https://github.com/minigdx/tiny.git
cd tiny
./gradlew build
```

## Development Workflow

### Building

```bash
./gradlew build                    # Build all modules
./gradlew publishToMavenLocal      # Deploy to local Maven repository
```

### Testing

```bash
./gradlew test                     # Run all tests
./gradlew :tiny-engine:test        # Run tests for a specific module
./gradlew :tiny-engine:jvmTest     # Run JVM-specific tests
./gradlew :tiny-engine:jsTest      # Run JS-specific tests
```

### Linting

```bash
make lint       # or ./gradlew ktlintCheck
make lintfix    # or ./gradlew ktlintFormat
```

### Installing the CLI locally

```bash
make install    # Builds and installs tiny-cli to ~/.bin/tiny-cli
```

## Project Structure

| Module                  | Description                              |
|-------------------------|------------------------------------------|
| `tiny-engine`           | Core multiplatform game engine           |
| `tiny-cli`              | CLI tool for development workflows       |
| `tiny-doc`              | Documentation (Asciidoctor)              |
| `tiny-doc-annotations`  | Annotations for doc generation           |
| `tiny-doc-generator`    | KSP-based documentation processor        |
| `tiny-web-editor`       | Web-based editor interface               |
| `tiny-sample`           | Sample games and examples                |

## Making Changes

1. Fork the repository
2. Create a feature branch from `main`
3. Make your changes
4. Run tests: `./gradlew test`
5. Run the linter: `make lint`
6. Open a Pull Request against `main`

## Code Style

This project uses [ktlint](https://pinterest.github.io/ktlint/) for Kotlin code formatting. Run `make lintfix` to auto-fix formatting issues before submitting.

## Reporting Issues

Please use [GitHub Issues](https://github.com/minigdx/tiny/issues) to report bugs or request features. Include:

- Steps to reproduce the issue
- Expected vs actual behavior
- Platform (desktop/web) and OS
- Tiny version
