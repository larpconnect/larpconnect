## Why

Default Guice injector configurations allow fallback mechanisms such as runtime circular proxy synthesis, unannotated zero-argument constructor instantiation, and implicit just-in-time (JIT) binding resolution. Hardening the root injector in the **Server** **application module** eliminates these fallbacks, strictly enforcing compile-time and bootstrap-time verification of dependency inversion invariants across **Njall**.

## What Changes

- **Root Injector Hardening Modules**: In `ServerModule.configure()`, install four Guice hardening modules from `com.google.inject.util.Modules`:
  - `Modules.disableCircularProxiesModule()`: Prohibits dynamic generation of circular proxy wrappers and enforces strict directed acyclic graph (DAG) topology.
  - `Modules.requireAtInjectOnConstructorsModule()`: Disables fallback to unannotated zero-argument constructors, requiring explicit `@Inject` annotations on constructors.
  - `Modules.requireExplicitBindingsModule()`: Prohibits JIT binding generation, requiring all injected types to be explicitly declared in their respective modules.
  - `Modules.requireExactBindingAnnotationsModule()`: Enforces exact attribute matching on binding annotations.
- **Explicit Route Bindings in API Plane**: In `AdminModule.configure()`, explicitly declare bindings for `StudioAdminRoute`, `UserAdminRoute`, and `RoleAdminRoute` to satisfy explicit binding requirements.
- **Constructor Injection Annotations in Data Plane**: In `ActiveSessionFactories`, `DefaultDataSourceFactory`, and `DefaultFlywayFactory`, declare package-private `@Inject` constructors adhering to ArchUnit visibility rules and constructor injection requirements.
- **Verification Testing**: Update `ServerModuleTest` to assert that the hardened injector rejects circular dependencies, unannotated constructors, and missing explicit bindings.

## Capabilities

### New Capabilities
- `guice-injector-hardening`: Hardens the root Guice injector against circular proxies, unannotated constructor fallback, JIT bindings, and non-exact binding annotations.

### Modified Capabilities

None.

## Impact

- **Affected Modules**:
  - `:server`: `ServerModule` (hardening module installation) and `ServerModuleTest`.
  - `:api`: `AdminModule` (explicit route class bindings).
  - `:data`: `ActiveSessionFactories`, `DefaultDataSourceFactory`, `DefaultFlywayFactory` (package-private `@Inject` constructors).
- **Public API Contract**: Zero externally observable REST API changes; all endpoints behave identically.
- **Internal API**: Architectural dependency injection enforcement strictly at injector initialization time.
