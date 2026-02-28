# Skill: Common Commands and Debugging

## Purpose

This skill provides guidance on how to develop in this environment with these
build tools.

## Technical Constraints

- The system uses gradle 9+ with the graddle wrapper (`./gradlew`) as the main
  entry point.
- The system is a multimodule project. a `<command>` can be run with either
  `./gradlew <command>` (to run it across all modules) or
  `./gradlew :<module>:<command>` (for a specific module)

## Specific Guidance

- Run gradle with `--stacktrace` when debugging.
- Always run `./gradlew spotlessApply check build` before submitting code.

### Common Commands

- `./gradlew check`: Executes the full suite of tests, static analysis, and
  formatting checks. No PR shall be merged if `./gradlew check` fails.
- `./gradlew spotlessApply`: Automatically corrects source code formatting
  according to the defined standard. Always run this before submitting.
- `./gradlew :batch:run --args="..."`: Executes the application in batch mode.
- `./gradlew :repl:run`: Initializes the interactive REPL session.
- `./gradlew build`: Builds the entire project
