## Context

Project Njall requires a dedicated administrative data schema (`njall_admin`) and HTTP API endpoints under `/api/admin/v1/` to manage multi-tenant studios, administrative users, roles, and user-role assignments. The database layer uses PostgreSQL 18+ managed by Flyway migrations and Hibernate ORM. The runtime uses Apache Pekko Typed actors, Pekko HTTP routes, and Google Guice dependency injection.

This design builds directly upon existing in-force architectural decisions:
- [ADR-0001](../../../../adr/0001-pekko-http-runtime-architecture.md): Pekko HTTP runtime architecture.
- [ADR-0003](../../../../adr/0003-data-module-and-flyway-migration-architecture.md): Data module and Flyway migration architecture.
- [ADR-0005](../../../../adr/0005-sealed-dao-and-hibernate-dual-session-architecture.md): Sealed DAO and Hibernate dual-session architecture.
- [ADR-0008](../../../../adr/0008-module-level-singleton-scoping-convention.md): Module-level singleton scoping convention.
- [ADR-0009](../../../../adr/0009-hibernate-session-factory-provider-injection-and-archunit-enforcement.md): Hibernate SessionFactory Provider injection and ArchUnit enforcement.

### C4 Container & Component Diagram (ASCII)

```
+─────────────────────────────────────────────────────────────────────────────────────────────+
|                                    SYSTEM RUNTIME CONTAINER                                 |
|                                                                                             |
|   +─────────────────────────────────────────────────────────────────────────────────────+   |
|   |                              HTTP REST Boundary (:api)                              |   |
|   |                                                                                     |   |
|   |   AdminRoute ── concat ──┬──> StudioAdminRoute    (/api/admin/v1/studios[/{id}])    |   |
|   |                          ├──> UserAdminRoute      (/api/admin/v1/users[/{id}])      |   |
|   |                          │    ├──> :addRole       (/api/admin/v1/users/{id}:addRole)|   |
|   |                          │    └──> :removeRole    (/api/admin/v1/users/{id}:remove) |   |
|   |                          └──> RoleAdminRoute      (/api/admin/v1/roles[/{id}])      |   |
|   +─────────────────────────────┬───────────────────┬───────────────────┬───────────────+   |
|                                 │ Ask               │ Ask               │ Ask               |
|                                 v                   v                   v                   |
|   +─────────────────────────────┴───────────────────┴───────────────────┴───────────────+   |
|   |                     Typed Actor Boundary on Blocking Dispatcher (:api)              |   |
|   |                                                                                     |   |
|   |   StudioAdminActor              UserAdminActor              RoleAdminActor          |   |
|   |   (StudioAdminCommand)          (UserAdminCommand)          (RoleAdminCommand)      |   |
|   +─────────────────────────────┬───────────────────┬───────────────────┬───────────────+   |
|                                 │                   │                   │                   |
|                                 v                   v                   v                   |
|   +─────────────────────────────┴───────────────────┴───────────────────┴───────────────+   |
|   |                           Data Access Objects & Hibernate (:data)                   |   |
|   |                                                                                     |   |
|   |   StudioDAO                     AdminUserDAO                AdminRoleDAO            |   |
|   |   (DefaultStudioDAO)            (DefaultAdminUserDAO)       (DefaultAdminRoleDAO)   |   |
|   |                                                                                     |   |
|   |   Session Provider: @NjallAdmin Provider<SessionFactory>                            |   |
|   +─────────────────────────────────────────────────┬───────────────────────────────────+   |
+─────────────────────────────────────────────────────┼───────────────────────────────────────+
                                                      │ JDBC Connection (njall_admin role)
                                                      v
+─────────────────────────────────────────────────────────────────────────────────────────────+
|                                  POSTGRESQL 18+ DATABASE                                    |
|                                                                                             |
|   Schema: njall_admin                                                                       |
|   - admin_users (UUIDv7, unique username, tstatus, created_at, updated_at)                  |
|   - admin_roles (UUIDv7, unique role_name)                                                  |
|   - admin_role_assignments (admin_user_id FK, role_id FK, composite PK)                    |
|   - studios_lookup (tenant_id UUIDv7, studio_id UUIDv4, unique alias, soft delete)         |
|   - Trigger: sync_admin_timestamp() updating updated_at                                     |
|   - RLS Policy: rls_studios_lookup for njall_users                                          |
+─────────────────────────────────────────────────────────────────────────────────────────────+
```

## Goals / Non-Goals

**Goals:**
- Apply Flyway migration `V2__admin_schema.sql` defining `njall_admin` tables, enum type, timestamps, triggers, and permissions.
- Implement immutable domain records (`AdminUser`, `AdminRole`, `StudioLookup`) extending `DatabaseObject`.
- Implement JPA entities (`AdminUserEntity`, `AdminRoleEntity`, `StudioLookupEntity`) and register them in the `@NjallAdmin` multibinder.
- Implement `AdminUserDAO`, `AdminRoleDAO`, and `StudioDAO` using `@NjallAdmin Provider<SessionFactory>`.
- Implement typed Pekko actors (`StudioAdminActor`, `UserAdminActor`, `RoleAdminActor`) on `larpconnect.blocking-dispatcher`.
- Implement modular sub-route handlers (`StudioAdminRoute`, `UserAdminRoute`, `RoleAdminRoute`) composed into `DefaultAdminRoute`.
- Support symmetric identifier resolution (UUID or alias/username/roleName).
- Standardize on AIP-136 custom method syntax (`:addRole`, `:removeRole`) with idempotent success and AIP-193 structured JSON errors.
- Verify end-to-end functionality via Cucumber scenarios in `:integration`.

**Non-Goals:**
- eTag concurrency control and `If-Match` optimistic locking (deferred to a future change).
- Studio tenant schema provisioning (this change manages only the studio lookup table, not dynamically provisioning isolated tenant PostgreSQL schemas).
- Authentication / OAuth / JWT token verification (deferred to security service integration).

## Decisions

### Decision 1: Harmonized Timestamp Convention (`_at`) and Single Trigger
- **Choice**: Use `created_at`, `updated_at`, and `deleted_at` (`TIMESTAMPTZ`) across all `njall_admin` tables, serviced by a single update trigger function `njall_admin.sync_admin_timestamp()`.
- **Rationale**: Standardizes instant/point-in-time naming across the admin schema and eliminates duplicate trigger functions.
- **Alternatives Considered**: Using `_on` (as in V1's `created_on`) was rejected because `_on` semantically suggests calendar dates (`DATE`), while `_at` denotes timestamps (`TIMESTAMPTZ`).

### Decision 2: Symmetric Identifier Resolution
- **Choice**: Accept either a UUID or human-readable alias/name across all single-resource routes:
  - `/api/admin/v1/studios/{id}` accepts `studio_id` (UUID) or `alias` (string).
  - `/api/admin/v1/users/{id}` accepts user UUID or `username` (string).
  - `/api/admin/v1/roles/{id}` accepts role UUID or `role_name` (string).
- **Rationale**: Eliminates unnecessary preliminary queries for administrative callers and CLI tooling while remaining fully type-safe.
- **Alternatives Considered**: Strict UUID-only paths were considered, but rejected because requiring callers to look up UUIDs before addressing resources by their unique natural key adds unnecessary latency and complexity.

### Decision 3: Modular Sub-Route Delegates Composed in `DefaultAdminRoute`
- **Choice**: Implement `StudioAdminRoute`, `UserAdminRoute`, and `RoleAdminRoute` as separate classes, injected and concatenated into `DefaultAdminRoute`.
- **Rationale**: Complies with `AGENTS.md` class (<= 500 SLOC) and method (<= 50 SLOC) limits, separates route parsing concerns, and enables isolated testing using `RouteTestKit`.
- **Alternatives Considered**: Writing all routes directly inside `DefaultAdminRoute` was rejected as it would produce an oversized class with cyclomatic complexity exceeding project limits.

### Decision 4: Least-Privilege Cross-Schema Grants for Row Level Security
- **Choice**: Grant `USAGE` on schema `njall_admin` and `SELECT` on `njall_admin.studios_lookup` to `njall_users`.
- **Rationale**: Required by PostgreSQL to evaluate the RLS policy for tenant user sessions while preventing unauthorized mutations (`INSERT`, `UPDATE`, `DELETE`) by non-admin roles.
- **Alternatives Considered**: Granting all privileges or disabling RLS was rejected as a violation of multi-tenant isolation principles.

### Decision 5: Dedicated Typed Pekko Actors on Blocking Dispatcher
- **Choice**: Assign `StudioAdminActor`, `UserAdminActor`, and `RoleAdminActor` each their own closed ADT message protocols and run them on `larpconnect.blocking-dispatcher`.
- **Rationale**: Synchronous Hibernate DAO calls must be isolated from the default actor system thread pool to prevent starvation, following `njall-pekko` standards.
- **Alternatives Considered**: Executing DAO queries directly inside route directives was rejected because it would block Pekko HTTP worker threads.

### Decision 6: Canonical Identifier Formats for Studio Aliases and Role Names
- **Choice**: Enforce regex `^[a-z][a-z0-9_]*$` (alphanumeric lowercase and underscore, strictly starting with a letter) for both studio aliases and role names at the API boundary (HTTP 400 Bad Request) and at the database level via PostgreSQL CHECK constraints (`CHECK (alias ~ '^[a-z][a-z0-9_]*$')` on `studios_lookup` and `CHECK (role_name ~ '^[a-z][a-z0-9_]*$')` on `admin_roles`).
- **Rationale**: Guarantees URL/subdomain safety, eliminates case sensitivity and collation hazards, and ensures uniform naming across routing, security, and storage layers.
- **Alternatives Considered**: Permitting mixed-case, hyphens, or uppercase enum-style role names was rejected to preserve strict lowercase alphanumeric and underscore conventions across all identifiers.

## Risks / Trade-offs

- **[PostgreSQL Enum Type Drift]** -> If enum `njall_admin.tstatus` evolves in future migrations, Hibernate string mapping or custom enum converters must be used rather than ordinal mappings to prevent deserialization errors.
  - *Mitigation*: Map `status` using `@Enumerated(EnumType.STRING)` or custom TypeConverter in `AdminUserEntity`.
- **[Hash Index on `studio_id`]** -> The proposed schema uses `USING hash (studio_id)` on `studios_lookup`.
  - *Mitigation*: PostgreSQL 18 fully logs hash indexes in WAL, but standard btree indexes are also supported. We will maintain the hash index as requested.
- **[Soft Deletes in Unique Constraints]** -> Soft-deleted studio aliases might block re-registration if not properly partitioned.
  - *Mitigation*: The unique index on `alias` explicitly uses partial indexing: `WHERE deleted_at IS NULL`, allowing aliases of soft-deleted studios to be reused if desired.

## Migration Plan

1. Flyway executes `V2__admin_schema.sql` automatically during application migration (`./gradlew :server:run --args="migrate"`).
2. Existing schema tables (`njall.servers`, `njall.server_contacts`) remain untouched and operational.
3. Rollback: If rollback is required in development/testing environments, drop schema `njall_admin CASCADE` and revert Flyway migration history.

## Open Questions

None. All decision tree branches (timestamps, identifier symmetry, role mutator semantics, error payloads, and route modularity) were resolved during exploration.
