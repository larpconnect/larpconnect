## 1. Data Layer DeletionFilter and StudioDAO Refactoring

- [x] 1.1 Create `DeletionFilter` enum in `com.larpconnect.njall.data.domain` with `ACTIVE_ONLY` and `INCLUDE_DELETED` constants and `includesDeleted()` helper method.
- [x] 1.2 Refactor `StudioDAO` interface to replace `boolean includeDeleted` with `DeletionFilter filter`, preserving zero-argument default overloads forwarding to `DeletionFilter.ACTIVE_ONLY`.
- [x] 1.3 Refactor `DefaultStudioDAO` implementation to accept `DeletionFilter` and compose HQL queries based on `filter.includesDeleted()`.
- [x] 1.4 Update `DefaultStudioDAOTest` unit tests to supply `DeletionFilter` and test both `ACTIVE_ONLY` and `INCLUDE_DELETED` behavior.
- [x] 1.5 Run `./gradlew :data:test` and verify that all data module tests pass before proceeding to `:api`.

## 2. API Layer Actor Commands and Route Response Mapping

- [x] 2.1 Refactor `StudioAdminCommand` records (`ListStudios`, `GetStudioById`, `GetStudioByAlias`) to accept `DeletionFilter` instead of `boolean includeDeleted`.
- [x] 2.2 Refactor `StudioAdminActor` message handlers to pass `cmd.filter()` to `studioDao` and update `onCreateStudio` to query with `DeletionFilter.INCLUDE_DELETED`.
- [x] 2.3 Refactor `StudioAdminRoute` to parse `include_deleted` query parameter into `DeletionFilter`, pass to actor commands, and replace `boolean isCreateOperation` with dedicated `mapCreateResponse` and `mapOkResponse` methods.
- [x] 2.4 Refactor `RoleAdminRoute` and `UserAdminRoute` to replace `boolean isCreateOperation` with dedicated `mapCreateResponse` and `mapOkResponse` methods.
- [x] 2.5 Update `:api` unit tests (`StudioAdminActorTest`, `StudioAdminRouteTest`, `RoleAdminRouteTest`, `UserAdminRouteTest`) and execute `./gradlew :api:test`.

## 3. Integration Tests and Verification

- [x] 3.1 Update any integration test step definitions in `:integration` referencing `StudioDAO` or `StudioAdminCommand` to supply `DeletionFilter`.
- [x] 3.2 Execute `./gradlew :integration:test` and verify all acceptance scenarios pass.

## 4. Quality Gates and Specification Validation

- [x] 4.1 Run `openspec validate eliminate-boolean-parameters --type change --strict` to verify change schema compliance.
- [x] 4.2 Run `./gradlew spotlessApply` and `./gradlew spotlessCheck` to enforce formatting.
- [x] 4.3 Run `./gradlew check build` across all modules to satisfy Checkstyle, SpotBugs, ErrorProne, ArchUnit, and JaCoCo coverage gates.
