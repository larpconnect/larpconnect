# 0020: Codebase-Wide Null Check and NPE Test Elimination

- Status: accepted, supersedes ADR-0019
- Date: 2026-09-25
- Supersedes: 0019-static-nullity-enforcement-and-constructor-streamlining.md

## Context

Under [ADR 0019](0019-static-nullity-enforcement-and-constructor-streamlining.md), Project Njall introduced ErrorProne static compile-time checks (`AddNullMarkedToPackageInfo`, `ParameterMissingNullable`, and `RedundantNullCheck`) and eliminated redundant runtime null checks inside domain constructors and records.

However, defensive runtime checks (`Objects.requireNonNull` and `param == null` branching) and unit tests asserting `NullPointerException` remained prevalent across configuration classes, service and resource factories, data access objects (DAOs), and API directives for parameters and fields not annotated with `@Nullable`.

In a codebase where `@NullMarked` is declared across all production packages, unannotated parameters and return types are contractually guaranteed to be non-null by the compiler. Retaining defensive checks creates unreachable branches, increases cyclomatic complexity, and perpetuates unit tests that simulate impossible runtime null states.

A comprehensive policy is required to eliminate unannotated runtime null checks and obsolete NPE unit tests across all application layers, reserving runtime null validation strictly for types explicitly annotated with `@Nullable`.

## Decision

1. **Universal Elimination of Defensive Null Checks on Unannotated Types**:
   - Eliminate `Objects.requireNonNull` calls and conditional null checks (`== null` / `!= null`) across all constructors, factory methods, DAO operations, configuration mappers, and directives unless the target parameter or field is explicitly annotated with `@Nullable`.
   - Internal methods and constructors rely entirely on package-level `@NullMarked` contracts and ErrorProne compile-time enforcement.
2. **Retirement of Obsolete NPE Unit Tests**:
   - Remove all unit test methods asserting `NullPointerException` when passing `null` to unannotated parameters across all modules.
   - Retain runtime null validation and NPE test assertions exclusively where parameters are explicitly marked `@Nullable` and have defined null-handling semantics.
3. **Preservation of Untrusted Boundary Defenses**:
   - External boundaries that receive untrusted, unvalidated input (e.g. Jackson deserialization, raw HTTP headers) continue to validate and parse payloads before passing non-null values into internal application services.

## Consequences

### Positive
- Eliminates dead code and unreachable execution branches across configuration, data, and API layers.
- Aligns unit tests with actual domain and package contracts; removes misleading tests that assert impossible null arguments.
- Maintains 100% compliance with Spotless, Checkstyle, SpotBugs, ErrorProne (`-Werror`), and JaCoCo coverage gates (>=85% line, >=90% branch).

### Negative
- Developers cannot write tests asserting NPE on standard methods without explicitly marking parameters `@Nullable`.
