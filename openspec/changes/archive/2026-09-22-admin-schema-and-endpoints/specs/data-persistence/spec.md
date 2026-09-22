## ADDED Requirements

### Requirement: Admin Domain Records and Sealed DatabaseObject Hierarchy
The system SHALL expand the sealed `DatabaseObject` hierarchy to permit `Server`, `AdminUser`, `AdminRole`, and `StudioLookup`. All domain records SHALL be immutable and provide UUID primary identifiers via `id()`. `StudioLookup` SHALL expose both `tenantId()` and `studioId()`, with `id()` mapping to `studioId()`.

#### Scenario: AdminUser implements DatabaseObject
- **GIVEN** an `AdminUser` record instance
- **WHEN** checked against `DatabaseObject`
- **THEN** it is an instance of `DatabaseObject` and returns a non-null UUID identifier from `id()`

#### Scenario: StudioLookup exposes tenantId and studioId
- **GIVEN** a `StudioLookup` record created from persistent data
- **WHEN** methods `tenantId()` and `studioId()` are invoked
- **THEN** both return valid non-null UUIDs, and `id()` equals `studioId()`

### Requirement: Admin DAOs with Mutation Operations
The system SHALL provide `AdminUserDAO`, `AdminRoleDAO`, and `StudioDAO` interfaces extending `DAO<T>`. In addition to standard `findById(UUID id)` and `list()`, `AdminUserDAO` SHALL provide `findByUsername(String username)`, `create(String username, AdminUserStatus status, List<UUID> roleIds)`, `addRole(UUID userId, UUID roleId)`, and `removeRole(UUID userId, UUID roleId)`. `AdminRoleDAO` SHALL provide `findByRoleName(String roleName)` and `create(String roleName)`. `StudioDAO` SHALL provide `findByAlias(String alias)`, `create(String alias)`, `list(boolean includeDeleted)`, and `softDelete(UUID studioId)`. All implementations SHALL inject `@NjallAdmin Provider<SessionFactory>`.

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
- **WHEN** `StudioDAO.list(false)` is invoked
- **THEN** the soft-deleted studio is excluded from the returned list
- **AND** `StudioDAO.list(true)` includes the soft-deleted studio

### Requirement: Admin JPA Entities Registration
The system SHALL register `AdminUserEntity`, `AdminRoleEntity`, and `StudioLookupEntity` in the `@NjallAdmin` entity multibinder within `DaoModule`, making them accessible to the Hibernate `@NjallAdmin SessionFactory`.

#### Scenario: Admin JPA entities are registered in DaoModule
- **GIVEN** the Guice injector with `DataModule` installed
- **WHEN** the `@NjallAdmin Set<Class<?>>` entity multibinding is resolved
- **THEN** the set contains `ServerEntity.class`, `ServerContactEntity.class`, `AdminUserEntity.class`, `AdminRoleEntity.class`, and `StudioLookupEntity.class`
