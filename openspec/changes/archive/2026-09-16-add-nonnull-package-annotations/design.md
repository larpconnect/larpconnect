## Context

Project Njall consists of four primary production modules: `:common`, `:data`, `:api`, and `:server`. Currently, non-test packages lack package-level nullness annotations, resulting in static analysis tools (SpotBugs, ErrorProne, Checkstyle) and IDEs being unable to reason definitively about parameter and return value nullness. Consequently, code was defensively littered with runtime `java.util.Objects.requireNonNull` assertions, even on Guice constructor parameters and `@Provides` methods where Guice guarantees non-null injection.

This design introduces universal `@NullMarked` across all 17 non-test packages, standardizes on `org.jspecify.annotations.Nullable` for values that may legitimately be null, unifies the codebase under JSpecify 1.0.1, and removes redundant runtime null validations on Guice-managed boundaries (Option A).

### C4 Component Diagram: Nullability Architecture & Static Analysis

```
+-----------------------------------------------------------------------------+
|                               Project Njall                                 |
+-----------------------------------------------------------------------------+
|                                                                             |
|  [Server Container :server]                                                 |
|  +-----------------------------------------------------------------------+  |
|  | CliRunner / ServerApp / HttpServerService                             |  |
|  | package-info: @NullMarked                                              |  |
|  +-----------------------------------+-----------------------------------+  |
|                                      |                                      |
|                                      v                                      |
|  [API Component :api]                [Data Component :data]                 |
|  +--------------------------------+  +-----------------------------------+  |
|  | Routes / Actor Probes / Jackson|  | DAOs / Entities / Flyway Migration|  |
|  | package-info: @NullMarked      |  | package-info: @NullMarked         |  |
|  +----------------+---------------+  +-----------------+-----------------+  |
|                   |                                    |                    |
|                   +-----------------+------------------+                    |
|                                     v                                       |
|  [Common Infrastructure :common]                                            |
|  +-----------------------------------------------------------------------+  |
|  | Config / Health / Shared Annotations                                  |  |
|  | package-info: @NullMarked                                              |  |
|  +-----------------------------------+-----------------------------------+  |
|                                      |                                      |
|                                      v                                      |
|  [Static Analysis & Tooling Layer]                                          |
|  +-----------------------------------------------------------------------+  |
|  | JSpecify (@NullMarked, @Nullable)                                     |  |
|  | SpotBugs / ErrorProne / Checkstyle / JaCoCo                            |  |
|  +-----------------------------------------------------------------------+  |
|                                                                             |
+-----------------------------------------------------------------------------+
```

## Goals / Non-Goals

**Goals:**
- Annotate all 17 non-test, non-integration `package-info.java` files with `@NullMarked` (`org.jspecify.annotations.NullMarked`).
- Centralize `jspecify` in `gradle/libs.versions.toml` and configure `compileOnly` dependencies in `njall.java-common-conventions.gradle.kts`.
- Mark optional/nullable parameters and returns explicitly with `org.jspecify.annotations.Nullable`.
- Eliminate redundant `requireNonNull` checks from Guice `@Inject` constructors and `@Provides` methods.
- Update unit test suites to reflect the removal of redundant constructor null validation tests, maintaining 100% build verification and JaCoCo coverage (85% line, 90% branch).
- Keep SpotBugs configuration clean without requiring broad null-analysis suppression filters.

**Non-Goals:**
- Annotating test packages (`:test` and test source directories) with non-null defaults.
- Removing runtime argument validation from public entry points / records that accept untrusted external input (e.g., config parsing or domain records constructed outside Guice).

## Decisions

### Decision 1: JSpecify `@NullMarked` Package Annotations
- **Choice**: Use `org.jspecify.annotations.NullMarked` (from JSpecify 1.0.1).
- **Rationale**: Unified nullness contracts across all packages using JSpecify eliminates clashing semantics between legacy JSR-305 and SpotBugs annotations. SpotBugs 4.9.7 previously produced false positive warnings when inspecting JSR-305 defaults alongside JSpecify `@Nullable`. Replacing them with `@NullMarked` resolves the mismatch cleanly and allows SpotBugs null check exclusions to be removed.
- **Alternatives Considered**: JSR-305 `@ParametersAreNonnullByDefault` + SpotBugs `@ReturnValuesAreNonnullByDefault`. Replaced per user instruction due to SpotBugs null analysis impedance mismatch.

### Decision 2: Use JSpecify for `@Nullable`
- **Choice**: Use `org.jspecify.annotations.Nullable`.
- **Rationale**: JSpecify is the modern standard supported directly by Google and ErrorProne, and aligns perfectly with `@NullMarked`.

### Decision 3: Option A (Aggressive Removal of Redundant Injected Null Checks)
- **Choice**: Remove `requireNonNull` from Guice `@Inject` constructors and `@Provides` methods.
- **Rationale**: Guice enforces at injector creation time that no null instances are injected unless explicitly annotated `@Nullable`. Having `requireNonNull` in every `@Inject` constructor duplicates what Guice and the package nullness contracts already guarantee.
- **Alternatives Considered**: Keeping constructor checks defensively (Option C). Rejected per user directive and because it clutters production code and forces test suites to author artificial `assertThatNullPointerException` test cases for constructors that will never receive null in production.

## Risks / Trade-offs

- **[Risk] Test Coverage Drop**: Removing `requireNonNull` removes branch checks, which might alter line or branch counts.
  - *Mitigation*: Run `tasks.jacocoTestCoverageVerification` across all modules to verify coverage remains comfortably above 85% line and 90% branch.
- **[Risk] SpotBugs False Positives**: Incompatible nullness annotations could trigger false positives.
  - *Mitigation*: Unified under JSpecify (`@NullMarked` and `@Nullable`), verified with clean `./gradlew spotbugsMain` execution without suppressions.

## Migration Plan

1. Update `gradle/libs.versions.toml`, `parent/build.gradle.kts`, and `njall.java-common-conventions.gradle.kts` for `jspecify`.
2. Update all 17 `package-info.java` files to `@NullMarked`.
3. Annotate `@Nullable` on identified sites.
4. Clean up `requireNonNull` calls in Guice classes.
5. Update unit tests in `:api`, `:common`, and `:server`.
6. Run `./gradlew check build` to verify formatting, compilation, tests, static analysis, and JaCoCo coverage.

## Open Questions

None. All architectural decisions (Option A, JSpecify NullMarked and Nullable unification) have been resolved.
