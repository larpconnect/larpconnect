## Why

While **Njall** adopted `@NullMarked` package-level defaults under ADR-0006, the project currently lacks static compiler enforcement to guarantee that null contracts are respected across all code paths. Furthermore, numerous classes retain defensive runtime `Objects.requireNonNull` and `!= null` checks on contractually non-null types, constructors check unannotated fields for nullity, and test suites redundantly assert `.isNotNull()` on Guice injector outputs. Enabling ErrorProne nullity checks and removing dead defensive checks eliminates clutter, guarantees compile-time safety, and prevents regressions.

## What Changes

- **Upgrade ErrorProne Toolchain**: Upgrade `build-logic` ErrorProne core dependency from 2.36.0 to 2.50.0 (aligning with `libs.versions.toml`) to support modern JSpecify checkers.
- **Enable ErrorProne Nullity Checks**: Enforce `AddNullMarkedToPackageInfo`, `ParameterMissingNullable`, and `RedundantNullCheck` as build errors across all Java compilation tasks.
- **Enforce Package NullMarked Coverage**: Add `@NullMarked` to `:test` **module** `package-info.java`, completing 100% `@NullMarked` coverage across all production Java packages in the repository.
- **Resolve Existing Compiler Warnings & Violations**:
  - Annotate [TraceContext.java](../../../../common/src/main/java/com/larpconnect/njall/common/telemetry/TraceContext.java) header parameter with `@Nullable String headerValue`.
  - Fix array nullability syntax on CLI entry points ([CliRunner.java](../../../../server/src/main/java/com/larpconnect/njall/server/cli/CliRunner.java) and [ServerApp.java](../../../../server/src/main/java/com/larpconnect/njall/server/ServerApp.java)) to `String @Nullable [] args` following JSpecify type-use rules.
  - Simplify [UserAdminActor.java](../../../../api/src/main/java/com/larpconnect/njall/api/admin/UserAdminActor.java) to omit redundant null check on `cmd.username()`.
  - Streamline [DefaultAdminUserDAO.java](../../../../data/src/main/java/com/larpconnect/njall/data/dao/DefaultAdminUserDAO.java) mapper to be strictly non-null, removing downstream `requireNonNull` calls.
  - Remove redundant null checks in [AdminDatabaseHealthCheck.java](../../../../data/src/main/java/com/larpconnect/njall/data/health/AdminDatabaseHealthCheck.java) and [ActiveSessionFactories.java](../../../../data/src/main/java/com/larpconnect/njall/data/session/ActiveSessionFactories.java).
- **Eliminate Constructor Null Checks on Non-Nullable Fields**:
  - Remove defensive `requireNonNull` checks from record compact constructors and class constructors across 13 classes ([ServerOptions](../../../../server/src/main/java/com/larpconnect/njall/server/cli/ServerOptions.java), [MigrationOptions](../../../../server/src/main/java/com/larpconnect/njall/server/cli/MigrationOptions.java), [CliConfigBuilder](../../../../server/src/main/java/com/larpconnect/njall/server/cli/CliConfigBuilder.java), [CommonModule](../../../../common/src/main/java/com/larpconnect/njall/common/CommonModule.java), [ConfigModule](../../../../common/src/main/java/com/larpconnect/njall/common/config/ConfigModule.java), [ServerConfig](../../../../common/src/main/java/com/larpconnect/njall/common/config/ServerConfig.java), [TraceContext](../../../../common/src/main/java/com/larpconnect/njall/common/telemetry/TraceContext.java), [Server](../../../../data/src/main/java/com/larpconnect/njall/data/domain/Server.java), [ServerContact](../../../../data/src/main/java/com/larpconnect/njall/data/domain/ServerContact.java), [StudioLookup](../../../../data/src/main/java/com/larpconnect/njall/data/domain/StudioLookup.java), [AdminRole](../../../../data/src/main/java/com/larpconnect/njall/data/domain/AdminRole.java), [AdminUser](../../../../data/src/main/java/com/larpconnect/njall/data/domain/AdminUser.java), and [AdminUserEntity](../../../../data/src/main/java/com/larpconnect/njall/data/dao/AdminUserEntity.java)).
  - Retire obsolete unit test methods that intentionally pass `null` to these constructors and assert `NullPointerException`.
- **Eliminate Guice Result Null Checks**:
  - Remove redundant `assertThat(...).isNotNull()` checks on Guice `createInjector` and `getInstance` calls across 10 test classes, asserting specific instance types or behaviors directly.

## Capabilities

### New Capabilities

None.

### Modified Capabilities

- `nullability-contracts`: Extends nullability contracts to enforce ErrorProne compile-time verification (`AddNullMarkedToPackageInfo`, `ParameterMissingNullable`, `RedundantNullCheck`), eliminate redundant constructor null checks on unannotated fields, and eliminate Guice result null checks in tests and production code.

## Impact

- **Build Logic**: [build-logic/src/main/kotlin/njall.java-common-conventions.gradle.kts](../../../../build-logic/src/main/kotlin/njall.java-common-conventions.gradle.kts) updated to configure ErrorProne 2.50.0 and enable the three checks as errors.
- **Production Code**: Impact across `:common`, `:data`, `:api`, `:server`, and `:test` **modules** removing dead null checks and clarifying contracts.
- **Unit Tests**: Retires approximately 25 obsolete constructor NPE test cases and cleans up Guice assertions. JaCoCo coverage gates (85% line, 90% branch) remain fully enforced.
