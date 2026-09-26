# Implementation Tasks

## 1. OpenAPI and Behavioral Acceptance Test Contracts

- [x] 1.1 Update `api/src/main/resources/openapi.yaml` with `/api/studios/{studio-id}/v1/studio` and `/api/admin/v1/studio-roles[/{id}]` paths, schemas (`Studio`, `DefaultStudioRole`, `CreateStudioRoleRequest`, `UpdateStudioRoleRequest`), and response components.
- [x] 1.2 Add Cucumber feature file `integration/src/test/resources/features/user_studios_api.feature` covering user-space studio retrieval by UUID and alias, 404 for unmapped/soft-deleted, and tenant ID non-leakage.
- [x] 1.3 Add Cucumber scenarios in `integration/src/test/resources/features/admin_management_api.feature` covering `/api/admin/v1/studio-roles` GET, POST, and AIP-134 PATCH operations.

## 2. Database Migration

- [x] 2.1 Create Flyway migration `data/src/main/resources/db/migration/V3__users_schema_and_default_roles.sql` defining `default_studio_roles`, `studios`, reverse hostname functions, foreign key `fk_studios_studios_lookup`, and RLS policies for `njall_users` and `njall_admin`.
- [x] 2.2 Verify migration application via `:data:test` and test containers.

## 3. Data Layer Refactoring and Implementation (:data)

- [x] 3.1 Rename `StudioDAO` and `DefaultStudioDAO` in `:data` to `StudioLookupDAO` and `DefaultStudioLookupDAO` (`@NjallAdmin Provider<SessionFactory>`), updating queries and entity mappings.
- [x] 3.2 Implement domain record `Studio` and entity `StudioEntity` in `com.larpconnect.njall.data.domain` / `entity`.
- [x] 3.3 Implement tenanted `StudioDAO` and `DefaultStudioDAO` (`@NjallUsers Provider<SessionFactory>`) with transaction-scoped `set_config('app.tenant_id', ...)` and `DAO<Studio>` inheritance.
- [x] 3.4 Implement domain record `DefaultStudioRole` and entity `DefaultStudioRoleEntity` in `com.larpconnect.njall.data.domain` / `entity`.
- [x] 3.5 Implement `DefaultStudioRoleDAO` (`@NjallAdmin Provider<SessionFactory>`) with `DAO<DefaultStudioRole>` inheritance.
- [x] 3.6 Update `DAO<T>` sealed permits, `DaoModule` multibindings, and update `StudioLookupDAO.create` to insert into `njall_users.studios` before `njall_admin.studios_lookup`.
- [x] 3.7 Add unit tests for `DefaultStudioLookupDAO`, `DefaultStudioDAO`, and `DefaultStudioRoleDAO`, and verify `./gradlew :data:check` passes.

## 4. Admin Studio Roles API Implementation (:api)

- [x] 4.1 Implement `DefaultStudioRoleAdminActor`, `StudioRoleAdminCommand`, `StudioRoleAdminResponse`, and factory in `com.larpconnect.njall.api.admin`.
- [x] 4.2 Implement `StudioRoleAdminRoute` supporting GET, POST, and AIP-134 PATCH with `update_mask`, bind in `AdminModule`, and mount in `DefaultAdminRoute`.
- [x] 4.3 Update `CreateStudioRequest` and `StudioAdminActor` to accept optional `name` and forward to `StudioLookupDAO.create`.
- [x] 4.4 Add unit tests for `StudioRoleAdminRoute` and `StudioRoleAdminActor`, and verify `./gradlew :api:check` passes.

## 5. User Space Studios API Implementation (:api)

- [x] 5.1 Create package `com.larpconnect.njall.api.studios` with `StudiosModule`, `StudioActor`, `StudioCommand`, `StudioResponse`, and actor factory.
- [x] 5.2 Implement `StudiosRoute` handling `GET /api/studios/{studio-id}/v1/studio`, delegating to `StudioActor` to resolve `{studio-id}` via `StudioLookupDAO` and query `StudioDAO`.
- [x] 5.3 Bind `StudiosRoute` as a `RouteProvider` in `StudiosModule` and install in `ApiModule`.
- [x] 5.4 Add unit tests for `StudiosRoute` and `StudioActor`, and verify `./gradlew :api:check` passes.

## 6. Integration Testing and Verification

- [x] 6.1 Implement step definitions in `:integration` for the new Cucumber feature scenarios.
- [x] 6.2 Run `./gradlew check build` to ensure all quality gates, JaCoCo coverage (85% line, 90% branch), ArchUnit rules, Spotless, SpotBugs, and Checkstyle pass.
- [x] 6.3 Run `openspec validate add-users-studio-api --type change --strict` to verify OpenSpec planning validity.
