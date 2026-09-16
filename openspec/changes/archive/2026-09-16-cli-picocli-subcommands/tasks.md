## 1. Dependencies and Build Configuration

- [x] 1.1 Add `picocli = "4.7.7"` version and library definition to `gradle/libs.versions.toml`.
- [x] 1.2 Add `api(libs.picocli)` dependency constraint to `parent/build.gradle.kts`.
- [x] 1.3 Add `implementation(libs.picocli)` dependency to `server/build.gradle.kts`.

## 2. Core CLI Implementation in `:server`

- [x] 2.1 Create `CliConfigBuilder` in `com.larpconnect.njall.server.cli` to convert CLI flags into Typesafe `Config` overlays and add comprehensive unit tests in `CliConfigBuilderTest`.
- [x] 2.2 Create `RootCommand` with global options (`-c/--config`, `-h/--help`, `-V/--version`) and default subcommand routing to `ServerCommand`.
- [x] 2.3 Create `ServerCommand` with HTTP server options (`-h/--host`, `-p/--port`, `--name`, `--primary-domain`, `--admin-contact`) and unit test option parsing and execution.
- [x] 2.4 Create `MigrateCommand` with Flyway options (`--jdbc-url`, `-u/--username`, `-p/--password`, `--schemas`, `--default-schema`, `--server-name`, `--primary-domain`, `--admin-contact`) and unit test option parsing and execution.
- [x] 2.5 Create `CliRunner` facade and `CliModule` Guice module in `com.larpconnect.njall.server.cli`.
- [x] 2.6 Wire `CliModule` into `ServerModule` and refactor `ServerApp` to delegate execution to `CliRunner`.

## 3. Integration & Acceptance Tests

- [x] 3.1 Update `integration/src/test/resources/features/database_migration.feature` to replace legacy `--migrate` scenarios with `migrate` subcommand scenarios, including database parameter overrides.
- [x] 3.2 Update `DatabaseMigrationSteps` step definitions to support the new `migrate` subcommand syntax and verify `:integration:test` passes.
- [x] 3.3 Remove obsolete `CliArgs` and `CliArgsTest`, and update `ServerAppTest` to verify `CliRunner` execution.

## 4. Verification & Quality Gates

- [x] 4.1 Run `./gradlew spotlessApply check build` across all modules ensuring JaCoCo coverage (85% line, 90% branch) and Checkstyle/SpotBugs compliance.
- [x] 4.2 Run `openspec validate cli-picocli-subcommands --type change --strict` to verify specification and task integrity.
