# 0019: Static Nullity Enforcement and Constructor Streamlining

- Status: accepted, supersedes ADR-0006
- Date: 2026-09-25
- Supersedes: 0006-package-level-nonnull-defaults-and-nullability-conventions.md

## Context

Under [ADR 0006](0006-package-level-nonnull-defaults-and-nullability-conventions.md), Project Njall established package-level `@NullMarked` defaults using JSpecify 1.0.1 and eliminated redundant runtime `requireNonNull` checks inside Guice `@Inject` constructors and `@Provides` methods.

However, compiler static analysis was not actively configured to enforce these nullity invariants. As a result, defensive runtime null checks (`Objects.requireNonNull` and `!= null`) persisted widely in domain object constructors and records, creating dead execution branches and redundant code. Furthermore, test suites retained defensive assertions checking `.isNotNull()` on Guice injector outputs, and several unit tests deliberately passed `null` to non-nullable constructor parameters to assert `NullPointerException`.

A comprehensive policy is required to automate compile-time nullity enforcement through ErrorProne and streamline all constructors, mappers, and test suites to fully respect `@NullMarked` contracts.

## Decision

1. **ErrorProne Static Compiler Enforcement**:
   - Upgrade ErrorProne in `build-logic` to version 2.50.0 to enable modern JSpecify checkers.
   - Enforce `AddNullMarkedToPackageInfo`, `ParameterMissingNullable`, and `RedundantNullCheck` as fatal compilation errors across all Java compilation tasks.
   - Mandate that all production and shared test utility packages declare `@NullMarked` in `package-info.java`.
2. **Constructor Null Check Elimination on Non-Nullable Fields**:
   - Eliminate defensive `Objects.requireNonNull` calls and `!= null` checks from constructors and record compact constructors across all domain models, options records, and modules where fields are not explicitly annotated with `@Nullable`.
   - Constructor parameters in `@NullMarked` packages are contractually non-null; runtime validation is strictly reserved for unvalidated external boundaries (such as Jackson deserializers or untrusted HTTP header parsers).
3. **Retirement of Redundant NPE Constructor Tests**:
   - Remove unit test methods that intentionally pass `null` to contractually non-null constructor parameters to assert `NullPointerException`. Compile-time static analysis guarantees non-null arguments; testing impossible null values against internal domain constructors is deprecated.
4. **JSpecify Type-Use Syntax for Nullable Arrays**:
   - Array parameters that legitimately permit `null` array references (such as CLI `args`) MUST be annotated using JSpecify type-use syntax as `Type @Nullable [] args` to annotate the array container rather than the component elements.
5. **Elimination of Redundant Guice Result Assertions**:
   - Guice guarantees non-null returns from `Guice.createInjector` and `injector.getInstance`. Test suites and application code MUST NOT assert `.isNotNull()` on Guice resolution outputs, asserting domain invariants or specific types directly instead.

## Consequences

### Positive
- Strict compile-time guarantee that null contracts are respected across all modules, eliminating runtime null check overhead and dead branches.
- Total consistency between package contracts, constructor logic, and unit tests.
- Improved JaCoCo branch coverage by removing unreachable defensive branches.
- Eliminates misleading unit tests that simulate impossible runtime null states.

### Negative
- Requires updating existing unit tests that asserted `NullPointerException` on domain constructors.
- Requires strict adherence to JSpecify type-use syntax (such as `String @Nullable []` for arrays) to avoid compiler warnings.
