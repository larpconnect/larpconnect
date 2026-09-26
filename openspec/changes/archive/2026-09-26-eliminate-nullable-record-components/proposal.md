## Why

Multiple Java records across **Njall** define record components annotated with `@Nullable`, violating the principle that records are strict, non-null data carriers. Leaking nullable fields into record definitions forces downstream consumers to perform defensive null checks, compromises static compiler safety under `@NullMarked`, and introduces null-handling inconsistencies across the **API plane**, **Data plane**, and **Application plane**.

## What Changes

- Prohibit `@Nullable` annotations on record components across all **modules**.
- Replace nullable record components with one of three non-null strategies:
  1. `java.util.Optional<T>` for general object references (e.g., `Optional<UUID>`, `Optional<String>`, `Optional<Instant>`).
  2. Empty immutable collections (`ImmutableList<T>`, `ImmutableSet<T>`, `ImmutableMap<K, V>`) initialized to empty collections rather than null references.
  3. An in-memory sentinel constant (`UNKNOWN`) for enumeration types, specifically `AdminUserStatus.UNKNOWN` for unspecified user statuses.
- Permit `@Nullable` parameters exclusively on record constructors (compact, canonical, or overloaded convenience constructors) to serve as a boundary normalization layer where null inputs are transformed into their non-null representations.
- Preserve the external PostgreSQL database contract (`njall_admin.tstatus` and `njall_admin.admin_users.status` remain `NOT NULL DEFAULT 'ACTIVE'`) and OpenAPI specification contract (non-null with `default: "ACTIVE"`).
- In the **Admin verticle**, ensure the **actor** layer resolves sentinel values (e.g., mapping `AdminUserStatus.UNKNOWN` to `ACTIVE`) before invoking the **DAO**, and ensure the **DAO** rejects sentinel values to guarantee database integrity.
- Add an ArchUnit rule in the `:integration` **library module** to enforce that zero record components in `com.larpconnect.njall..` are annotated with `@Nullable`.

## Capabilities

### New Capabilities

*(None)*

### Modified Capabilities

- `architectural-invariants`: Enforce via ArchUnit that record classes across the codebase do not declare record components annotated with `@Nullable`.
- `nullability-contracts`: Specify that record components in `@NullMarked` packages must be non-null, mandating `Optional<T>`, empty immutable collections, or sentinel enum values, while permitting `@Nullable` parameters in record constructors for boundary normalization.

## Impact

- **API plane**:
  - `UserAdminCommand.CreateUser`: `status` becomes `AdminUserStatus` (defaults to `UNKNOWN`), `initialRoles` becomes `ImmutableList<String>` (defaults to empty).
  - `UserAdminCommand.AddRole` & `RemoveRole`: `roleId` becomes `Optional<UUID>`, `roleName` becomes `Optional<String>`.
  - `CreateUserRequest`: `status` defaults to `UNKNOWN`, `roles` defaults to `ImmutableList.of()`.
  - `RoleAssignmentRequest`: `roleId` and `roleName` become `Optional<UUID>` and `Optional<String>`.
  - `UserAdminActor`: Processes `Optional` values and converts `UNKNOWN` status to `ACTIVE`.
- **Data plane**:
  - `AdminUserStatus`: Adds `UNKNOWN` constant as an in-memory/API sentinel.
  - `StudioLookup`: `deletedAt` becomes `Optional<Instant>`.
  - `MigrationConfig`: `password` becomes `Optional<String>`.
  - `SessionConfig`: `password` becomes `Optional<String>`.
  - `DefaultAdminUserDAO`: Rejects `AdminUserStatus.UNKNOWN` to protect database constraints.
- **Integration**:
  - `ArchitectureTest`: New ArchUnit invariant test verifying record component nullability rules.
