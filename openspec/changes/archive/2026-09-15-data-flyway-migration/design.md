## Context

LarpConnect (Project Njall) is a high-performance, reactive server application built with Java 25 LTS, Apache Pekko Typed, and Google Guice. Current modules include `:parent`, `:bom`, `:common`, `:api`, `:server`, `:test`, and `:integration`. 

To support persistent domain models, we are introducing the `:data` module. The `:data` module will ultimately host both Flyway migrations and Hibernate-based CTI entity mappings. The immediate goal is establishing the Flyway-based migration infrastructure, executing a bootstrap script that initializes PostgreSQL schemas, role access controls, custom enum types, and core seed tables (`servers`, `server_contacts`).

Per project architectural constraints (AGENTS.md):
- The `:data` module must not depend on Apache Pekko or use actors.
- Exactly one top-level package per module (`com.larpconnect.njall.data`).
- Public Guice module hierarchy with strictly single-layer subpackage installation.
- Administrative credentials for `njall` must be transient, opened solely for migration and immediately released.

## Goals / Non-Goals

**Goals:**
- Create the `:data` Gradle library module registered in `settings.gradle.kts` and `bom/build.gradle.kts`.
- Implement `DatabaseMigrator` using Flyway 13+ targeting PostgreSQL 18+ and PostGIS.
- Manage bootstrap schema creation (`njall`, `njall_admin`, `njall_users`, `njall_system`), role search paths, and privilege grants.
- Create custom enums (`trole`, `tcontact`) and seed tables (`servers` with UUIDv4, `server_contacts` with UUIDv7).
- Substitute configuration placeholders (`${server_name}`, `${primary_domain}`, `${admin_contact}`) from `ServerConfig`.
- Implement transient `njall` connection lifecycle where connections and DataSource pools are cleanly closed upon completion.
- Support `--migrate` CLI flag in `ServerApp` that triggers migrations and exits immediately.
- Provide unit tests and a Testcontainers integration test harness using `postgis/postgis:18-3.6-alpine`.

**Non-Goals:**
- Implementing Hibernate ORM entities, DTOs, or DAOs (deferred to follow-on changes).
- Establishing long-lived connection pools for `njall_admin`, `njall_users`, or `njall_system`.
- Exposing database actors or Pekko streams in the `:data` layer.

## Architecture & C4 Diagrams

### C4 Level 2: Container Diagram

```
+─────────────────────────────────────────────────────────────────────────────+
|                                OPERATOR / CLI                               |
|                                                                             |
|   Runs: larpconnect [--migrate]                                             |
+──────────────────────────────────────┬──────────────────────────────────────+
                                       │
                                       ▼
+─────────────────────────────────────────────────────────────────────────────+
|                         LARPCONNECT SERVER PROCESS                          |
|                                                                             |
|  +───────────────────────────────────+   +───────────────────────────────+  |
|  | CLI Argument & Boot Coordinator   |   | Pekko HTTP Server Runtime     |  |
|  | (Detects --migrate -> triggers    |   | (Binds socket on 0.0.0.0:8080 |  |
|  |  migrator & exits)                |   |  when not migrating)          |  |
|  +─────────────────┬─────────────────+   +───────────────────────────────+  |
|                    │                                                        |
|                    ▼                                                        |
|  +───────────────────────────────────+                                      |
|  | Database Migration Component      |                                      |
|  | (:data / Flyway)                  |                                      |
|  +─────────────────┬─────────────────+                                      |
+────────────────────┼────────────────────────────────────────────────────────+
                     │
                     │ JDBC (Transient connection as user 'njall')
                     ▼
+─────────────────────────────────────────────────────────────────────────────+
|                         POSTGRESQL 18+ / POSTGIS                            |
|                                                                             |
|   Schemas: njall, njall_admin, njall_users, njall_system                    |
|   Tables:  servers, server_contacts, flyway_schema_history                  |
+─────────────────────────────────────────────────────────────────────────────+
```

### C4 Level 3: Component Diagram (:server & :data)

```
+─────────────────────────────────────────────────────────────────────────────+
|                                  :server                                    |
|                                                                             |
|   ServerApp (main entrypoint)                                               |
|       │                                                                     |
|       ├──> CliArgs (Pure argument parsing: detects --migrate)               |
|       └──> ServerModule (Guice)                                             |
|                 │                                                           |
|                 ├──> installs CommonModule                                  |
|                 ├──> installs ApiModule                                     |
|                 ├──> installs HttpServerModule                              |
|                 └──> installs DataModule ─────────────────────────────────┐ |
+───────────────────────────────────────────────────────────────────────────│─+
                                                                            │
+───────────────────────────────────────────────────────────────────────────│─+
|                                   :data                                   │ |
|                                                                           │ |
|   DataModule <────────────────────────────────────────────────────────────┘ |
|       ├──> installs DatabaseConfigModule                                    |
|       │         └──> provides DatabaseConfig & MigrationConfig              |
|       │                                                                     |
|       └──> installs MigrationModule                                         |
|                 └──> binds DatabaseMigrator -> FlywayDatabaseMigrator       |
|                                                                             |
|   FlywayDatabaseMigrator                                                    |
|       ├──> Reads MigrationConfig (JDBC URL, njall credentials, placeholders)|
|       ├──> Creates transient DataSource                                     |
|       ├──> Runs flyway.migrate()                                            |
|       └──> Closes DataSource in finally block                               |
+─────────────────────────────────────────────────────────────────────────────+
```

## Decisions

### Decision 1: Dedicated `:data` Module with Zero Pekko Reliance
- **Rationale**: Isolates relational database interaction, connection pooling, and migration dependencies from the actor runtime. Keeps persistence logic decoupled and testable without actor system overhead.
- **Alternatives Considered**: Housing migrations in `:server` or `:common`. Rejected to respect single responsibility and maintain clean DAG topology.

### Decision 2: Flyway for Database Migrations
- **Rationale**: Standard, deterministic SQL-based migration runner with native PostgreSQL and schema history management.
- **Alternatives Considered**: Liquibase. Rejected due to XML/YAML verbosity and team conventions favoring native SQL scripts.

### Decision 3: Transient Administrative Connection Lifecycle
- **Rationale**: The `njall` role holds elevated privileges (schema ownership, role configuration). To minimize attack surface, its credentials and connections must only exist during migration execution and must never be held open during HTTP request handling.
- **Alternatives Considered**: Reusing a shared connection pool for migrations and server queries. Rejected for security isolation.

### Decision 4: `--migrate` CLI Flag Semantics (Run and Exit)
- **Rationale**: Cleanly separates deployment/migration phases from server runtime phases in containerized and orchestrated environments (e.g. Kubernetes Job vs Pod).
- **Alternatives Considered**: Running migrations automatically on every server boot. Rejected to avoid race conditions across horizontally scaled nodes.

### Decision 5: UUIDv4 for `servers` vs UUIDv7 for `server_contacts`
- **Rationale**: `servers.id` is externally exposed and must not leak server creation timestamps. `server_contacts.id` is an internal entity that benefits from UUIDv7 temporal locality and index efficiency.
- **Alternatives Considered**: Using UUIDv7 everywhere. Rejected because public server IDs would reveal creation time.

### Decision 6: Testcontainers with `postgis/postgis:18-3.6-alpine`
- **Rationale**: Provides genuine PostgreSQL 18+ environment with native PostGIS 3.6 libraries and `uuidv7()` built-in support, matching production capabilities.
- **Alternatives Considered**: H2 or Mockito in-memory simulation. Rejected because PostgreSQL-specific extensions (`postgis`), enums, and role search paths cannot be faithfully verified without real Postgres.

## Risks / Trade-offs

- **[Risk] Docker permissions in WSL environment** -> *Mitigation*: Ensure user is added to `docker` group (`sudo usermod -aG docker $USER`) before executing containerized tests.
- **[Risk] Pre-existing role requirements in PostgreSQL** -> *Mitigation*: Test harness runs a superuser bootstrap script creating the 4 roles (`njall`, `njall_admin`, `njall_users`, `njall_system`) before invoking Flyway.
- **[Risk] Migration failure leaves database in indeterminate state** -> *Mitigation*: Flyway executes migrations in transactions where possible; failure triggers explicit error logging, clean connection release, and non-zero process termination.

## Migration Plan

1. Create `:data` module with Gradle build configuration and Guice bindings.
2. Place `V1__bootstrap_schemas_and_servers.sql` in `:data/src/main/resources/db/migration/`.
3. Add configuration defaults to `common/src/main/resources/reference.conf`.
4. Integrate `--migrate` flag into `ServerApp`.
5. Execute Testcontainers verification to validate schema, enum, table, and grant creation.

## Open Questions

None. All architectural questions resolved during exploration.
