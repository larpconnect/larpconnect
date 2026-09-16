## Why

Now that Flyway database migrations are established, the application requires persistent domain representations and administrative query APIs. Exposing server metadata and associated contacts via a dedicated administrative endpoint (`GET /api/admin/v1/servers`) enables system operators and orchestration tools to inspect runtime node topologies, domains, and contact configurations safely without direct database access.

## What Changes

- **Sealed Persistence Model**: Introduce `DatabaseObject` and `DAO<T extends DatabaseObject>` sealed interfaces in `:data` providing uniform `findById(UUID id)` and `list()` query contracts.
- **Dual Session Factories**: Configure two isolated Hibernate `SessionFactory` instances in `:data` bound with `@NjallAdmin` (connecting via role `njall_admin`) and `@NjallUsers` (connecting via role `njall_users`) with structured connection pool configurations.
- **Read-Only Server Model**: Implement immutable domain representations `Server` and `ServerContact` backed by a package-private Hibernate entity and `ServerDAO` (using `@NjallAdmin SessionFactory`), enforcing strict read-only semantics against `njall.servers` and `njall.server_contacts`.
- **Module Dependency**: Update `:api` to depend on `:data`, establishing the clean DAG `:common` -> `:data` -> `:api` -> `:server`.
- **Administrative Server Endpoint**: Expose `GET /api/admin/v1/servers` in `:api` via `AdminRoute`, delegating blocking DAO queries to a typed `ServerAdminActor` running on a dedicated Pekko blocking dispatcher (`larpconnect.blocking-dispatcher`), returning camelCase JSON.
- **OpenAPI Specification**: Document `GET /api/admin/v1/servers` and corresponding `Server` / `ServerContact` schemas in `openapi.yaml`.
- **Dual-Layer Testing**: Implement unit tests across `:data` and `:api`, and add Cucumber integration scenarios in `:integration` verifying end-to-end endpoint execution against a live PostgreSQL Testcontainer.

## Capabilities

### New Capabilities
- `data-persistence`: Sealed base DAO and DatabaseObject abstractions, dual Hibernate session factories for admin and user roles, and read-only domain queries for servers and contacts.
- `admin-server-api`: Administrative HTTP endpoint at `/api/admin/v1/servers` returning registered server topologies and contact points in camelCase JSON.

### Modified Capabilities
<!-- None -->

## Impact

- **Persistence Layer (`:data`)**: Adds Hibernate ORM 7, PostgreSQL dialect configuration, dual session factory providers, `DatabaseConfig` extensions, and `ServerDAO`.
- **API Layer (`:api`)**: Adds dependency on `:data`, adds Jackson serialization, updates `openapi.yaml`, expands `DefaultAdminRoute`, and adds `ServerAdminActor`.
- **Runtime Application (`:server`)**: Integrates new data bindings and coordinates session factory shutdown.
- **Integration Test Suite (`:integration`)**: Adds Cucumber feature tests and step definitions for `GET /api/admin/v1/servers`.
