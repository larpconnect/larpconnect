# 0006: Package-Level Non-Null Defaults and Nullability Conventions

## Status

Accepted

## Date

2026-09-16

## Context

Project Njall modules (`:common`, `:data`, `:api`, `:server`) previously lacked package-level nullness contracts. Consequently, compiler flags and static analyzers (SpotBugs, ErrorProne, Checkstyle) could not verify nullness invariants across package or module boundaries. This led to pervasive defensive programming using runtime `java.util.Objects.requireNonNull` calls, even on parameters provided by Google Guice injection where Guice already guarantees non-null instances.

A clear, universal convention is needed for package-level defaults, explicit nullable declarations, and dependency-injection null-checking semantics.

## Decision

1. **Universal Package-Level Non-Null Defaults**: Every non-test Java package across all production modules (`:common`, `:data`, `:api`, `:server`) must declare `@NullMarked` (`org.jspecify.annotations.NullMarked`) in its `package-info.java`.
2. **Standardized `@Nullable` Annotation**: Wherever a parameter, record component, or return value may legitimately be `null`, it must be explicitly annotated with `org.jspecify.annotations.Nullable` (`org.jspecify.annotations.Nullable`).
3. **Unified JSpecify Ecosystem**: JSpecify 1.0.1 serves as the single nullness specification across the entire codebase. This eliminates dependency on legacy JSR-305 (`@ParametersAreNonnullByDefault`) and SpotBugs (`@ReturnValuesAreNonnullByDefault`) annotations, avoiding clashing nullness semantics and keeping SpotBugs analysis clean without suppressions.
4. **Elimination of Redundant Injection-Time Null Checks (Option A)**: In Guice `@Inject` constructors and `@Provides` methods, redundant runtime `requireNonNull` checks are eliminated. Guice guarantees non-null injection by default, and package annotations enforce static compile-time safety. Runtime checks remain reserved for untrusted external boundaries and records constructed directly.
5. **Build Logic Enforcement**: Centralize `jspecify` in `gradle/libs.versions.toml` and configure `compileOnly` dependencies on JSpecify annotations in `njall.java-common-conventions.gradle.kts`.

## Consequences

- **Positive**: Complete compile-time and static analysis clarity across all package boundaries; unified nullness contracts under JSpecify; no need for SpotBugs null analysis filter exclusions; elimination of boilerplate null-checking branches on Guice-managed objects; cleaner unit tests that do not need artificial null-assertion test cases for constructors that never receive null in production.
- **Negative**: Removing `requireNonNull` from existing constructors requires updating unit tests that asserted `NullPointerException` on those constructors.
