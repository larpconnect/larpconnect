## MODIFIED Requirements

### Requirement: Admin DAOs with Mutation Operations
The system SHALL provide `AdminUserDAO`, `AdminRoleDAO`, and `StudioDAO` interfaces extending `DAO<T>`. In addition to standard `findById(UUID id)` and `list()`, `AdminUserDAO` SHALL provide `findByUsername(String username)`, `create(String username, AdminUserStatus status, List<UUID> roleIds)`, `addRole(UUID userId, UUID roleId)`, and `removeRole(UUID userId, UUID roleId)`. `AdminRoleDAO` SHALL provide `findByRoleName(String roleName)` and `create(String roleName)`. `StudioDAO` SHALL provide `findByAlias(String alias, DeletionFilter filter)`, `findById(UUID studioId, DeletionFilter filter)`, `list(DeletionFilter filter)`, `create(String alias)`, and `softDelete(UUID studioId)`. Parameterless overloads `findByAlias(String alias)`, `findById(UUID studioId)`, and `list()` SHALL default to `DeletionFilter.ACTIVE_ONLY`. All implementations SHALL inject `@NjallAdmin Provider<SessionFactory>`.

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
- **WHEN** `StudioDAO.list(DeletionFilter.ACTIVE_ONLY)` or `StudioDAO.list()` is invoked
- **THEN** the soft-deleted studio is excluded from the returned list
- **AND** `StudioDAO.list(DeletionFilter.INCLUDE_DELETED)` includes the soft-deleted studio
