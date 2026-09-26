# Design: Users Studio API, Admin Default Studio Roles, and Tenant Isolation

## Context

In **Njall**, the application server currently hosts an administrative API under `/api/admin/v1/` backed by the `@NjallAdmin` Hibernate session factory. In-force architectural decision [0005: Sealed DAO and Hibernate Dual Session Architecture](../../adr/0005-sealed-dao-and-hibernate-dual-session-architecture.md) established dual session factories for `@NjallAdmin` and `@NjallUsers`, with a planned follow-up to introduce tenanted DAOs using `@NjallUsers` and Row-Level Security (RLS).

Currently, [`StudioDAO`](../../data/src/main/java/com/larpconnect/njall/data/dao/StudioDAO.java) queries the administrative directory table `njall_admin.studios_lookup` to map public studio IDs and aliases to internal database tenant IDs. However, "Studio" is fundamentally a domain and user-space concept. To support user-facing operations, the system requires a clean separation:
1. An admin directory lookup DAO (`StudioLookupDAO`) in the **Data plane** managing `njall_admin.studios_lookup`.
2. A user-space tenanted DAO (`StudioDAO`) in the **Data plane** managing `njall_users.studios` with tenant isolation.
3. User-space HTTP routing in the **API plane** under `/api/studios/{studio-id}/v1` that maps public studio identifiers to internal tenant IDs without ever leaking internal tenant UUIDs to clients.
4. Administrative endpoints under `/api/admin/v1/studio-roles` to manage system default studio roles conforming to Google AIP-134 for PATCH operations.

## Goals / Non-Goals

**Goals:**
- Provide single-tenant studio retrieval via `GET /api/studios/{studio-id}/v1/studio` in a dedicated `com.larpconnect.njall.api.studios` package.
- Resolve `{studio-id}` (UUID or alias) to internal tenant ID via `StudioLookupDAO`, returning 404 for unmapped or soft-deleted records.
- Enforce PostgreSQL Row-Level Security on `njall_users.studios` using transaction-scoped `set_config('app.tenant_id', :tenantId, true)`.
- Never expose internal database `tenant_id` in user-facing API responses.
- Provision Flyway migration `V3__users_schema_and_default_roles.sql` defining `default_studio_roles`, `studios`, reverse hostname functions, and RLS policies.
- Provide administrative endpoints `/api/admin/v1/studio-roles[/{id}]` (GET, POST, PATCH conforming to AIP-134).
- Update `CreateStudioRequest` on `POST /api/admin/v1/studios` to accept optional `name` and provision both user-space `studios` and admin `studios_lookup` entries.

**Non-Goals:**
- Implementing dynamic per-tenant actor trees (`TenantActor` routing pools) in this change; routing will directly invoke actor/DAO layers.
- User-space mutations or listing of multiple studios (each tenant is strictly isolated).
- Tenant-level role customizations (this change manages only global `default_studio_roles`).

## Architectural Topology (C4 Component Diagram)

```
+-----------------------------------------------------------------------------------------+
| Client / Downstream Consumer                                                            |
+-----------------------------------------------------------------------------------------+
       |                                                               |
       | (Admin Plane)                                                 | (User Plane)
       v                                                               v
  /api/admin/v1/studio-roles                                    /api/studios/{studio-id}/v1/studio
       |                                                               |
+------|---------------------------------------------------------------|------------------+
| API Plane (:api)                                                     |                  |
|      v                                                               v                  |
| +-----------------------------+                       +-------------------------------+ |
| | DefaultAdminRoute           |                       | StudiosRoute                  | |
| | - DefaultStudioRoleRoute    |                       | - non-blocking HTTP route     | |
| +-----------------------------+                       | - forward to StudioActor      | |
|      |                                                +-------------------------------+ |
|      v                                                               |                  |
| +-----------------------------+                                      v                  |
| | StudioRoleAdminActor        |                       +-------------------------------+ |
| +-----------------------------+                       | StudioActor (@Blocking Props) | |
|      |                                                | - resolve studio-id to lookup | |
|      |                                                | - query tenanted StudioDAO    | |
|      |                                                +-------------------------------+ |
+------|---------------------------------------------------------------|------------------+
       |                                                               |
+------|---------------------------------------------------------------|------------------+
| Data Plane (:data)                                                   |                  |
|      |                                                               |                  |
|      v                                                               v                  |
| +-----------------------------+                       +-------------------------------+ |
| | DefaultStudioRoleDAO        |                       | DefaultStudioDAO              | |
| | - uses @NjallAdmin          |                       | - uses @NjallUsers            | |
| +-----------------------------+                       | - SET LOCAL app.tenant_id     | |
|      |                                                +-------------------------------+ |
|      |                                                               ^                  |
|      |        +-----------------------------------------------+      |                  |
|      +------->| DefaultStudioLookupDAO                        |<-----+                  |
|               | - uses @NjallAdmin                            |                         |
|               +-----------------------------------------------+                         |
+-----------------------|----------------------------------------------|------------------+
                        |                                              |
+-----------------------|----------------------------------------------|------------------+
| PostgreSQL Database   v                                              v                  |
|               +-------------------------------+      +--------------------------------+ |
|               | njall_admin.studios_lookup    |=====>| njall_users.studios            | |
|               | (routing / alias directory)   | (FK) | (tenanted data, RLS enabled)   | |
|               +-------------------------------+      +--------------------------------+ |
|               +-------------------------------+                                         |
|               | njall_users.default_studio_roles                                        |
|               +-------------------------------+                                         |
+-----------------------------------------------------------------------------------------+
```

## Decisions

### 1. DAO Nomenclature and Separation of Concerns
- **Decision**: Rename existing `StudioDAO` and `DefaultStudioDAO` in `:data` to `StudioLookupDAO` and `DefaultStudioLookupDAO`. Introduce a new `StudioDAO` and `DefaultStudioDAO` qualified with `@NjallUsers`.
- **Rationale**: `StudioLookup` represents the global directory and routing entry in `njall_admin`. The actual `Studio` entity belongs in `njall_users` representing tenant state. This naming makes the boundary explicit.
- **Alternatives Considered**:
  - *Keep a single DAO*: Rejected because it conflates `@NjallAdmin` and `@NjallUsers` session factories and violates single-responsibility principles.

### 2. Transaction-Scoped RLS Configuration via `set_config`
- **Decision**: In `DefaultStudioDAO`, before executing queries on the `@NjallUsers` session, execute:
  ```java
  session.createNativeQuery("SELECT set_config('app.tenant_id', :tenantId, true)")
         .setParameter("tenantId", tenantId.toString())
         .getSingleResult();
  ```
- **Rationale**: Setting the 3rd parameter `is_local = true` ensures that the configuration is automatically cleared when the transaction ends, preventing tenant ID leakage into connection pools.
- **Alternatives Considered**:
  - *Hibernate multi-tenancy filter*: More complex to configure with modern Hibernate 6/7 and less portable than native PostgreSQL RLS.

### 3. Cross-Schema Foreign Key and Studio Provisioning
- **Decision**: In migration V3, enforce `ALTER TABLE njall_admin.studios_lookup ADD CONSTRAINT fk_studios_studios_lookup FOREIGN KEY (tenant_id) REFERENCES njall_users.studios (id);`. Update `StudioLookupDAO.create` to insert the tenant studio into `njall_users.studios` first, then record the lookup in `njall_admin.studios_lookup`.
- **Rationale**: Guarantees referential integrity between routing lookups and underlying tenant studio records.

### 4. Google AIP-134 Conforming PATCH for Studio Roles
- **Decision**: Implement `PATCH /api/admin/v1/studio-roles/{id}`:
  - Supports query parameter `?update_mask=name` (or treats omitted mask as full mutable update).
  - Validates non-blank role name.
  - Returns HTTP 200 with updated `DefaultStudioRole` resource.
  - Returns HTTP 404 if role ID does not exist, and HTTP 409 if name conflicts with an existing role.
- **Rationale**: Adheres to project-wide Google AIP standards and existing error conventions (AIP-193).

### 5. Dedicated User Space API Package and Non-Blocking Route Dispatching
- **Decision**: Create `com.larpconnect.njall.api.studios` package with `StudiosRoute`, implementing `RouteProvider`. Route directives run entirely non-blocking, delegating `{studio-id}` resolution and data querying asynchronously to `StudioActor` executing on the `@Blocking Props` dispatcher.
- **Rationale**: Isolates user-space studio routes from administrative management routes in `com.larpconnect.njall.api.admin` and protects Pekko HTTP connection threads from synchronous JDBC/Hibernate blocking I/O.

## Risks / Trade-offs

- **[Risk] RLS blocks administrative creation of studios** -> *Mitigation*: Migration V3 explicitly grants admin policies (`CREATE POLICY rls_studios_admin ON njall_users.studios FOR ALL TO njall_admin USING (true) WITH CHECK (true);` and `rls_default_studio_roles_admin`).
- **[Risk] Connection reuse across requests in connection pool** -> *Mitigation*: Using `is_local = true` in `set_config` bounds the `app.tenant_id` setting strictly to the current database transaction.
- **[Risk] Leaking tenant_id to downstream clients** -> *Mitigation*: The user-facing `StudioResponse` record only serializes `studioId`, `alias`, and `name`; `tenant_id` is never mapped to the response model.

## Migration Plan

1. Apply Flyway migration `V3__users_schema_and_default_roles.sql`.
2. Refactor `:data`: rename `StudioDAO` -> `StudioLookupDAO`, implement new `StudioDAO` and `DefaultStudioRoleDAO`, update `DaoModule`.
3. Add `/api/admin/v1/studio-roles` endpoints and update studio creation in `:api`.
4. Add `com.larpconnect.njall.api.studios` package with `StudiosRoute` and `StudioActor` in `:api`.
5. Update `openapi.yaml` with all new and modified paths and schemas.
6. Verify unit and integration test suites.

## Open Questions

<!-- None -->
