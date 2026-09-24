## 1. Remediate Constructor Visibility and RouteProvider in `:api`

- [x] 1.1 Move `api/src/main/java/com/larpconnect/njall/api/RouteProvider.java` to `api/src/main/java/com/larpconnect/njall/api/http/RouteProvider.java` and update package declaration.
- [x] 1.2 In `api/src/main/java/com/larpconnect/njall/api/http/DefaultRootRoute.java` and `api/src/main/java/com/larpconnect/njall/api/http/HttpModule.java`, update `RouteProvider` import to `com.larpconnect.njall.api.http.RouteProvider` (or remove redundant same-package import).
- [x] 1.3 In `api/src/main/java/com/larpconnect/njall/api/admin/AdminRoute.java` and `api/src/main/java/com/larpconnect/njall/api/admin/AdminModule.java`, update `RouteProvider` import to `com.larpconnect.njall.api.http.RouteProvider`.
- [x] 1.4 In `api/src/main/java/com/larpconnect/njall/api/admin/PekkoHealthCheck.java`, update constructor `public PekkoHealthCheck(ActorSystem<Void> system)` to package-private `PekkoHealthCheck(ActorSystem<Void> system)`.
- [x] 1.5 In `api/src/main/java/com/larpconnect/njall/api/admin/RoleAdminRoute.java`, `StudioAdminRoute.java`, and `UserAdminRoute.java`, update `@Inject` constructors from `public` to package-private.
- [x] 1.6 Run `./gradlew :api:test` to verify all tests in `:api` pass with the remediated constructors and relocated `RouteProvider`.

## 2. Implement ArchUnit Rules in `:integration`

- [x] 2.1 In `integration/src/test/java/com/larpconnect/njall/integration/arch/ArchitectureTest.java`, implement the ArchUnit rule `inject_constructors_must_not_be_public` asserting that constructors annotated with `@Inject` or `@AssistedInject` on non-test classes within `com.larpconnect.njall..` are not public.
- [x] 2.2 In `integration/src/test/java/com/larpconnect/njall/integration/arch/ArchitectureTest.java`, implement the ArchUnit rule `package_dependencies_must_not_go_up` asserting that no non-test class in `com.larpconnect.njall..` depends on an ancestor package within `com.larpconnect.njall`.
- [x] 2.3 Run `./gradlew :integration:test` to verify that `ArchitectureTest` passes cleanly.

## 3. Project-Wide Quality Verification and OpenSpec Validation

- [x] 3.1 Run `wsl openspec validate archunit-constructor-and-package-rules --type change --strict` to verify OpenSpec schema and artifact consistency.
- [x] 3.2 Run `wsl ./gradlew check build` to verify formatting (Spotless), static analysis (Checkstyle, SpotBugs, ErrorProne), test suites, and coverage gates across all modules.
