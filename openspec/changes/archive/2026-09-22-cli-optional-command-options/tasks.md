## 1. Options Records and Configuration Builder Refactoring

- [x] 1.1 Refactor `ServerOptions` to declare `Optional<T>` record components and compact constructor validation, preserving overloaded constructor accepting nullable values.
- [x] 1.2 Refactor `MigrationOptions` to declare `Optional<T>` record components and compact constructor validation, preserving overloaded constructor accepting nullable values.
- [x] 1.3 Refactor `CliConfigBuilder` to consume `Optional` accessors directly via `.ifPresent(...)` and accept `Optional<File>` in `withConfigFile(...)`.
- [x] 1.4 Update and run `CliConfigBuilderTest` in `:server` to verify that `withServerOptions`, `withMigrationOptions`, and `withConfigFile` handle `Optional` values correctly.

## 2. Command Accessor Untainting

- [x] 2.1 Refactor `RootCommand` so `configFile()` returns `Optional<File>`.
- [x] 2.2 Refactor `ServerCommand` so `host()`, `port()`, `name()`, `primaryDomain()`, and `adminContact()` return `Optional<T>`, and `resolveConfigFile()` delegates via `Optional.flatMap(...)`.
- [x] 2.3 Refactor `MigrateCommand` so `jdbcUrl()`, `username()`, `password()`, `schemas()`, `defaultSchema()`, `serverName()`, `primaryDomain()`, and `adminContact()` return `Optional<T>`, and `resolveConfigFile()` delegates via `Optional.flatMap(...)`.

## 3. Unit Test Verification

- [x] 3.1 Update `RootCommandTest` to assert `Optional<File>` using AssertJ `.isEmpty()` and `.contains(...)`.
- [x] 3.2 Update `ServerCommandTest` to assert `Optional<T>` option accessors using AssertJ `.contains(...)` and verify default constructor empty state.
- [x] 3.3 Update `MigrateCommandTest` to assert `Optional<T>` option accessors using AssertJ `.contains(...)` / `.hasValue(...)` and verify default constructor empty state.
- [x] 3.4 Execute `./gradlew :server:test` and verify 100% test pass rate in `:server`.

## 4. Quality Gates and Specification Validation

- [x] 4.1 Run `openspec validate cli-optional-command-options --type change --strict` to ensure change schema and delta spec compliance.
- [x] 4.2 Run `./gradlew spotlessApply` and `./gradlew spotlessCheck` to verify code formatting.
- [x] 4.3 Run `./gradlew check build` across all modules to satisfy Checkstyle, SpotBugs, ErrorProne, ArchUnit, and JaCoCo coverage gates.
