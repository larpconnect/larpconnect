## 1. Data Plane Non-Null Records & Sentinel Enum (`:data`)

- [x] 1.1 Add in-memory `UNKNOWN` sentinel constant to `AdminUserStatus` in `data/src/main/java/com/larpconnect/njall/data/domain/AdminUserStatus.java`.
- [x] 1.2 Update `StudioLookup` in `data/src/main/java/com/larpconnect/njall/data/domain/StudioLookup.java` to declare `Optional<Instant> deletedAt`, provide overloaded constructor accepting `@Nullable Instant deletedAt`, and update `isDeleted()`.
- [x] 1.3 Update `MigrationConfig` in `data/src/main/java/com/larpconnect/njall/data/config/MigrationConfig.java` to declare `Optional<String> password`, update compact constructor validation, and update `hasPassword()`.
- [x] 1.4 Update `SessionConfig` in `data/src/main/java/com/larpconnect/njall/data/config/SessionConfig.java` to declare `Optional<String> password`, update compact constructor validation, and update `hasPassword()`.
- [x] 1.5 Add validation guard in `DefaultAdminUserDAO` in `data/src/main/java/com/larpconnect/njall/data/dao/DefaultAdminUserDAO.java` to reject `AdminUserStatus.UNKNOWN` with `IllegalArgumentException`.
- [x] 1.6 Update `:data` unit tests (`AdminDomainTest`, `MigrationConfigTest`, `SessionConfigTest`, `DefaultStudioDAOTest`, `DefaultAdminUserDAOTest`) and verify with `./gradlew :data:check`.

## 2. API Plane Non-Null Commands & Requests (`:api`)

- [x] 2.1 Update `UserAdminCommand` in `api/src/main/java/com/larpconnect/njall/api/admin/UserAdminCommand.java`:
  - `CreateUser`: declare `AdminUserStatus status` and `ImmutableList<String> initialRoles`, with overloaded constructor defaulting nulls to `UNKNOWN` and empty list.
  - `AddRole` & `RemoveRole`: declare `Optional<UUID> roleId` and `Optional<String> roleName`, with overloaded constructors wrapping `@Nullable` parameters.
- [x] 2.2 Update `CreateUserRequest` in `api/src/main/java/com/larpconnect/njall/api/admin/CreateUserRequest.java`: declare `AdminUserStatus status` and `ImmutableList<String> roles`, with compact constructor normalizing nulls to `UNKNOWN` and `ImmutableList.of()`.
- [x] 2.3 Update `RoleAssignmentRequest` in `api/src/main/java/com/larpconnect/njall/api/admin/RoleAssignmentRequest.java`: declare `Optional<UUID> roleId` and `Optional<String> roleName`, with overloaded constructors wrapping `@Nullable` parameters.
- [x] 2.4 Update `UserAdminActor` in `api/src/main/java/com/larpconnect/njall/api/admin/UserAdminActor.java`: resolve `AdminUserStatus.UNKNOWN` to `ACTIVE` in `onCreateUser`, and update `resolveRole` and `resolveRoleIds` to process non-null `Optional` and `ImmutableList`.
- [x] 2.5 Update `:api` unit tests (`CreateUserRequestTest`, `UserAdminActorTest`, `UserAdminRouteTest`) and verify with `./gradlew :api:check`.

## 3. Server Module Callbacks & Compatibility (`:server`)

- [x] 3.1 Verify `:server` CLI configuration builder and runner compile and operate cleanly with non-null `MigrationConfig` and `SessionConfig`.
- [x] 3.2 Run and verify `:server` tests with `./gradlew :server:check`.

## 4. Integration Verification & ArchUnit Enforcement (`:integration`)

- [x] 4.1 Add ArchUnit invariant rule `records_must_not_have_nullable_components` to `ArchitectureTest` in `integration/src/test/java/com/larpconnect/njall/integration/arch/ArchitectureTest.java`.
- [x] 4.2 Verify all Cucumber steps and integration tests pass with `./gradlew :integration:check`.

## 5. Whole Project Verification & OpenSpec Validation

- [x] 5.1 Run full project verification suite with `./gradlew check build`.
- [x] 5.2 Validate OpenSpec change with `openspec validate eliminate-nullable-record-components --type change --strict`.
