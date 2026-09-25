## Why

In **Njall**, all production packages declare `@NullMarked`, establishing compile-time guarantees that parameters, return types, and fields are strictly non-null unless explicitly marked with `@Nullable`. Despite this contract, defensive runtime null checks (`Objects.requireNonNull` and `!= null`) and corresponding unit tests asserting `NullPointerException` persisted across configuration classes, factories, **DAO** implementations, and **API plane** request models for fields and parameters that are not annotated with `@Nullable`. These redundant checks and tests pollute the codebase, add synthetic branch overhead, and violate the principle of relying on static type-system guarantees under JSpecify and ErrorProne.

## What Changes

- **Eliminate Unannotated Runtime Null Checks**:
  - Remove defensive `Objects.requireNonNull` and conditional null checks on all parameters and fields that lack explicit `@Nullable` annotations across configuration classes ([MigrationConfig](../../../../data/src/main/java/com/larpconnect/njall/data/config/MigrationConfig.java), [DatabaseConfig](../../../../data/src/main/java/com/larpconnect/njall/data/config/DatabaseConfig.java), [SessionConfig](../../../../data/src/main/java/com/larpconnect/njall/data/config/SessionConfig.java), [ServerConfig](../../../../common/src/main/java/com/larpconnect/njall/common/config/ServerConfig.java)).
  - Remove redundant parameter null assertions in service and resource factories ([DefaultSessionFactoryFactory](../../../../data/src/main/java/com/larpconnect/njall/data/session/DefaultSessionFactoryFactory.java), [DefaultDataSourceFactory](../../../../data/src/main/java/com/larpconnect/njall/data/datasource/DefaultDataSourceFactory.java), [DefaultFlywayFactory](../../../../data/src/main/java/com/larpconnect/njall/data/migration/DefaultFlywayFactory.java)).
  - Remove redundant argument null checks in **DAO** query and mutation methods ([DefaultServerDAO](../../../../data/src/main/java/com/larpconnect/njall/data/dao/DefaultServerDAO.java), [DefaultStudioDAO](../../../../data/src/main/java/com/larpconnect/njall/data/dao/DefaultStudioDAO.java)).
  - Remove redundant parameter checks and compact constructors in **API plane** directives and DTO records ([DefaultTracingDirective](../../../../api/src/main/java/com/larpconnect/njall/api/telemetry/DefaultTracingDirective.java), [CreateUserRequest](../../../../api/src/main/java/com/larpconnect/njall/api/admin/model/CreateUserRequest.java), [CreateStudioRequest](../../../../api/src/main/java/com/larpconnect/njall/api/admin/model/CreateStudioRequest.java), [CreateRoleRequest](../../../../api/src/main/java/com/larpconnect/njall/api/admin/model/CreateRoleRequest.java), [AdminErrorResponse](../../../../api/src/main/java/com/larpconnect/njall/api/admin/model/AdminErrorResponse.java)).
- **Retire Obsolete NPE Unit Tests**:
  - Delete all unit test methods asserting `NullPointerException` when passing `null` to unannotated constructor parameters, factory methods, **DAO** methods, and directives.
  - Retain runtime null validation and NPE testing exclusively where parameters or fields are explicitly marked `@Nullable` with defined null-handling semantics.
- **Normalize Test Fixtures**:
  - Convert multiline string concatenations in configuration tests to text blocks to maintain zero ErrorProne warnings under fatal `-Werror`.

## Capabilities

### New Capabilities

None.

### Modified Capabilities

- `nullability-contracts`: Broadens the prohibition of redundant runtime null checks and NPE unit tests from record constructors to all methods, constructors, factories, and DAOs, establishing that unannotated parameters and fields rely strictly on compile-time non-null contracts.

## Impact

- **Production Code**: Removed defensive null checks across `:data`, `:common`, and `:api` **modules**, preserving clean static type guarantees and reducing clutter.
- **Unit Tests**: Removed over 20 obsolete NPE test methods across `:data`, `:common`, and `:api` test suites.
- **Coverage & Quality Gates**: All JaCoCo thresholds (>=85% line coverage, >=90% branch coverage) and static analysis tools (Checkstyle, SpotBugs, ErrorProne, Spotless) pass without regressions.
