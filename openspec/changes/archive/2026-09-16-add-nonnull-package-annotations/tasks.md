## 1. Build Logic & Annotation Dependencies

- [x] 1.1 Centralize `jspecify` in `gradle/libs.versions.toml` and configure constraints in `parent/build.gradle.kts`.
- [x] 1.2 Configure `compileOnly("org.jspecify:jspecify:1.0.1")` in `build-logic/src/main/kotlin/njall.java-common-conventions.gradle.kts` and remove legacy JSR-305 and SpotBugs annotation dependencies.

## 2. Package-Level Nullness Annotations

- [x] 2.1 Annotate `package-info.java` files in `:common` (4 packages) with JSpecify `@NullMarked`.
- [x] 2.2 Annotate `package-info.java` files in `:data` (7 packages) with JSpecify `@NullMarked`.
- [x] 2.3 Annotate `package-info.java` files in `:api` (3 packages) with JSpecify `@NullMarked`.
- [x] 2.4 Annotate `package-info.java` files in `:server` (3 packages) with JSpecify `@NullMarked`.

## 3. Explicit Nullable Annotations

- [x] 3.1 Annotate nullable query methods in `:data` (`DefaultServerDAO.findServerEntity`) with `org.jspecify.annotations.Nullable`.
- [x] 3.2 Annotate nullable CLI options and execution methods in `:server` (`CliRunner`, `ServerCommand`, `MigrateCommand`, `RootCommand`, `CliConfigBuilder`, `MigrationOptions`, `ServerOptions`, `ServerApp`) with `org.jspecify.annotations.Nullable`.

## 4. Streamline Guice-Injected Null Checks & Update Unit Tests

- [x] 4.1 Remove redundant `requireNonNull` calls in `:common` (`HealthModule`) and update `:common` unit tests.
- [x] 4.2 Remove redundant `requireNonNull` calls in `:data` (`DefaultServerDAO`, `FlywayDatabaseMigrator`) and update `:data` unit tests.
- [x] 4.3 Remove redundant `requireNonNull` calls in `:api` (`AdminModule`, `DefaultAdminRoute`, `DefaultHealthCheckActorFactory`, `DefaultServerAdminActorFactory`, `HealthCheckActor`, `PekkoHealthCheck`, `ServerAdminActor`, `DefaultRootRoute`) and update `:api` unit tests.
- [x] 4.4 Remove redundant `requireNonNull` calls in `:server` (`DefaultServerManagerService`, `DefaultHttpServerService`, `CliRunner`, `MigrateCommand`, `RootCommand`, `ServerCommand`, `ServerApp`, `ServerModule`) and update `:server` unit tests.

## 5. Verification & Validation

- [x] 5.1 Run `wsl ./gradlew spotlessApply` and verify compilation across all submodules.
- [x] 5.2 Run `wsl ./gradlew check build` to verify Spotless, Checkstyle, SpotBugs, ErrorProne, unit tests, and JaCoCo coverage gates (85% line, 90% branch).
- [x] 5.3 Run `openspec validate add-nonnull-package-annotations --type change --strict` to validate change artifacts.
