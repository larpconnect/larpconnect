## Why

The `com.larpconnect.njall.api.admin` package in the `:api` **library module** currently houses 38 classes and records encompassing multiple distinct administrative domains: health checks, server telemetry, studios, studio roles, users, roles, and shared validation. Co-locating all actors, commands, responses, route handlers, and factory bindings in a single flat package increases cognitive load, complicates dependency isolation, and runs contrary to **Njall** architectural principles where functional domains and endpoint hierarchies are partitioned into clean, self-contained packages. Decomposing `com.larpconnect.njall.api.admin` into domain-specific subpackages with dedicated Guice modules establishes clear architectural boundaries and enforces the Directed Acyclic Graph (DAG) package topology mandated by `AGENTS.md`.

## What Changes

- Decompose `com.larpconnect.njall.api.admin` into endpoint-aligned subpackages:
  - `com.larpconnect.njall.api.admin.health`: Pekko health check actor, command, response protocol, actor factory, `PekkoHealthCheck` probe, `HealthAdminRoute`, and `HealthAdminModule`.
  - `com.larpconnect.njall.api.admin.servers`: Server admin **actor**, command, response protocol, actor factory, `ServersAdminRoute`, and `ServersAdminModule`.
  - `com.larpconnect.njall.api.admin.studios`: Studio admin **actor**, commands, responses, factory, `StudioAdminRoute`, request DTOs (`CreateStudioRequest`), and `StudiosAdminModule`.
  - `com.larpconnect.njall.api.admin.studioroles`: Studio role admin **actor**, commands, responses, factory, `StudioRoleAdminRoute`, request DTOs (`CreateStudioRoleRequest`, `UpdateStudioRoleRequest`), and `StudioRolesAdminModule`.
  - `com.larpconnect.njall.api.admin.users`: User admin **actor**, commands, responses, factory, `UserAdminRoute`, request DTOs (`CreateUserRequest`), and `UsersAdminModule`.
  - `com.larpconnect.njall.api.admin.roles`: Role admin **actor**, commands, responses, factory, `RoleAdminRoute`, request DTOs (`CreateRoleRequest`, `RoleAssignmentRequest`), and `RolesAdminModule`.
  - `com.larpconnect.njall.api.admin.common`: Shared administrative utilities and records (`AdminValidation`, `AdminErrorResponse`) and `AdminCommonModule`, preventing downward subpackages from depending upward on the parent `admin` package.
- Extract dedicated `HealthAdminRoute` and `ServersAdminRoute` implementations, transforming `DefaultAdminRoute` in `com.larpconnect.njall.api.admin` into a pure composite aggregator conforming to IOSP-Lite.
- Equip each subpackage with its own `package-info.java` annotated with `@NullMarked`.
- Provide a dedicated Guice **module** in each subpackage, and configure `AdminModule` in `com.larpconnect.njall.api.admin` to install each subpackage **module** exactly one layer down.
- Maintain public contracts and HTTP routing unchanged: `AdminRoute` interface and `DefaultAdminRoute` implementation remain at `com.larpconnect.njall.api.admin`.

## Capabilities

### New Capabilities

None.

### Modified Capabilities

- `architectural-invariants`: Codify and verify package dependency and Guice **module** structure across `com.larpconnect.njall.api.admin` subpackages, ensuring zero upward dependencies and complete `@NullMarked` coverage.

## Impact

- **Affected Code**: `api/src/main/java/com/larpconnect/njall/api/admin/` classes relocated into subpackages `health`, `servers`, `studios`, `studioroles`, `users`, `roles`, and `common`.
- **Guice Configuration**: `AdminModule` updated to install subpackage modules (`HealthAdminModule`, `ServersAdminModule`, `StudiosAdminModule`, `StudioRolesAdminModule`, `UsersAdminModule`, `RolesAdminModule`, `AdminCommonModule`).
- **Tests**: Colocated unit tests in `api/src/test/java/com/larpconnect/njall/api/admin/` moved to matching test subpackages; integration imports in `server/src/test/java/com/larpconnect/njall/server/ServerModuleTest.java` updated.
- **External Contracts**: Zero breaking changes to HTTP endpoints, OpenAPI 3.1 specifications, wire formats, or error payloads.
