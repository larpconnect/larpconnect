## Context

In **Njall**, packages declare `@NullMarked` to enforce non-null defaults across method parameters, returns, and fields. However, multiple records across the **API plane** and **Data plane** currently declare record components annotated with `@Nullable`. Because record components automatically define private final fields and public accessor methods, `@Nullable` leaks into downstream consumers, forcing callers across the **Application plane** and **Admin verticle** to introduce redundant defensive null checks or risk `NullPointerException`s.

This design establishes a strict architectural boundary: record components are strictly non-null data carriers. Any nullness is isolated to constructor parameters (overloaded convenience or compact constructors) where values are normalized into `java.util.Optional<T>`, empty immutable collections, or sentinel enumeration values before assignment.

```
+---------------------------------------------------------------------------------------------------+
|                                C4 COMPONENT DIAGRAM: RECORD FLOW                                  |
+---------------------------------------------------------------------------------------------------+
|                                                                                                   |
|  [External Client]                                                                                |
|         │                                                                                         |
|         │ HTTP JSON (status omitted, roles omitted, roleId=null)                                  |
|         ▼                                                                                         |
|  +─────────────────────────────────────────────────────────────────────────────────────────────+  |
|  | API Plane (Container: Server)                                                               |  |
|  |                                                                                             |  |
|  |  +─────────────────────────+          +──────────────────────────────────────────────────+  |  |
|  |  | Route & Request DTO     |          | Pekko Actor Command Record                       |  |  |
|  |  | (CreateUserRequest)     | ───────> | (UserAdminCommand.CreateUser)                    |  |  |
|  |  | * Compact constructor   |          | * Normalizes status -> AdminUserStatus.UNKNOWN   |  |  |
|  |  |   normalizes inputs     |          | * Normalizes initialRoles -> ImmutableList.of()  |  |  |
|  |  +─────────────────────────+          +──────────────────────────────────────────────────+  |  |
|  |                                                                 │                           |  |
|  +─────────────────────────────────────────────────────────────────┼───────────────────────────+  |
|                                                                    │                              |
|                                                                    ▼                              |
|  +─────────────────────────────────────────────────────────────────────────────────────────────+  |
|  | Application Plane (Pekko Actor: UserAdminActor)                                             |  |
|  |  * Evaluates cmd.status()                                                                   |  |
|  |  * Maps AdminUserStatus.UNKNOWN -> AdminUserStatus.ACTIVE                                   |  |
|  |  * Consumes non-null ImmutableList<String> directly                                         |  |
|  +─────────────────────────────────────────────────────────────────┬───────────────────────────+  |
|                                                                    │                              |
|                                                                    │ create(username, ACTIVE, ...) |
|                                                                    ▼                              |
|  +─────────────────────────────────────────────────────────────────────────────────────────────+  |
|  | Data Plane (DefaultAdminUserDAO)                                                            |  |
|  |  * Rejects AdminUserStatus.UNKNOWN (guards database constraint)                            |  |
|  |  * Persists to PostgreSQL via Hibernate Session                                             |  |
|  +─────────────────────────────────────────────────────────────────┬───────────────────────────+  |
|                                                                    │                              |
|                                                                    ▼                              |
|  +─────────────────────────────────────────────────────────────────────────────────────────────+  |
|  | Database: PostgreSQL (njall_admin.admin_users)                                              |  |
|  |  * status njall_admin.tstatus NOT NULL DEFAULT 'ACTIVE'                                     |  |
|  +─────────────────────────────────────────────────────────────────────────────────────────────+  |
|                                                                                                   |
+---------------------------------------------------------------------------------------------------+
```

## Goals / Non-Goals

**Goals:**
- Eliminate `@Nullable` annotations from all record component definitions across all **modules**.
- Standardize on three non-null substitution patterns: `Optional<T>`, empty immutable collections, and enum sentinel values (`UNKNOWN`).
- Confine `@Nullable` parameter annotations strictly to constructor signatures for boundary normalization.
- Ensure Jackson deserialization cleanly populates non-null record representations from payloads with omitted or null fields.
- Protect database invariants by ensuring sentinel enum values are converted before persistence and rejected by **DAO**s.
- Enforce the invariant across the codebase using ArchUnit in the `:integration` **library module**.

**Non-Goals:**
- Modifying the PostgreSQL schema: `njall_admin.tstatus` and `njall_admin.admin_users.status` remain `NOT NULL DEFAULT 'ACTIVE'` without adding `'UNKNOWN'`.
- Modifying the OpenAPI contract: OpenAPI schemas remain strictly non-null with `default: "ACTIVE"`. `UNKNOWN` is an in-memory/API sentinel only.
- Eliminating `@Nullable` from non-record classes (e.g., Hibernate entities, Picocli command classes, or helper method arguments).

## Decisions

### Decision 1: Record Header Component Non-Null Invariant
- **Choice**: Record component declarations MUST NOT be annotated with `@Nullable`.
- **Rationale**: Records are pure data carriers. Accessors should guarantee non-null return values so callers never have to branch on nullity.
- **Alternatives Considered**:
  - *Keep `@Nullable` on record components*: Preserves current state but spreads defensive null checks across actor logic and violates the `@NullMarked` intent.

### Decision 2: General Types Use `Optional<T>`
- **Choice**: For optional single-value references (`UUID`, `Instant`, `String`), record components use `Optional<T>`.
- **Rationale**: `Optional` explicitly denotes potential absence without null references. Overloaded constructors accept `@Nullable T` and wrap via `Optional.ofNullable(val)`. Jackson's `Jdk8Module` binds null or omitted JSON properties directly to `Optional.empty()`.
- **Alternatives Considered**:
  - *Magic null-object sentinels (e.g. empty UUID `00000000-...`)*: Confuses valid identity values with absence and creates subtle bugs.

### Decision 3: Collection Components Use Empty Immutable Collections
- **Choice**: Collections in records use Guava immutable collections (`ImmutableList<T>`, `ImmutableSet<T>`, `ImmutableMap<K, V>`) defaulting to empty instances (`ImmutableList.of()`).
- **Rationale**: Wrapping collections in `Optional<ImmutableList<T>>` introduces nested boilerplate (`opt.map(List::stream)...`). Empty collections model zero-or-more cardinality cleanly.
- **Alternatives Considered**:
  - *`Optional<ImmutableList<T>>`*: Retained in CLI options (`MigrationOptions`) solely where distinguishing "flag omitted" vs "empty list provided" is required for config overrides. For all domain and command records, empty collection is the standard.

### Decision 4: Enums Use In-Memory Sentinel (`UNKNOWN`)
- **Choice**: Add `UNKNOWN` to `AdminUserStatus` in `:data` as an in-memory/API sentinel. When `status` is omitted or null in a request/command constructor, it defaults to `UNKNOWN`.
- **Rationale**: Avoids `Optional<AdminUserStatus>`. In `UserAdminActor` in `:api`, `cmd.status() == AdminUserStatus.UNKNOWN` resolves to `AdminUserStatus.ACTIVE`.
- **Alternatives Considered**:
  - *Default directly to `ACTIVE` in record constructor*: While valid for `CreateUser`, `UNKNOWN` allows the domain layer to differentiate between "caller explicitly requested ACTIVE" vs "caller omitted status", preserving flexibility if future validation rules apply.
  - *Add `'UNKNOWN'` to PostgreSQL enum*: Rejected because the database requires valid operational states; database columns are `NOT NULL DEFAULT 'ACTIVE'`.

### Decision 5: ArchUnit Invariant Rule
- **Choice**: Add an ArchUnit test rule in `:integration` checking `JavaClass.isRecord()` to verify that zero record components are annotated with `org.jspecify.annotations.Nullable`.
- **Rationale**: Prevents future regressions automatically during `./gradlew check`.

## Risks / Trade-offs

- **[Risk] Jackson deserialization mismatch for records**: If a record component is `ImmutableList<String>` or `AdminUserStatus` and the incoming JSON omits the property, Jackson might pass `null` to the canonical constructor.
  - *Mitigation*: Implement compact constructors on request records (`CreateUserRequest`) that normalize `null` to `ImmutableList.of()` and `AdminUserStatus.UNKNOWN`.
- **[Risk] Accidental persistence of sentinel values**: If `AdminUserStatus.UNKNOWN` is passed to Hibernate, PostgreSQL will throw an enum violation error.
  - *Mitigation*: Add defensive validation in `DefaultAdminUserDAO` in `:data` asserting `status != AdminUserStatus.UNKNOWN` with a clear `IllegalArgumentException`.

## Migration Plan

1. Update domain enums and records in `:data` (`AdminUserStatus`, `StudioLookup`, `MigrationConfig`, `SessionConfig`).
2. Update command, request, and actor classes in `:api` (`UserAdminCommand`, `CreateUserRequest`, `RoleAssignmentRequest`, `UserAdminActor`).
3. Update unit and integration test assertions across `:api`, `:data`, and `:server`.
4. Add ArchUnit invariant test in `:integration`.
5. Verify build and checks pass via `./gradlew check build`.

## Open Questions

*(No outstanding questions; prior exploration resolved scope, database invariance, and sentinel boundaries.)*
