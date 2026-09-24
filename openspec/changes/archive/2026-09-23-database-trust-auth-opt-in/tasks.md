## 1. Common Configuration Baseline and Test Overlay

- [x] 1.1 Add test classpath overlay `common/src/test/resources/application.conf` declaring `larpconnect.data.database.trust-auth = true`.
- [x] 1.2 Update `common/src/main/resources/reference.conf` to declare `trust-auth = false` with environment variable substitution `${?LARPCONNECT_DATA_DATABASE_TRUST_AUTH}`.
- [x] 1.3 Verify `:common` module compilation and tests pass via `./gradlew :common:check`.

## 2. Data Plane Configuration Records and Invariant Enforcement

- [x] 2.1 Update `SessionConfig` record to include `trustAuth`, implement fail-fast validation in constructor and `fromConfig`, and update factory methods.
- [x] 2.2 Update `MigrationConfig` record to include `trustAuth`, implement fail-fast validation in constructor and `fromConfig`, and update factory methods.
- [x] 2.3 Update `DatabaseConfig.fromConfig` to resolve global `trust-auth` and layer profile-specific overrides.
- [x] 2.4 Update `DefaultDataSourceFactory` and `DefaultSessionFactoryFactory` to honor `trustAuth`.
- [x] 2.5 Update and expand unit tests in `:data` (`SessionConfigTest`, `MigrationConfigTest`, `DatabaseConfigTest`, `DatabaseConfigModuleTest`, `DataModuleTest`, `DefaultSessionFactoryFactoryTest`) covering valid passwords, explicit trust-auth, and fail-fast rejections.
- [x] 2.6 Verify `:data` module compilation and tests pass via `./gradlew :data:check`.

## 3. CLI Migrate Subcommand and Configuration Builder

- [x] 3.1 Add `--trust-auth` option to `MigrateCommand` and `MigrationOptions` in `:server`.
- [x] 3.2 Update `CliConfigBuilder` to map `trustAuth` option to `larpconnect.data.database.migration.trust-auth`.
- [x] 3.3 Update CLI unit tests in `:server` (`MigrateCommandTest`, `CliConfigBuilderTest`).
- [x] 3.4 Verify `:server` module compilation and tests pass via `./gradlew :server:check`.

## 4. Verification and Specification Validation

- [x] 4.1 Run full build and test verification via `./gradlew check build` ensuring JaCoCo, Spotless, Checkstyle, SpotBugs, and ErrorProne gates pass.
- [x] 4.2 Verify Docker Compose container orchestration compatibility via `./gradlew composeStart` and `./gradlew composeDown`.
