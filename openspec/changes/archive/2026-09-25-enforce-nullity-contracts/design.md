## Context

**Njall** adopted JSpecify 1.0.1 `@NullMarked` package-level defaults in [ADR-0006](../../../../adr/0006-package-level-nonnull-defaults-and-nullability-conventions.md). While packages under `src/main/java` declare `@NullMarked`, the Gradle compilation toolchain was not configured with ErrorProne checks to enforce that nullability contracts are respected. Furthermore, defensive programming patterns remain pervasive: constructors and records perform runtime `Objects.requireNonNull` validation on non-nullable fields, and unit tests redundantly assert `.isNotNull()` on Guice injector outputs.

This design establishes automated static compiler enforcement using ErrorProne 2.50.0 and eliminates dead defensive checks across all **modules**.

### C4 Component Diagram: Compiler Static Analysis Pipeline

```
+-----------------------------------------------------------------------------------------+
|                                BUILD & COMPILATION PIPELINE                             |
+-----------------------------------------------------------------------------------------+
|                                                                                         |
|   +---------------------------------------------------------------------------------+   |
|   |                        build-logic (njall.java-common-conventions)              |   |
|   |   - Configures Java 25 Toolchain with -Werror                                   |   |
|   |   - Resolves errorprone("com.google.errorprone:error_prone_core:2.50.0")        |   |
|   |   - Enables: AddNullMarkedToPackageInfo, ParameterMissingNullable,              |   |
|   |              RedundantNullCheck as ERROR                                        |   |
|   +---------------------------------------+-----------------------------------------+   |
|                                           |                                             |
|                                           v                                             |
|   +---------------------------------------------------------------------------------+   |
|   |                        JavaCompile Task (javac + ErrorProne Plugin)             |   |
|   |   - Analyzes AST and type annotations across all packages                       |   |
|   |   - Validates @NullMarked presence on package-info.java                         |   |
|   |   - Enforces @Nullable requirement when null is checked                         |   |
|   |   - Rejects null-checks on statically non-null variables                        |   |
|   +-------------------+-------------------+-------------------+---------------------+   |
|                       |                   |                   |                         |
|                       v                   v                   v                         v
|              +-----------------+ +-----------------+ +-----------------+ +-----------------+
|              |     :common     | |      :data      | |      :api       | |     :server     |
|              |   (Contracts)   | |  (Data plane)   | |   (API plane)   | |  (Application)  |
|              +-----------------+ +-----------------+ +-----------------+ +-----------------+
|                                                                                         |
+-----------------------------------------------------------------------------------------+
```

## Goals / Non-Goals

**Goals:**
- Upgrade ErrorProne in `build-logic` to 2.50.0 and configure `AddNullMarkedToPackageInfo`, `ParameterMissingNullable`, and `RedundantNullCheck` as fatal compilation errors.
- Ensure 100% `@NullMarked` package declaration across all production and test utility packages.
- Clean up all existing compile-time violations across `:common`, `:data`, `:api`, and `:server`.
- Remove defensive `Objects.requireNonNull` and `!= null` checks on unannotated constructor parameters and record components across all 13 identified classes.
- Remove redundant Guice `isNotNull()` assertions in test suites and production code.
- Retire obsolete unit test methods that intentionally pass `null` to non-nullable constructor parameters.
- Pass `./gradlew check build` cleanly with zero Spotless, Checkstyle, SpotBugs, ErrorProne, or JaCoCo coverage failures.

**Non-Goals:**
- Introducing external nullability linters outside ErrorProne (e.g., NullAway or Checker Framework).
- Modifying database entity nullable column mappings (Hibernate entity fields remain aligned with the database schema).
- Changing public REST API JSON payloads or schemas.

## Decisions

### Decision 1: Upgrade ErrorProne to 2.50.0 in Build Logic
- **Choice**: Update `njall.java-common-conventions.gradle.kts` to resolve `error_prone_core:2.50.0` from `libs.versions.toml`.
- **Rationale**: `AddNullMarkedToPackageInfo` was introduced in modern ErrorProne releases and is not supported by legacy 2.36.0. Upgrading adheres to [AGENTS.md](../../../../AGENTS.md) Section 3 requirement to use the latest stable library versions.
- **Alternatives Considered**: Retaining 2.36.0 and omitting `AddNullMarkedToPackageInfo`. Rejected because package-level enforcement is essential to ensure future packages do not omit `@NullMarked`.

### Decision 2: Remove Constructor Null Checks on Non-Nullable Fields (Option A)
- **Choice**: Remove defensive `requireNonNull` checks from record compact constructors and standard constructors for all fields not explicitly annotated `@Nullable`.
- **Rationale**: In a `@NullMarked` package, all parameters and record components are statically non-null by contract. Having runtime null checks on unannotated fields generates dead branches and directly violates the compile-time guarantees established by [ADR-0006](../../../../adr/0006-package-level-nonnull-defaults-and-nullability-conventions.md).
- **Alternatives Considered**: Keeping runtime `requireNonNull` checks by marking all record components `@Nullable`. Rejected because domain objects like `Server` and `ServerConfig` require non-null values for core fields (e.g. `id`, `name`).

### Decision 3: Retire Redundant Constructor NPE Unit Tests
- **Choice**: Delete unit test methods that assert `NullPointerException` on constructor parameters that are contractually non-null.
- **Rationale**: With compile-time enforcement, passing `null` to these constructors is a type error. Tests should verify valid domain invariants, not impossible null states. Deleting these tests also eliminates compiler warnings in test suites.
- **Alternatives Considered**: Suppressing ErrorProne warnings in tests while keeping NPE assertions. Rejected because retaining tests for contracts that no longer exist produces misleading documentation and fragile test suites.

### Decision 4: Standardize on JSpecify Type-Use Syntax for Nullable Arrays
- **Choice**: Declare nullable array parameters as `String @Nullable [] args` on [CliRunner.java](../../../../server/src/main/java/com/larpconnect/njall/server/cli/CliRunner.java) and [ServerApp.java](../../../../server/src/main/java/com/larpconnect/njall/server/ServerApp.java).
- **Rationale**: In JSpecify, `@Nullable String[] args` designates the elements as nullable while leaving the array reference non-null. Placing `@Nullable` between the type and the brackets (`String @Nullable []`) correctly designates the array reference itself as nullable, resolving `RedundantNullCheck` cleanly.
- **Alternatives Considered**: Disallowing `null` arrays entirely by requiring `String[] args` and forcing callers to pass `new String[0]`. Rejected because standard Java `main(String[] args)` and test runners often pass `null` or empty arrays interchangeably.

### Decision 5: Remove Redundant Guice Result Assertions
- **Choice**: Remove `assertThat(injector).isNotNull()` and `assertThat(injector.getInstance(...)).isNotNull()` across all 10 affected test classes.
- **Rationale**: Guice guarantees non-null returns; a resolution failure throws `ProvisionException` or `ConfigurationException`. Asserting non-null tests Guice framework invariants rather than application behavior.
- **Alternatives Considered**: Retaining `.isNotNull()` chained with other assertions. Rejected because `.isNotNull()` is redundant when immediately followed by `.isInstanceOf(...)` or `.isSameAs(...)`.

## Risks / Trade-offs

- **[Risk] Test Coverage Drops**: Removing constructor `requireNonNull` branches and deleting obsolete NPE tests could reduce JaCoCo line or branch metrics.
  - *Mitigation*: Removing `requireNonNull` actually removes uncovered conditional branches, improving branch coverage. We will run `./gradlew jacocoTestCoverageVerification` on every **module** to ensure line (>=85%) and branch (>=90%) thresholds remain exceeded.
- **[Risk] Unchecked Callers at External Boundaries**: If unvalidated JSON deserialization passes null to a record constructor that lacks `requireNonNull`, a null could theoretically enter domain logic.
  - *Mitigation*: Boundary DTOs (e.g., in Jackson or Picocli) validate input at deserialization time, isolating internal domain records from untrusted nulls.

## Migration Plan

1. Update `build-logic` to ErrorProne 2.50.0 and enable the three checks as `ERROR`.
2. Add `@NullMarked` to `:test` **module** `package-info.java`.
3. Fix all compiler warnings in `:common`, `:data`, `:api`, and `:server`.
4. Remove redundant constructor null checks and update corresponding unit tests.
5. Remove redundant Guice null assertions in test suites.
6. Verify full quality suite with `wsl ./gradlew check build`.

## Open Questions

None. All decision branches were grilled and resolved.
