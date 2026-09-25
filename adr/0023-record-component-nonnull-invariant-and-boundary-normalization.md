# 0023: Record Component Non-Null Invariant and Boundary Normalization

## Status

Accepted

## Date

2026-09-26

## Context

Project Njall establishes that records are strict data carriers (ADR 0021) and mandates deep immutability across all record types (ADR 0022). Furthermore, ADR 0006 and the `nullability-contracts` specification enforce package-level `@NullMarked` defaults across all non-test packages to eliminate redundant defensive null checks and maximize static compile-time safety.

However, several records across `:api` and `:data` currently declare record components annotated with `@Nullable`. In Java records, component annotations on the record header automatically propagate to generated private final fields and public accessor methods. When record components are declared `@Nullable`, nullness leaks directly into downstream callers, forcing actors, services, and route handlers across the Application and Admin layers to introduce branching null checks or risk `NullPointerException`s.

At the same time, callers and deserializers (such as Jackson) often receive payloads or external inputs where optional fields are legitimately omitted or null. A clear architectural boundary is required: record constructors serve as the boundary normalization layer, converting nullable inputs into clean non-null representations so that the record's internal state and accessor methods are unconditionally non-null.

## Decision

1. **Record Component Non-Null Invariant**: All `record` classes within `com.larpconnect.njall..` (excluding test classes) must not declare record components annotated with `org.jspecify.annotations.Nullable`.
2. **Three Non-Null Substitution Strategies**:
   - **General Object References**: Use `java.util.Optional<T>` for single-value optional properties (e.g., `Optional<UUID> roleId`, `Optional<String> roleName`, `Optional<Instant> deletedAt`, `Optional<String> password`).
   - **Collections**: Use Guava immutable collections (`ImmutableList<T>`, `ImmutableSet<T>`, `ImmutableMap<K, V>`) initialized to empty instances (e.g., `ImmutableList.of()`) rather than null references or nested `Optional<List<T>>` wrappers.
   - **Enumerations**: Use in-memory sentinel constants (such as `AdminUserStatus.UNKNOWN`) to denote unspecified enumeration values at the API and command boundary.
3. **Constructor-Boundary Normalization**: Record constructors (compact constructors and overloaded convenience constructors) are explicitly permitted to accept `@Nullable` parameters. The constructor is responsible for normalizing null inputs (e.g., via `Optional.ofNullable`, defaulting null collections to `ImmutableList.of()`, or defaulting null enums to `UNKNOWN`).
4. **Preservation of Database and OpenAPI Invariants**:
   - The PostgreSQL schema remains strictly non-null (`njall_admin.tstatus` allows `('ACTIVE', 'DISABLED', 'DELETED')` and `admin_users.status` is `NOT NULL DEFAULT 'ACTIVE'`).
   - The OpenAPI specification remains strictly non-null with `default: "ACTIVE"`.
   - `AdminUserStatus.UNKNOWN` exists solely as an in-memory/API sentinel. The application layer (`UserAdminActor`) resolves `UNKNOWN` to `ACTIVE` before invoking persistence.
   - `DefaultAdminUserDAO` validates that `status != AdminUserStatus.UNKNOWN`, rejecting any attempt to persist sentinel enum values.
5. **ArchUnit Enforcement**: Implement an ArchUnit test in `:integration` (`ArchitectureTest`) verifying that zero non-test record classes in `com.larpconnect.njall..` declare record components annotated with `@Nullable`.

## Consequences

- **Positive**: Guarantees that record accessors never return null, eliminating defensive null checks across downstream actors, services, and routes; pairs cleanly with `@NullMarked` and ADR 0022; isolates null normalization to constructor boundaries; prevents invalid enum values from reaching PostgreSQL.
- **Negative**: Requires updating callsites that inspect record components to use `Optional` methods or empty collection checks rather than `!= null`.
- **Follow-up**: Maintain the ArchUnit rule in continuous integration to catch any future record declarations with `@Nullable` components.
