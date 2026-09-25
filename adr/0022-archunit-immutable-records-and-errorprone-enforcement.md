# 0022: ArchUnit Immutable Records and ErrorProne Enforcement

## Status

Accepted

## Date

2026-09-25

## Context

Project Njall establishes in AGENTS.md that records are strict data carriers (re-affirmed in ADR 0021) and that *"Immutable objects and immutability contracts should be used whenever possible. This may involve using immutable collections from Guava and/or defensive copies, but it needs to be maintained throughout the system."*

While ADR 0021 prohibited static factory methods on record classes, records remained unconstrained by static immutability analysis. Without automated enforcement, records could hold mutable collection types (e.g., `java.util.List` or `java.util.Map`), unconstrained generic type parameters (`T`), or non-immutable third-party classes. Google ErrorProne provides an `@Immutable` annotation (`com.google.errorprone.annotations.Immutable`) and a corresponding compiler bug pattern, but ErrorProne only validates classes that are explicitly annotated. If a record lacks the annotation, ErrorProne's compiler check never runs on it.

An automated ArchUnit rule is required in `:integration` to mandate that every non-test record in `com.larpconnect.njall..` carries `@Immutable`, creating an airtight verification pair where ArchUnit guarantees the annotation's presence and ErrorProne guarantees deep immutability at compile time.

## Decision

1. **ArchUnit Record Immutability Invariant**: Mandate via ArchUnit in `:integration` (`ArchitectureTest`) that all `record` classes within `com.larpconnect.njall..` (excluding test classes) must be annotated with `@com.google.errorprone.annotations.Immutable`.
2. **Universal ErrorProne Annotations Availability**: Add `compileOnly(libs.errorprone.annotations)` to `njall.java-common-conventions.gradle.kts` in `build-logic`, making `@Immutable` and `@ImmutableTypeParameter` universally available across all project modules.
3. **Compiler Immutability Enforcement**: Rely on ErrorProne's `Immutable` bug pattern, which is enabled at `ERROR` severity by default in ErrorProne 2.50.0, to treat immutability violations as fatal build errors across all modules.
4. **Annotate All Production Records**: Annotate every record class across `:common`, `:data`, `:api`, and `:server` with `@Immutable`.
5. **Resolve Deep Immutability Violations**:
   - In `MigrationConfig` (`:data`), replace mutable interfaces `List<String>` and `Map<String, String>` with Guava `ImmutableList<String>` and `ImmutableMap<String, String>`, providing overloaded constructors for backward compatibility.
   - In `ApiCall<T>` (`:common`), constrain type parameter `T` with `@ImmutableTypeParameter` to guarantee encapsulated command payloads are deeply immutable.
   - In Pekko Typed command records (`:api`), configure ErrorProne's `Immutable:KnownImmutable` check option in build conventions with `org.apache.pekko.actor.typed.ActorRef` to recognize thread-safe Pekko actor references as immutable across the system without requiring scattered `@SuppressWarnings("Immutable")` annotations.

## Consequences

- **Positive**: Guarantees compile-time and architectural immutability across all record data carriers in the system; eliminates accidental mutable collection leakage; pairs ArchUnit and ErrorProne for zero-gap immutability enforcement; aligns code directly with AGENTS.md rule 7.3.
- **Negative**: Requires annotating all future production records with `@Immutable` and resolving any non-immutable component types.
- **Follow-up**: Ensure new records introduced in subsequent features adhere to this standard.
