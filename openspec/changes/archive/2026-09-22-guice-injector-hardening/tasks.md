## 1. Data Layer Constructor Injection Annotations

- [x] 1.1 Add package-private `@Inject` constructor to `ActiveSessionFactories` in `com.larpconnect.njall.data.session`.
- [x] 1.2 Add package-private `@Inject` constructor to `DefaultDataSourceFactory` in `com.larpconnect.njall.data.migration`.
- [x] 1.3 Add package-private `@Inject` constructor to `DefaultFlywayFactory` in `com.larpconnect.njall.data.migration`.
- [x] 1.4 Execute `./gradlew :data:test` and verify all `:data` unit tests pass before making changes to dependent modules.

## 2. API Layer Explicit Route Bindings

- [x] 2.1 Explicitly declare `bind(StudioAdminRoute.class)`, `bind(UserAdminRoute.class)`, and `bind(RoleAdminRoute.class)` in `AdminModule.configure()`.
- [x] 2.2 Execute `./gradlew :api:test` and verify all `:api` unit tests pass before making changes to dependent modules.

## 3. Server Module Injector Hardening

- [x] 3.1 In `ServerModule.configure()`, install `Modules.disableCircularProxiesModule()`, `Modules.requireAtInjectOnConstructorsModule()`, `Modules.requireExplicitBindingsModule()`, and `Modules.requireExactBindingAnnotationsModule()`.
- [x] 3.2 Update `ServerModuleTest` with tests verifying that the root injector rejects circular dependency proxies, requires constructor `@Inject`, and forbids unannotated JIT bindings.
- [x] 3.3 Execute `./gradlew :server:test` and verify all `:server` unit tests pass.

## 4. Integration Acceptance Verification

- [x] 4.1 Execute `./gradlew :integration:test` and verify that all Cucumber acceptance scenarios and ArchUnit invariants pass against the hardened injector.

## 5. Quality Gates and Specification Validation

- [x] 5.1 Run `openspec validate guice-injector-hardening --type change --strict` to verify change schema compliance.
- [x] 5.2 Run `./gradlew spotlessApply` and `./gradlew spotlessCheck` to enforce formatting rules.
- [x] 5.3 Run `./gradlew check build` across all modules to satisfy Checkstyle, SpotBugs, ErrorProne, ArchUnit, and JaCoCo coverage gates.
