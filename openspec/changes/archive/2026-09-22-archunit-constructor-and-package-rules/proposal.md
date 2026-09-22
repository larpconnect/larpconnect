## Why

Enforcing architectural invariants through automated test gates prevents code erosion and preserves structural modularity as **Njall** scales. Specifically, allowing dependency injection constructors to be public leaks implementation details and permits bypassing Guice via direct `new` invocations. Furthermore, permitting child packages to depend upward on their parent packages creates cyclic architectural coupling and violates hierarchical encapsulation. Enforcing non-public `@Inject`/`@AssistedInject` constructors and down-or-out (never up) package dependencies via ArchUnit guarantees these invariants are mechanically verified on every build.

## What Changes

- **ArchUnit Constructor Visibility Rule**: Introduce an ArchUnit rule in `:integration` (`ArchitectureTest`) requiring that all constructors annotated with `@Inject` (`com.google.inject.Inject`, `jakarta.inject.Inject`, `javax.inject.Inject`) or `@AssistedInject` (`com.google.inject.assistedinject.AssistedInject`) across `com.larpconnect.njall..` must not be public.
- **Remediate Public Injected Constructors**: Update the 4 classes in the **API plane** (`PekkoHealthCheck`, `RoleAdminRoute`, `StudioAdminRoute`, `UserAdminRoute`) whose `@Inject` constructors are currently declared `public` to be package-private.
- **ArchUnit Package Dependency Direction Rule**: Introduce an ArchUnit rule in `:integration` (`ArchitectureTest`) requiring that within the `com.larpconnect.njall` namespace, package dependencies must go down (to subpackages) or out (to sibling/peer packages), never up (to enclosing ancestor packages).
- **Relocate `RouteProvider` (Option A)**: Move `RouteProvider` from the parent package `com.larpconnect.njall.api` to `com.larpconnect.njall.api.http`, eliminating upward package dependencies from `api.http` and `api.admin` to `api`. Update all import references across the **library module** `:api`.

## Capabilities

### New Capabilities
- `architectural-invariants`: Enforces structural codebase invariants via ArchUnit, specifically constructor visibility restrictions on injected components and downward/outward package dependency hierarchy.

### Modified Capabilities
<!-- Existing capabilities whose behaviour is changing (not just implementation). None are changing externally observable behavioral requirements. -->

## Impact

- **Affected Code**:
  - `integration/src/test/java/com/larpconnect/njall/integration/arch/ArchitectureTest.java`: Add ArchUnit rules for constructor visibility and upward package dependency prohibition.
  - `api/src/main/java/com/larpconnect/njall/api/RouteProvider.java` -> moved to `api/src/main/java/com/larpconnect/njall/api/http/RouteProvider.java`.
  - `api/src/main/java/com/larpconnect/njall/api/http/DefaultRootRoute.java`, `api/src/main/java/com/larpconnect/njall/api/http/HttpModule.java`, `api/src/main/java/com/larpconnect/njall/api/admin/AdminRoute.java`, `api/src/main/java/com/larpconnect/njall/api/admin/AdminModule.java`: Update `RouteProvider` imports.
  - `api/src/main/java/com/larpconnect/njall/api/admin/PekkoHealthCheck.java`, `RoleAdminRoute.java`, `StudioAdminRoute.java`, `UserAdminRoute.java`: Make `@Inject` constructors package-private.
- **Dependencies**: No new external dependencies; uses existing `archunit-junit5`.
- **Breaking Changes**: None. All modified constructors and relocated interfaces remain accessible to their required internal callers within `:api` and `:integration`.
