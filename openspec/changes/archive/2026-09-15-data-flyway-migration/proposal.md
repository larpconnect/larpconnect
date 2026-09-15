## Why

LarpConnect requires a persistent relational data layer to manage servers, contacts, multi-tenancy, and future domain models. Establishing a robust database migration infrastructure early using Flyway allows the project to manage PostgreSQL schema evolutions declaratively, enforce role-based security isolation from the initial bootstrap, and keep administrative database credentials completely segregated from regular application runtime traffic.

## What Changes

- Introduce a new `:data` Gradle library module adhering to project DAG and convention rules (with zero dependency on Apache Pekko or actors).
- Configure database migration via Flyway with support for PostgreSQL 18+ and PostGIS.
- Implement bootstrap migration script (`V1__bootstrap_schemas_and_servers.sql`) creating schemas (`njall`, `njall_admin`, `njall_users`, `njall_system`), custom enums (`trole`, `tcontact`), tables (`njall.servers`, `njall.server_contacts`), and role privilege grants.
- Support configuration-driven variable substitution for bootstrap seed data (`${server_name}`, `${primary_domain}`, `${admin_contact}`).
- Implement a transient administrative migrator lifecycle: the `njall` connection is opened exclusively to run migrations and is immediately closed and discarded.
- Add a `--migrate` command-line launch flag to `ServerApp` that executes migrations and terminates immediately.
- Rig integration testing using Testcontainers with `postgis/postgis:18-3.6-alpine` and pre-seeded test roles.

## Capabilities

### New Capabilities
- `database-migration`: Database migration execution via Flyway, managing PostgreSQL schemas, role-based security privileges, bootstrap seed tables, and transient administrative connection lifecycles triggered via the `--migrate` CLI flag.

### Modified Capabilities
<!-- None: No existing capability contracts are altered. -->

## Impact

- **New Module**: `:data` added to `settings.gradle.kts` and `bom/build.gradle.kts`.
- **Server Application**: `:server` depends on `:data`, installs `DataModule`, and `ServerApp` handles the `--migrate` CLI flag.
- **Dependencies**: Flyway PostgreSQL runtime and PostgreSQL JDBC driver added to `:data`; Testcontainers PostgreSQL added for testing.
- **Runtime Security**: The `njall` administrative role connection is strictly transient and never maintained by the server runtime.
