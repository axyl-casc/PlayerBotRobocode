# Project Agents.md Guide for OpenAI Codex

This AGENTS.md file provides guidance for OpenAI Codex and other AI agents working with this codebase.

## Project Structure for OpenAI Codex Navigation

- `Launcher.java` – entry point that launches the PlayerBot
- `PlayerBot.java` – main bot implementation
- `lib/` – third party jars used during compilation
- `build.bat` – Windows build script that compiles and packages the bot
- `PlayerBot.sh` – simple shell script for compiling and running the bot on Unix

All source files reside in the repository root. There is no `src/` directory.

## Coding Conventions for OpenAI Codex

- Use **Java 11** for all new code
- Follow the existing style of four space indentation
- Prefer descriptive variable and method names
- Add comments when the logic is non‑trivial

## Build and Testing Requirements

No automated tests are present. To verify changes compile the project using:

```bash
javac -d build -cp "lib/*" PlayerBot.java Launcher.java
```

Successful compilation is required before submitting code.

## Pull Request Guidelines for OpenAI Codex

1. Include a clear description of the changes
2. Reference any related issues
3. Ensure the project compiles without errors
4. Keep PRs focused on a single concern

## Programmatic Checks for OpenAI Codex

Before submitting changes, run:

```bash
javac -d build -cp "lib/*" PlayerBot.java Launcher.java
```

The AGENTS.md file helps ensure OpenAI Codex follows these requirements.
