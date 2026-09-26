# data-persistence Specification Delta

## ADDED Requirements

### Requirement: Tenanted User Space StudioDAO
The system SHALL provide a tenanted `StudioDAO` interface extending `DAO<Studio>` in the **Data plane** operating on the user schema. The `StudioDAO` SHALL provide `findById(UUID tenantId)` and `getStudio(UUID tenantId)`. All implementations SHALL inject `@NjallUsers Provider<SessionFactory>`. Before executing queries against `njall_users.studios`, the implementation SHALL set the PostgreSQL local configuration `app.tenant_id` to the provided tenant UUID within the transaction context using `set_config('app.tenant_id', :tenantId, true)` to enforce Row-Level Security isolation.

#### Scenario: Retrieve studio via tenanted StudioDAO with RLS
- **GIVEN** a persisted studio in `njall_users.studios` with tenant UUID and name "Valiant Games"
- **WHEN** `StudioDAO.findById(tenantId)` is invoked with that tenant UUID
- **THEN** the DAO sets `app.tenant_id` to `tenantId` in the session
- **AND** the DAO returns an Optional containing the `Studio` record with matching name
- **AND** querying with a different tenant UUID returns empty due to Row-Level Security filtering

### Requirement: Default Studio Role DAO
The system SHALL provide a `DefaultStudioRoleDAO` interface extending `DAO<DefaultStudioRole>` in the **Data plane**. In addition to standard `findById(UUID id)` and `list()`, `DefaultStudioRoleDAO` SHALL provide `findByName(String name)`, `create(String name)`, and `update(UUID id, String name)`. All implementations SHALL inject `@NjallAdmin Provider<SessionFactory>` and execute queries against `njall_users.default_studio_roles`.

#### Scenario: Create and list default studio roles
- **GIVEN** an active Hibernate session via `@NjallAdmin Provider<SessionFactory>`
- **WHEN** `DefaultStudioRoleDAO.create("ADMIN")` is executed
- **THEN** a new default studio role is persisted with a generated UUIDv7
- **AND** `DefaultStudioRoleDAO.list()` returns the role in the list

#### Scenario: Update default studio role name
- **GIVEN** an existing default studio role with name "MEMBER"
- **WHEN** `DefaultStudioRoleDAO.update(roleId, "PLAYER")` is executed
- **THEN** the role record is updated with name "PLAYER"
- **AND** `DefaultStudioRoleDAO.findById(roleId)` reflects the updated name

## MODIFIED Requirements

### Requirement: Admin DAOs with Mutation Operations
The system SHALL provide `AdminUserDAO`, `AdminRoleDAO`, and `StudioLookupDAO` interfaces extending `DAO<T>`. In addition to standard `findById(UUID id)` and `list()`, `AdminUserDAO` SHALL provide `findByUsername(String username)`, `create(String username, AdminUserStatus status, List<UUID> roleIds)`, `addRole(UUID userId, UUID roleId)`, and `removeRole(UUID userId, UUID roleId)`. `AdminRoleDAO` SHALL provide `findByRoleName(String roleName)` and `create(String roleName)`. `StudioLookupDAO` SHALL provide `findByAlias(String alias, DeletionFilter filter)`, `findById(UUID studioId, DeletionFilter filter)`, `list(DeletionFilter filter)`, `create(String alias, String name)`, `create(String alias)`, and `softDelete(UUID studioId)`. Parameterless overloads `findByAlias(String alias)`, `findById(UUID studioId)`, and `list()` SHALL default to `DeletionFilter.ACTIVE_ONLY`. When creating a new studio lookup, `StudioLookupDAO` SHALL insert the corresponding **Tenant** record into `njall_users.studios` before inserting the routing entry into `njall_admin.studios_lookup`. All implementations SHALL inject `@NjallAdmin Provider<SessionFactory>`.

#### Scenario: Create and retrieve admin user via AdminUserDAO
- **GIVEN** an active Hibernate session via `@NjallAdmin Provider<SessionFactory>`
- **WHEN** `AdminUserDAO.create("admin_tyr", AdminUserStatus.ACTIVE, List.of())` is invoked
- **THEN** a new `admin_users` record is persisted with a generated UUIDv7
- **AND** `AdminUserDAO.findByUsername("admin_tyr")` returns the persisted user

#### Scenario: Assign and unassign role via AdminUserDAO
- **GIVEN** a persisted admin user and persisted role
- **WHEN** `AdminUserDAO.addRole(userId, roleId)` is executed
- **THEN** an entry is inserted into `admin_role_assignments`
- **AND** subsequent `removeRole(userId, roleId)` deletes the junction entry

#### Scenario: StudioDAO filters soft-deleted records by default
- **GIVEN** a studio with `deleted_at` timestamp set
- **WHEN** `StudioLookupDAO.list(DeletionFilter.ACTIVE_ONLY)` or `StudioLookupDAO.list()` is invoked
- **THEN** the soft-deleted studio is excluded from the returned list
- **AND** `StudioLookupDAO.list(DeletionFilter.INCLUDE_DELETED)` includes the soft-deleted studio
