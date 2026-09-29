## 1. Subpackage Foundation and Shared Utilities

- [x] 1.1 Create subpackage directories `health`, `servers`, `studios`, `studioroles`, `users`, `roles`, and `common` under `api/src/main/java/com/larpconnect/njall/api/admin/` and add `@NullMarked` `package-info.java` to each.
- [x] 1.2 Move `AdminValidation.java` and `AdminErrorResponse.java` to `com.larpconnect.njall.api.admin.common`, making methods and records accessible to sibling subpackages.
- [x] 1.3 Create `AdminCommonModule.java` in `com.larpconnect.njall.api.admin.common` providing administrative `ObjectMapper`.
- [x] 1.4 Move `AdminValidationTest.java` to `api/src/test/java/com/larpconnect/njall/api/admin/common/` and verify unit tests pass.

## 2. Health Domain Migration

- [x] 2.1 Move `HealthCheckActor.java`, `HealthCheckActorFactory.java`, `DefaultHealthCheckActorFactory.java`, `HealthCheckCommand.java`, `HealthCheckResponse.java`, and `PekkoHealthCheck.java` to `com.larpconnect.njall.api.admin.health`.
- [x] 2.2 Extract `HealthAdminRoute.java` from `DefaultAdminRoute` into `com.larpconnect.njall.api.admin.health`.
- [x] 2.3 Create `HealthAdminModule.java` in `com.larpconnect.njall.api.admin.health` binding `HealthAdminRoute`, `HealthCheckActorFactory`, `ActorRef<ApiCall<HealthCheckCommand>>`, and multibinding `PekkoHealthCheck` into `HealthCheck`.
- [x] 2.4 Move `HealthCheckActorTest.java`, `HealthCheckActorFactoryTest.java`, and `PekkoHealthCheckTest.java` to `api/src/test/java/com/larpconnect/njall/api/admin/health/` and verify unit tests pass.

## 3. Server Telemetry Domain Migration

- [x] 3.1 Move `ServerAdminActor.java`, `ServerAdminActorFactory.java`, `DefaultServerAdminActorFactory.java`, `ServerAdminCommand.java`, and `ServerAdminResponse.java` to `com.larpconnect.njall.api.admin.servers`.
- [x] 3.2 Extract `ServersAdminRoute.java` from `DefaultAdminRoute` into `com.larpconnect.njall.api.admin.servers`.
- [x] 3.3 Create `ServersAdminModule.java` in `com.larpconnect.njall.api.admin.servers` binding `ServersAdminRoute`, `ServerAdminActorFactory`, and `ActorRef<ServerAdminCommand>`.
- [x] 3.4 Move `ServerAdminActorTest.java` and `ServerAdminActorFactoryTest.java` to `api/src/test/java/com/larpconnect/njall/api/admin/servers/` and verify unit tests pass.

## 4. Studio and Studio Role Domain Migration

- [x] 4.1 Move `StudioAdminActor.java`, `StudioAdminActorFactory.java`, `DefaultStudioAdminActorFactory.java`, `StudioAdminCommand.java`, `StudioAdminResponse.java`, `StudioAdminRoute.java`, and `CreateStudioRequest.java` to `com.larpconnect.njall.api.admin.studios`.
- [x] 4.2 Create `StudiosAdminModule.java` in `com.larpconnect.njall.api.admin.studios` binding `StudioAdminRoute`, `StudioAdminActorFactory`, and `ActorRef<StudioAdminCommand>`.
- [x] 4.3 Move `StudioRoleAdminActor.java`, `StudioRoleAdminActorFactory.java`, `DefaultStudioRoleAdminActorFactory.java`, `StudioRoleAdminCommand.java`, `StudioRoleAdminResponse.java`, `StudioRoleAdminRoute.java`, `CreateStudioRoleRequest.java`, and `UpdateStudioRoleRequest.java` to `com.larpconnect.njall.api.admin.studioroles`.
- [x] 4.4 Create `StudioRolesAdminModule.java` in `com.larpconnect.njall.api.admin.studioroles` binding `StudioRoleAdminRoute`, `StudioRoleAdminActorFactory`, and `ActorRef<StudioRoleAdminCommand>`.
- [x] 4.5 Move `StudioAdminActorTest.java`, `StudioAdminRouteTest.java`, `StudioRoleAdminActorTest.java`, and `StudioRoleAdminRouteTest.java` to matching test subpackages and verify unit tests pass.

## 5. User and Role Domain Migration

- [x] 5.1 Move `UserAdminActor.java`, `UserAdminActorFactory.java`, `DefaultUserAdminActorFactory.java`, `UserAdminCommand.java`, `UserAdminResponse.java`, `UserAdminRoute.java`, and `CreateUserRequest.java` to `com.larpconnect.njall.api.admin.users`.
- [x] 5.2 Create `UsersAdminModule.java` in `com.larpconnect.njall.api.admin.users` binding `UserAdminRoute`, `UserAdminActorFactory`, and `ActorRef<UserAdminCommand>`.
- [x] 5.3 Move `RoleAdminActor.java`, `RoleAdminActorFactory.java`, `DefaultRoleAdminActorFactory.java`, `RoleAdminCommand.java`, `RoleAdminResponse.java`, `RoleAdminRoute.java`, `CreateRoleRequest.java`, and `RoleAssignmentRequest.java` to `com.larpconnect.njall.api.admin.roles`.
- [x] 5.4 Create `RolesAdminModule.java` in `com.larpconnect.njall.api.admin.roles` binding `RoleAdminRoute`, `RoleAdminActorFactory`, and `ActorRef<RoleAdminCommand>`.
- [x] 5.5 Move `UserAdminActorTest.java`, `UserAdminRouteTest.java`, `RoleAdminActorTest.java`, `RoleAdminRouteTest.java`, `CreateUserRequestTest.java`, and `RoleAssignmentRequestTest.java` to matching test subpackages and verify unit tests pass.

## 6. Admin Module and Root Route Assembly

- [x] 6.1 Refactor `DefaultAdminRoute.java` in `com.larpconnect.njall.api.admin` to aggregate `HealthAdminRoute`, `ServersAdminRoute`, `StudioAdminRoute`, `StudioRoleAdminRoute`, `UserAdminRoute`, and `RoleAdminRoute` via pure composition.
- [x] 6.2 Refactor `AdminModule.java` in `com.larpconnect.njall.api.admin` to install subpackage modules (`HealthAdminModule`, `ServersAdminModule`, `StudiosAdminModule`, `StudioRolesAdminModule`, `UsersAdminModule`, `RolesAdminModule`, `AdminCommonModule`), bind `AdminRoute` to `DefaultAdminRoute`, and multibind `RouteProvider`.
- [x] 6.3 Update `AdminModuleTest.java` and `AdminRouteTest.java` in `api/src/test/java/com/larpconnect/njall/api/admin/` and update `ServerModuleTest.java` in `:server`.

## 7. Architectural Verification and Quality Gate

- [x] 7.1 Execute `./gradlew :api:test` to verify all unit tests in the `:api` **module** pass.
- [x] 7.2 Execute `./gradlew :integration:test` to verify ArchUnit rules (zero upward dependencies, non-public `@Inject` constructors, and immutable records).
- [x] 7.3 Execute `./gradlew check build` to verify Spotless, Checkstyle, SpotBugs, JaCoCo coverage, and full build integrity.
- [x] 7.4 Execute `openspec validate decompose-admin-subpackages --type change --strict` to verify OpenSpec artifact coherence.
