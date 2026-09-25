## 1. Build Logic & Toolchain Configuration

- [x] 1.1 Upgrade ErrorProne to version 2.50.0 in `build-logic/src/main/kotlin/njall.java-common-conventions.gradle.kts` and configure `AddNullMarkedToPackageInfo`, `ParameterMissingNullable`, and `RedundantNullCheck` as fatal compilation errors.
- [x] 1.2 Add `@NullMarked` annotation to `test/src/main/java/com/larpconnect/njall/test/package-info.java` to achieve 100% package null-marking.

## 2. Common Module Streamlining

- [x] 2.1 Annotate `headerValue` with `@Nullable String headerValue` in `TraceContext.parseTraceparent` in [TraceContext.java](../../../../common/src/main/java/com/larpconnect/njall/common/telemetry/TraceContext.java).
- [x] 2.2 Remove defensive `Objects.requireNonNull` constructor checks in `TraceContext`, `CommonModule`, `ConfigModule`, and `ServerConfig`.
- [x] 2.3 Remove obsolete constructor NPE test cases in `ServerConfigTest` and verify `:common:test` passes cleanly.

## 3. Data Module Streamlining

- [x] 3.1 Refactor `AdminUserEntity` constructor to assign `roles` directly without `roles != null` fallback and update test caller in `DefaultAdminUserDAOTest` to pass `Set.of()`.
- [x] 3.2 Refactor `DefaultAdminUserDAO.toUser` to strictly non-null mapping and eliminate downstream `requireNonNull(toUser(...))` calls.
- [x] 3.3 Simplify `AdminDatabaseHealthCheck.evaluatePingResult` to omit redundant null check on `result`.
- [x] 3.4 Streamline `ActiveSessionFactories.register` to take non-null `SessionFactory`, removing defensive null check and updating `ActiveSessionFactoriesTest`.
- [x] 3.5 Remove defensive constructor `Objects.requireNonNull` checks in `Server`, `ServerContact`, `StudioLookup`, `AdminRole`, and `AdminUser`.
- [x] 3.6 Remove obsolete constructor NPE test cases in `ServerTest` and `AdminDomainTest`.
- [x] 3.7 Remove redundant Guice `.isNotNull()` assertions in `DataModuleTest`, `DatabaseConfigModuleTest`, `DaoModuleTest`, and `SessionModuleTest`.
- [x] 3.8 Remove redundant `url != null` check in `DefaultSessionFactoryFactoryTest` mock driver and verify `:data:test` passes cleanly.

## 4. API Module Streamlining

- [x] 4.1 Remove redundant `cmd.username() == null` check in `UserAdminActor.validateCreateUserCommand`.
- [x] 4.2 Verify `:api:test` passes cleanly.

## 5. Server Module Streamlining

- [x] 5.1 Standardize CLI array argument parameter signatures to `String @Nullable [] args` in `CliRunner` and `ServerApp`.
- [x] 5.2 Remove defensive constructor `Objects.requireNonNull` checks in `ServerOptions`, `MigrationOptions`, and `CliConfigBuilder`.
- [x] 5.3 Remove redundant null check in `ServerManagerServiceTest.terminateSystem`.
- [x] 5.4 Remove redundant Guice `.isNotNull()` assertions in `ServerModuleTest`, `ServerBindingModuleTest`, and `CliRunnerTest`.
- [x] 5.5 Verify `:server:test` passes cleanly.

## 6. Test Module & End-to-End Verification

- [x] 6.1 Remove redundant Guice `assertThat(injector).isNotNull()` assertion in `TestModuleTest`.
- [x] 6.2 Execute `openspec validate enforce-nullity-contracts --type change --strict` to ensure planning artifacts are structurally valid.
- [x] 6.3 Execute full quality verification via `wsl ./gradlew check build` ensuring JaCoCo (85% line, 90% branch), Spotless, Checkstyle, SpotBugs, ErrorProne, and ArchUnit gates pass cleanly across all **modules**.
